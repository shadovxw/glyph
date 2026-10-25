package me.shadovxw.glyph.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import me.shadovxw.glyph.theme.Glyph
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Where the 520 dp orbit group sits on each screen (DESIGN.md 5A). Values are CSS-style offsets in dp. */
private class OrbitSpot(val top: Float, private val left: Float?, private val right: Float?) {
    fun leftFor(screenWidthDp: Float): Float = left ?: (screenWidthDp - GroupSize - right!!)
}

private fun fromLeft(top: Float, left: Float) = OrbitSpot(top, left, null)
private fun fromRight(top: Float, right: Float) = OrbitSpot(top, null, right)

private val Spots = listOf(
    fromRight(top = -150f, right = -190f), // 1 Today
    fromLeft(top = -200f, left = -60f),    // 2 Music
    fromRight(top = -170f, right = -250f), // 3 WhatsApp
    fromRight(top = 380f, right = -260f),  // 4 GitHub
    fromRight(top = -170f, right = -200f), // 5 Apps
)

private const val GroupSize = 520f
private const val GridDp = 24f
private const val DotPanDp = 240f
private const val DotPanMillis = 60_000L
private const val OrbitMillis = 120_000L
private const val ReverseDotMillis = 70_000L
private const val BreatheMillis = 9_000L
private const val ParallaxDpPerPage = 60f
private const val RotationDegPerPage = 8f

/**
 * DESIGN.md 5A: the launcher's own home background. One clock drives everything and only runs
 * while the activity is started (paused on ON_STOP).
 *
 * [pagePosition] is `currentPage + swipe offset`; it is read while drawing, so swiping does not recompose.
 */
@Composable
fun OrbitBackground(
    pagePosition: () -> Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val c = Glyph.colors
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var timeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(animate, lifecycle) {
        if (!animate) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                timeMs += (now - last) / 1_000_000
                last = now
            }
        }
    }

    val dotsAlpha = if (c.isDark) 0.32f else 0.26f
    val orbitAlpha = if (c.isDark) 0.40f else 0.32f

    Canvas(modifier.fillMaxSize()) {
        val t = timeMs
        val pos = pagePosition().coerceIn(0f, (Spots.size - 1).toFloat())
        drawDots(t, pos, c.ink, dotsAlpha)
        drawOrbit(t, pos, c.ink, orbitAlpha)
    }
}

private fun DrawScope.drawDots(t: Long, pos: Float, ink: androidx.compose.ui.graphics.Color, baseAlpha: Float) {
    val grid = GridDp.dp.toPx()
    val pan = DotPanDp.dp.toPx() * ((t % DotPanMillis).toFloat() / DotPanMillis)
    val ox = positiveMod(pan - ParallaxDpPerPage.dp.toPx() * pos, grid)
    val oy = positiveMod(pan, grid)
    val radius = 0.5.dp.toPx()

    var y = oy
    while (y < size.height + grid) {
        // Fade top to bottom: 100% -> 70% at 60% height -> 45% at the bottom.
        val f = (y / size.height).coerceIn(0f, 1f)
        val fade = if (f <= 0.6f) lerp(1f, 0.70f, f / 0.6f) else lerp(0.70f, 0.45f, (f - 0.6f) / 0.4f)
        val color = ink.copy(alpha = baseAlpha * fade)
        var x = ox
        while (x < size.width + grid) {
            drawCircle(color, radius, Offset(x, y))
            x += grid
        }
        y += grid
    }
}

private fun DrawScope.drawOrbit(t: Long, pos: Float, ink: androidx.compose.ui.graphics.Color, alpha: Float) {
    val screenWidthDp = size.width / density
    val i = floor(pos).toInt().coerceIn(0, Spots.size - 1)
    val j = (i + 1).coerceAtMost(Spots.size - 1)
    val f = pos - i
    val leftDp = lerp(Spots[i].leftFor(screenWidthDp), Spots[j].leftFor(screenWidthDp), f) - ParallaxDpPerPage * pos
    val topDp = lerp(Spots[i].top, Spots[j].top, f)
    val center = Offset((leftDp + GroupSize / 2).dp.toPx(), (topDp + GroupSize / 2).dp.toPx())

    val color = ink.copy(alpha = alpha)
    val hair = Stroke(width = 1.dp.toPx())
    val dashed = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx())),
    )

    val spin = 360f * (t % OrbitMillis).toFloat() / OrbitMillis + RotationDegPerPage * pos
    rotate(degrees = spin, pivot = center) {
        drawCircle(color, 110.dp.toPx(), center, style = hair)
        drawCircle(color, 170.dp.toPx(), center, style = dashed)
        drawCircle(color, 235.dp.toPx(), center, style = hair)
        drawCircle(color, 5.dp.toPx(), onRing(center, 110.dp.toPx(), 40.0))
        drawCircle(color, 3.5.dp.toPx(), onRing(center, 170.dp.toPx(), 205.0))
    }

    // One more dot circles the other way around the outer ring, outside the group's rotation.
    val reverse = -360.0 * (t % ReverseDotMillis) / ReverseDotMillis
    drawCircle(color, 4.dp.toPx(), onRing(center, 235.dp.toPx(), reverse))

    // Small ring that breathes: scale 1 -> 1.08, opacity 50% -> 100%.
    val breathe = (1 - cos(2 * PI * (t % BreatheMillis) / BreatheMillis)).toFloat() / 2f
    drawCircle(
        color = ink.copy(alpha = alpha * (0.5f + 0.5f * breathe)),
        radius = 60.dp.toPx() * (1f + 0.08f * breathe),
        center = center,
        style = hair,
    )
}

private fun onRing(center: Offset, radius: Float, degrees: Double): Offset {
    val a = Math.toRadians(degrees)
    return Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat())
}

private fun positiveMod(v: Float, m: Float): Float = ((v % m) + m) % m
