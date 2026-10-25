package me.shadovxw.glyph.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.today.AirQualityData
import me.shadovxw.glyph.data.today.AqiScaleMax
import me.shadovxw.glyph.data.today.SourceState
import me.shadovxw.glyph.data.today.StepGoal
import me.shadovxw.glyph.data.today.WeatherData
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphProgressBar
import me.shadovxw.glyph.theme.GlyphType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

/** Air quality (US AQI from Open-Meteo): number, category, and a scale bar with a marker. */
@Composable
fun AirQualityCard(aqi: AirQualityData?, state: SourceState, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            BasicText("Air quality", style = GlyphType.label.copy(color = c.inkDim))
            BasicText(aqi?.aqi?.toString() ?: "–", style = GlyphType.mediumStat.copy(color = c.ink))
            if (aqi != null) {
                BasicText(aqi.category, style = GlyphType.rowSecondary.copy(color = c.inkDim))
            } else {
                StateHint(state, "Allow location", "Not available", onGrant)
            }
        }
        if (aqi != null) AqiScale(aqi.aqi / AqiScaleMax.toFloat())
    }
}

/** A 3 dp track with a 2 x 9 dp marker at [fraction] of the way along. */
@Composable
private fun AqiScale(fraction: Float) {
    val c = Glyph.colors
    val f = fraction.coerceIn(0.01f, 0.99f)
    Box(Modifier.fillMaxWidth().height(9.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(c.inkTrack))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(f))
            Box(Modifier.width(2.dp).height(9.dp).background(c.ink))
            Spacer(Modifier.weight(1f - f))
        }
    }
}

/** Sun: a dotted arc from sunrise to sunset with the sun's current position, and both times in mono. */
@Composable
fun SunCard(weather: WeatherData?, now: LocalDateTime, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val sunrise: LocalTime? = weather?.sunrise
    val sunset: LocalTime? = weather?.sunset
    val daylight = if (sunrise != null && sunset != null) ChronoUnit.MINUTES.between(sunrise, sunset).toFloat() else 0f
    val elapsed = if (sunrise != null) ChronoUnit.MINUTES.between(sunrise, now.toLocalTime()).toFloat() else 0f
    val progress = if (daylight > 0f) (elapsed / daylight).coerceIn(0f, 1f) else 0f
    val isDay = daylight > 0f && elapsed in 0f..daylight

    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        BasicText("Sun", style = GlyphType.label.copy(color = c.inkDim))
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val w = size.width
            val h = size.height
            // Horizon, then the dotted half-ellipse above it.
            drawLine(c.inkLine, Offset(0f, h), Offset(w, h), strokeWidth = 1.dp.toPx())
            drawArc(
                color = c.inkDim,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = Size(w, h * 2f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx())),
                ),
            )
            if (weather != null) {
                // The sun sits at its position along the arc; at night it rests on the horizon, dimmed.
                val angle = Math.toRadians(180.0 + 180.0 * progress)
                val sun = Offset(
                    x = w / 2f + (w / 2f) * cos(angle).toFloat(),
                    y = h + h * sin(angle).toFloat(),
                )
                drawCircle(if (isDay) c.ink else c.inkDim, radius = 4.dp.toPx(), center = sun)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(sunrise?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "–", style = GlyphType.meta.copy(color = c.inkDim))
            BasicText(sunset?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "–", style = GlyphType.meta.copy(color = c.inkDim))
        }
    }
}

/** Steps from Health Connect: count, "of 8,000", and a progress bar. */
@Composable
fun StepsCard(steps: Int?, state: SourceState, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("Steps", style = GlyphType.label.copy(color = c.inkDim))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText(steps?.let { "%,d".format(it) } ?: "–", style = GlyphType.monoStat.copy(color = c.ink))
            BasicText(
                " of %,d".format(StepGoal),
                style = GlyphType.rowSecondary.copy(color = c.inkDim),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        GlyphProgressBar((steps ?: 0) / StepGoal.toFloat())
        if (steps == null) StateHint(state, "Allow Health Connect", "Health Connect not available", onGrant)
    }
}
