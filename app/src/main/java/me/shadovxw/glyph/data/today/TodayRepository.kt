package me.shadovxw.glyph.data.today

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.SettingsRepository

/**
 * Everything the Today screen shows, from real sources (DESIGN.md 8). Each source reports its own state, so one
 * missing permission only affects its own cards. Call [refresh] when Glyph comes forward and after a permission
 * is granted. The weather is cached so the cards fill in straight away on the next start.
 */
class TodayRepository(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val location = LocationSource(context, settings)
    val calendar = CalendarSource(context)
    val screenTime = ScreenTimeSource(context)
    val steps = StepsSource(context)
    private val weather = WeatherSource()

    private val _state = MutableStateFlow(TodayData())
    val state: StateFlow<TodayData> = _state.asStateFlow()

    private var lastRefreshAt = 0L
    private var job: Job? = null

    init {
        scope.launch { showCachedWeather() }
    }

    /** Skips the work if it ran less than a minute ago, unless [force] (used after a permission is granted). */
    fun refresh(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && job?.isActive == true) return
        if (!force && now - lastRefreshAt < MinRefreshMillis) return
        lastRefreshAt = now
        job?.cancel()
        job = scope.launch {
            launch { refreshWeather() }
            launch { refreshCalendar() }
            launch { refreshScreenTime() }
            launch { refreshSteps() }
        }
    }

    // ---- weather, air quality, sunrise/sunset ----

    private suspend fun showCachedWeather() {
        val cache = settings.weatherCache() ?: return
        if (System.currentTimeMillis() - cache.savedAtMillis > CacheMaxAgeMillis) return
        runCatching {
            val data = WeatherSource.parseWeather(cache.forecastJson)
            val aqi = cache.airJson?.let(WeatherSource::parseAirQuality)
            _state.update { if (it.weather == null) it.copy(weather = data, aqi = aqi, weatherState = SourceState.Ready) else it }
        }
    }

    private suspend fun refreshWeather() {
        if (!location.hasPermission()) {
            _state.update { if (it.weather == null) it.copy(weatherState = SourceState.NeedsPermission) else it }
            return
        }
        val coordinates = location.current()
        if (coordinates == null) {
            _state.update { if (it.weather == null) it.copy(weatherState = SourceState.Unavailable) else it }
            return
        }
        runCatching { weather.fetch(coordinates) }
            .onSuccess { bundle ->
                _state.update { it.copy(weather = bundle.weather, aqi = bundle.aqi, weatherState = SourceState.Ready) }
                settings.saveWeatherCache(bundle.forecastJson, bundle.airJson)
            }
            .onFailure {
                // Offline or the service is down: keep whatever is already shown.
                _state.update { if (it.weather == null) it.copy(weatherState = SourceState.Unavailable) else it }
            }
    }

    // ---- calendar ----

    private suspend fun refreshCalendar() {
        if (!calendar.hasPermission()) {
            _state.update { it.copy(nextEvent = null, calendarState = SourceState.NeedsPermission) }
            return
        }
        runCatching { calendar.nextEvent() }
            .onSuccess { event -> _state.update { it.copy(nextEvent = event, calendarState = SourceState.Ready) } }
            .onFailure { _state.update { it.copy(nextEvent = null, calendarState = SourceState.Unavailable) } }
    }

    // ---- screen time ----

    private suspend fun refreshScreenTime() {
        if (!screenTime.hasAccess()) {
            _state.update { it.copy(screenTime = null, screenTimeState = SourceState.NeedsPermission) }
            return
        }
        runCatching { screenTime.read() }
            .onSuccess { data -> _state.update { it.copy(screenTime = data, screenTimeState = SourceState.Ready) } }
            .onFailure { _state.update { it.copy(screenTime = null, screenTimeState = SourceState.Unavailable) } }
    }

    // ---- steps (Health Connect) ----

    private suspend fun refreshSteps() {
        if (!steps.isAvailable()) {
            _state.update { it.copy(steps = null, stepsState = SourceState.Unavailable) }
            return
        }
        runCatching {
            if (!steps.hasPermission()) {
                _state.update { it.copy(steps = null, stepsState = SourceState.NeedsPermission) }
            } else {
                val today = steps.today()
                _state.update { it.copy(steps = today?.toInt() ?: 0, stepsState = SourceState.Ready) }
            }
        }.onFailure { _state.update { it.copy(steps = null, stepsState = SourceState.Unavailable) } }
    }

    private companion object {
        const val MinRefreshMillis = 60_000L
        const val CacheMaxAgeMillis = 6L * 60 * 60 * 1000
    }
}
