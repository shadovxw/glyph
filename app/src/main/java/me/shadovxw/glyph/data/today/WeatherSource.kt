package me.shadovxw.glyph.data.today

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** The raw responses are kept so the cards can show them instantly on the next start. */
class WeatherBundle(val weather: WeatherData, val aqi: AirQualityData?, val forecastJson: String, val airJson: String?)

/** Open-Meteo: forecast, sunrise/sunset and (US) air quality. No API key. */
class WeatherSource {
    suspend fun fetch(coordinates: Coordinates): WeatherBundle = coroutineScope {
        val lat = coordinates.latitude
        val lon = coordinates.longitude
        val forecast = async {
            get(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&hourly=temperature_2m" +
                    "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=2",
            )
        }
        val air = async {
            runCatching {
                get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi&timezone=auto")
            }.getOrNull()
        }
        val forecastJson = forecast.await()
        val airJson = air.await()
        WeatherBundle(parseWeather(forecastJson), airJson?.let(::parseAirQuality), forecastJson, airJson)
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun parseWeather(json: String, now: LocalDateTime = LocalDateTime.now()): WeatherData {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            val hourly = root.getJSONObject("hourly")

            val times = hourly.getJSONArray("time")
            val temps = hourly.getJSONArray("temperature_2m")
            val start = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            var first = (0 until times.length()).firstOrNull { LocalDateTime.parse(times.getString(it)) >= start } ?: times.length()
            // Four columns, three hours apart, starting at the next full hour.
            val columns = (0 until 4).map { first + it * 3 }.filter { it < times.length() && !temps.isNull(it) }.map {
                HourlyTemp(hour = LocalDateTime.parse(times.getString(it)).hour, temp = temps.getDouble(it).roundToInt())
            }

            return WeatherData(
                tempNow = current.getDouble("temperature_2m").roundToInt(),
                condition = conditionFor(current.getInt("weather_code")),
                high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                hourly = columns,
                sunrise = LocalTime.from(LocalDateTime.parse(daily.getJSONArray("sunrise").getString(0))),
                sunset = LocalTime.from(LocalDateTime.parse(daily.getJSONArray("sunset").getString(0))),
            )
        }

        fun parseAirQuality(json: String): AirQualityData? {
            val current = JSONObject(json).optJSONObject("current") ?: return null
            if (current.isNull("us_aqi")) return null
            val aqi = current.getDouble("us_aqi").roundToInt()
            return AirQualityData(aqi, categoryFor(aqi))
        }

        /** WMO weather codes, as used by Open-Meteo. */
        fun conditionFor(code: Int): String = when (code) {
            0 -> "Clear"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm, hail"
            else -> "—"
        }

        /** US AQI bands. */
        fun categoryFor(aqi: Int): String = when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Sensitive groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very unhealthy"
            else -> "Hazardous"
        }
    }
}
