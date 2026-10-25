package me.shadovxw.glyph.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.shadovxw.glyph.data.LowBatteryPercent
import me.shadovxw.glyph.data.rememberBatteryPercent
import me.shadovxw.glyph.data.today.EventData
import me.shadovxw.glyph.data.today.ScreenTimeData
import me.shadovxw.glyph.data.today.SourceState
import me.shadovxw.glyph.data.today.WeatherData
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphProgressBar
import me.shadovxw.glyph.theme.GlyphType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The line under a value when there is nothing to show: it says why, and when the user can fix it, a tap does.
 * Nothing here ever shows an invented number.
 */
@Composable
fun StateHint(state: SourceState, allowText: String, unavailableText: String, onGrant: () -> Unit) {
    val c = Glyph.colors
    when (state) {
        SourceState.NeedsPermission -> BasicText(
            text = allowText,
            style = GlyphType.rowSecondary.copy(color = c.ink),
            modifier = Modifier.clickable(onClick = onGrant).padding(vertical = 6.dp),
        )
        SourceState.Unavailable -> BasicText(unavailableText, style = GlyphType.rowSecondary.copy(color = c.inkDim))
        else -> Unit
    }
}

/** Transparent cards: content sits straight on the background, no inner padding (DESIGN.md 2.3 and 2.4). */
@Composable
fun WeatherCard(weather: WeatherData?, state: SourceState, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            BasicText("Weather", style = GlyphType.label.copy(color = c.inkDim))
            BasicText(weather?.let { "${it.tempNow}°" } ?: "–", style = GlyphType.bigStat.copy(color = c.ink))
            if (weather != null) {
                BasicText(
                    "${weather.condition} · H ${weather.high}°  L ${weather.low}°",
                    style = GlyphType.rowSecondary.copy(color = c.inkDim),
                )
            } else {
                StateHint(state, "Allow location", "Weather unavailable", onGrant)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weather?.hourly?.forEach { hour ->
                Column(Modifier.weight(1f)) {
                    BasicText("%02d".format(hour.hour), style = GlyphType.meta.copy(color = c.inkDim))
                    BasicText("${hour.temp}°", style = GlyphType.rowSecondary.copy(color = c.ink))
                }
            }
        }
    }
}

@Composable
fun BatteryCard(modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val percent by rememberBatteryPercent()
    CompactCard(modifier) {
        BasicText("Battery", style = GlyphType.label.copy(color = c.inkDim))
        BasicText(
            if (percent >= 0) "$percent%" else "–",
            style = GlyphType.monoStat.copy(color = c.ink, fontSize = 17.sp, lineHeight = 20.sp),
        )
        // The only colour on Today: the bar turns red below 20%.
        GlyphProgressBar(
            fraction = percent.coerceAtLeast(0) / 100f,
            color = if (percent in 0 until LowBatteryPercent) c.accentRed else c.ink,
        )
    }
}

@Composable
fun ScreenTimeCard(data: ScreenTimeData?, state: SourceState, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    CompactCard(modifier) {
        BasicText("Screen time", style = GlyphType.label.copy(color = c.inkDim))
        BasicText(
            data?.let { formatDuration(it.todayMillis) } ?: "–",
            style = GlyphType.monoStat.copy(color = c.ink, fontSize = 17.sp, lineHeight = 20.sp),
        )
        if (data != null) {
            BasicText(deltaLabel(data.deltaPercent), style = GlyphType.rowSecondary.copy(color = c.inkDim))
        } else {
            StateHint(state, "Allow usage access", "Not available", onGrant)
        }
    }
}

/** "2h 14m", or "45m" under an hour. */
internal fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000).toInt()
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${"%02d".format(minutes % 60)}m"
}

private fun deltaLabel(percent: Int?): String = when {
    percent == null -> "No history yet"
    percent < 0 -> "−${-percent}% vs. avg"
    percent > 0 -> "+$percent% vs. avg"
    else -> "Same as avg"
}

/** Compact stat card: transparent, so no inner padding (the 14 x 16 padding is for frosted and solid cards). */
@Composable
private fun CompactCard(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) { content() }
}

private val monthFmt = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
private val dayOfMonthFmt = DateTimeFormatter.ofPattern("d", Locale.getDefault())
private val clockFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

@Composable
fun NextEventCard(event: EventData?, state: SourceState, now: LocalDateTime, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val shownDate = event?.begin ?: now
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Date chip: filled square, inverts with the mode.
        Box(Modifier.size(48.dp).background(c.chipBg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(shownDate.format(monthFmt).uppercase(), style = GlyphType.smallest.copy(color = c.chipText))
                BasicText(
                    shownDate.format(dayOfMonthFmt),
                    style = GlyphType.rowPrimary.copy(color = c.chipText, fontWeight = FontWeight(500)),
                )
            }
        }
        Column {
            when {
                event != null -> {
                    BasicText(relativeLabel(now, event.begin), style = GlyphType.label.copy(color = c.inkDim))
                    BasicText(event.title, style = GlyphType.rowPrimary.copy(color = c.ink), maxLines = 1)
                    BasicText(
                        "${event.begin.format(clockFmt)} – ${event.end.format(clockFmt)}",
                        style = GlyphType.meta.copy(color = c.inkDim),
                    )
                }
                state == SourceState.Ready -> {
                    BasicText("Next", style = GlyphType.label.copy(color = c.inkDim))
                    BasicText("Nothing coming up", style = GlyphType.rowPrimary.copy(color = c.ink))
                }
                else -> {
                    BasicText("Next", style = GlyphType.label.copy(color = c.inkDim))
                    StateHint(state, "Allow calendar", "Calendar unavailable", onGrant)
                }
            }
        }
    }
}

/** "Next · in 49 min", "Next · in 2h 15m", or "Next · Tue 10:30" when it is a day or more away. */
internal fun relativeLabel(now: LocalDateTime, begin: LocalDateTime): String {
    val minutes = ChronoUnit.MINUTES.between(now, begin)
    return when {
        minutes <= 0 -> "Now"
        minutes < 60 -> "Next · in $minutes min"
        minutes < 24 * 60 -> "Next · in ${minutes / 60}h" + if (minutes % 60 == 0L) "" else " ${minutes % 60}m"
        else -> "Next · ${begin.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${begin.format(clockFmt)}"
    }
}
