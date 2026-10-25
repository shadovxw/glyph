package me.shadovxw.glyph.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Radius = 170.dp
private val ItemRing = 118.dp
private val ItemWidth = 72.dp
private val ItemHeight = 32.dp

/**
 * Tap the page indicator and this opens: a half circle rising from it with every page on an arc, so any page is one tap
 * away from any other. The current page is filled. [anchor] is the middle of the circle's flat edge, in the pixels of
 * the screen it is drawn over. A tap anywhere outside closes it.
 */
@Composable
fun PageSwitcher(
    names: List<String>,
    current: Int,
    anchor: Offset,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val c = Glyph.colors
    val density = LocalDensity.current

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val t by animateFloatAsState(if (shown) 1f else 0f, tween(260, easing = GlyphEasing), label = "pageSwitcher")

    val radiusPx = with(density) { Radius.toPx() }
    val ringPx = with(density) { ItemRing.toPx() }
    val itemWidthPx = with(density) { ItemWidth.toPx() }
    val itemHeightPx = with(density) { ItemHeight.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.screenBg.copy(alpha = 0.35f * t))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Box(
            Modifier
                .offset { IntOffset((anchor.x - radiusPx).roundToInt(), (anchor.y - radiusPx).roundToInt()) }
                .width(Radius * 2)
                .height(Radius)
                .graphicsLayer {
                    scaleX = 0.7f + 0.3f * t
                    scaleY = 0.7f + 0.3f * t
                    alpha = t
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .pointerInput(Unit) { detectTapGestures { } }, // taps on the disc itself do not close it
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val full = Size(size.width, size.width)
                drawArc(c.screenBg, 180f, 180f, useCenter = true, topLeft = Offset.Zero, size = full)
                drawArc(c.inkLine, 180f, 180f, useCenter = false, topLeft = Offset.Zero, size = full, style = Stroke(1.dp.toPx()))
            }
            names.forEachIndexed { index, name ->
                val angle = PI - (index + 0.5) * PI / names.size
                val x = radiusPx + ringPx * cos(angle).toFloat() - itemWidthPx / 2
                val y = radiusPx - ringPx * sin(angle).toFloat() - itemHeightPx / 2
                val isCurrent = index == current
                Box(
                    Modifier
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(ItemWidth, ItemHeight)
                        .background(if (isCurrent) c.chipBg else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onPick(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(name, style = GlyphType.rowPrimary.copy(color = if (isCurrent) c.chipText else c.ink))
                }
            }
        }
    }
}
