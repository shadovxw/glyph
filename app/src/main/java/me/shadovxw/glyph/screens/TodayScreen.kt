package me.shadovxw.glyph.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.shadovxw.glyph.data.ThemeMode
import me.shadovxw.glyph.data.today.TodayActions
import me.shadovxw.glyph.data.today.TodayData
import me.shadovxw.glyph.launcher.glyphCard
import me.shadovxw.glyph.screens.today.AirQualityCard
import me.shadovxw.glyph.screens.today.AppShortcuts
import me.shadovxw.glyph.screens.today.BatteryCard
import me.shadovxw.glyph.screens.today.ModeSwitch
import me.shadovxw.glyph.screens.today.MotionSwitch
import me.shadovxw.glyph.screens.today.NextEventCard
import me.shadovxw.glyph.screens.today.ScreenTimeCard
import me.shadovxw.glyph.screens.today.SearchBar
import me.shadovxw.glyph.screens.today.StepsCard
import me.shadovxw.glyph.screens.today.SunCard
import me.shadovxw.glyph.screens.today.WeatherCard
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.bottomFade
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dayFmt = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/** Room kept under the content for the page indicator and dock, so the last card can scroll clear of them. */
private val BottomClearance = 150.dp

/**
 * Screen 1 (DESIGN.md 3): clock block with the mode switch, search bar, weather / battery / screen time row,
 * the next event, air quality and sun, steps, and the app shortcuts. Every value comes from a real source
 * (data/today); a card without its data shows a dash and a tap-to-allow hint, never a made-up number.
 *
 * The page scrolls vertically. Each card gets a [glyphCard] index so the page transition can cascade them
 * top to bottom. Upward drags left over at the bottom of the page open the drawer (see HomeScreen).
 */
@Composable
fun TodayScreen(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    motion: Boolean,
    onSetMotion: (Boolean) -> Unit,
    today: TodayData,
    actions: TodayActions,
) {
    val c = Glyph.colors
    // Truncated to the minute so the screen only recomposes when the minute changes.
    val now by produceState(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) {
        while (true) {
            value = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            delay(1_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bottomFade()
            .verticalScroll(rememberScrollState())
            .padding(
                start = GlyphDimens.screenSide,
                end = GlyphDimens.screenSide,
                top = GlyphDimens.screenTop,
                bottom = BottomClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap),
    ) {
        // 1. Clock block + mode switch (right, bottom-aligned)
        Row(
            modifier = Modifier.glyphCard(0).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(now.format(dayFmt), style = GlyphType.rowPrimary.copy(color = c.ink))
                BasicText(now.format(timeFmt), style = GlyphType.homeClock.copy(color = c.ink))
                BasicText(
                    now.format(dateFmt) + (today.weather?.let { " · ${it.tempNow}° ${it.condition}" } ?: ""),
                    style = GlyphType.rowSecondary.copy(color = c.inkDim),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            MotionSwitch(motion = motion, onSetMotion = onSetMotion)
            ModeSwitch(mode = themeMode, onSetMode = onSetThemeMode)
        }

        // 2. Search
        SearchBar(Modifier.glyphCard(1))

        // 3. Weather | Battery over Screen time
        Row(
            modifier = Modifier.glyphCard(2).fillMaxWidth().height(156.dp),
            horizontalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap),
        ) {
            WeatherCard(today.weather, today.weatherState, actions.grantLocation, Modifier.weight(1f).fillMaxHeight())
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap),
            ) {
                BatteryCard(Modifier.weight(1f).fillMaxWidth())
                ScreenTimeCard(today.screenTime, today.screenTimeState, actions.grantUsageAccess, Modifier.weight(1f).fillMaxWidth())
            }
        }

        // 4. Next event
        NextEventCard(today.nextEvent, today.calendarState, now, actions.grantCalendar, Modifier.glyphCard(3))

        // 5. Air quality | Sun
        Row(
            modifier = Modifier.glyphCard(4).fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap),
        ) {
            AirQualityCard(today.aqi, today.weatherState, actions.grantLocation, Modifier.weight(1f).fillMaxHeight())
            SunCard(today.weather, now, Modifier.weight(1f).fillMaxHeight())
        }

        // 6. Steps
        StepsCard(today.steps, today.stepsState, actions.grantSteps, Modifier.glyphCard(5).fillMaxWidth())

        // 7. App shortcuts
        AppShortcuts(Modifier.glyphCard(6))
    }
}
