package me.shadovxw.glyph.data.today

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import java.time.LocalTime

/** Where a card's data stands. Cards never invent values: without data they show a dash and say why. */
enum class SourceState { Loading, Ready, NeedsPermission, Unavailable }

@Immutable
class HourlyTemp(val hour: Int, val temp: Int)

@Immutable
class WeatherData(
    val tempNow: Int,
    val condition: String,
    val high: Int,
    val low: Int,
    val hourly: List<HourlyTemp>,
    val sunrise: LocalTime,
    val sunset: LocalTime,
)

@Immutable
class AirQualityData(val aqi: Int, val category: String)

@Immutable
class ScreenTimeData(val todayMillis: Long, val deltaPercent: Int?)

@Immutable
class EventData(val title: String, val begin: LocalDateTime, val end: LocalDateTime)

@Immutable
data class TodayData(
    val weather: WeatherData? = null,
    val aqi: AirQualityData? = null,
    val weatherState: SourceState = SourceState.Loading,
    val screenTime: ScreenTimeData? = null,
    val screenTimeState: SourceState = SourceState.Loading,
    /** Null with a Ready state means there is nothing coming up. */
    val nextEvent: EventData? = null,
    val calendarState: SourceState = SourceState.Loading,
    val steps: Int? = null,
    val stepsState: SourceState = SourceState.Loading,
)

/** What a card does when its data needs the user to allow something. */
@Immutable
class TodayActions(
    val grantLocation: () -> Unit,
    val grantCalendar: () -> Unit,
    val grantUsageAccess: () -> Unit,
    val grantSteps: () -> Unit,
) {
    companion object {
        val None = TodayActions({}, {}, {}, {})
    }
}

/** DESIGN.md 3, screen 1. Becomes a setting later. */
const val StepGoal = 8_000
/** The air quality scale bar runs 0 to this; higher values pin to the end. */
const val AqiScaleMax = 300
