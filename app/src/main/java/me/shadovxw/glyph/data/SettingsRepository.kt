package me.shadovxw.glyph.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.shadovxw.glyph.data.today.Coordinates

private val Context.glyphDataStore by preferencesDataStore(name = "glyph_settings")

/** How Glyph picks light or dark: follow the phone's setting (the default), or force one. */
enum class ThemeMode {
    System, Light, Dark;

    /** The actual choice, given whether the phone is currently in dark mode. */
    fun resolve(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }
}

/** Shared settings. The lock-screen wallpaper reads the same theme setting (DESIGN.md 5B). */
class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.glyphDataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.System
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.glyphDataStore.edit { it[themeKey] = mode.name }
    }

    private val motionKey = booleanPreferencesKey("motion")

    /** Whether the live animation runs: the Orbit background on home and the rain and dust on the wallpaper. */
    val motion: Flow<Boolean> = context.glyphDataStore.data.map { it[motionKey] ?: true }

    suspend fun setMotion(on: Boolean) {
        context.glyphDataStore.edit { it[motionKey] = on }
    }

    /** Launches per package from the drawer, used for suggested apps. Stored as "pkg=count|pkg=count". */
    val launchCounts: Flow<Map<String, Int>> = context.glyphDataStore.data.map { parseCounts(it[countsKey]) }

    suspend fun recordLaunch(packageName: String) {
        context.glyphDataStore.edit { prefs ->
            val counts = parseCounts(prefs[countsKey]).toMutableMap()
            counts[packageName] = (counts[packageName] ?: 0) + 1
            prefs[countsKey] = counts.entries.joinToString("|") { "${it.key}=${it.value}" }
        }
    }

    private val countsKey = stringPreferencesKey("launch_counts")

    private fun parseCounts(raw: String?): Map<String, Int> =
        raw.orEmpty().split('|').mapNotNull { entry ->
            val (pkg, count) = entry.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            count.toIntOrNull()?.let { pkg to it }
        }.toMap()

    // ---- Apps page: the user's groups, as JSON (see data/groups/AppGroup.kt). Null until the first run has made the defaults. ----

    private val groupsKey = stringPreferencesKey("app_groups")

    val appGroups: Flow<String?> = context.glyphDataStore.data.map { it[groupsKey] }

    suspend fun setAppGroups(json: String) {
        context.glyphDataStore.edit { it[groupsKey] = json }
    }

    // ---- Today data (last location and the last weather responses, so the cards fill in instantly) ----

    private val latKey = doublePreferencesKey("last_lat")
    private val lonKey = doublePreferencesKey("last_lon")
    private val forecastKey = stringPreferencesKey("weather_forecast_json")
    private val airKey = stringPreferencesKey("weather_air_json")
    private val weatherAtKey = longPreferencesKey("weather_saved_at")

    suspend fun saveCoordinates(latitude: Double, longitude: Double) {
        context.glyphDataStore.edit { it[latKey] = latitude; it[lonKey] = longitude }
    }

    suspend fun savedCoordinates(): Coordinates? {
        val prefs = context.glyphDataStore.data.first()
        val lat = prefs[latKey] ?: return null
        val lon = prefs[lonKey] ?: return null
        return Coordinates(lat, lon)
    }

    class WeatherCache(val forecastJson: String, val airJson: String?, val savedAtMillis: Long)

    suspend fun saveWeatherCache(forecastJson: String, airJson: String?) {
        context.glyphDataStore.edit { prefs ->
            prefs[forecastKey] = forecastJson
            if (airJson != null) prefs[airKey] = airJson else prefs.remove(airKey)
            prefs[weatherAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun weatherCache(): WeatherCache? {
        val prefs = context.glyphDataStore.data.first()
        val forecast = prefs[forecastKey] ?: return null
        return WeatherCache(forecast, prefs[airKey], prefs[weatherAtKey] ?: 0L)
    }

}
