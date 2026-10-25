package me.shadovxw.glyph.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A plain line icon with a transparent background and a 44 x 44 hit area (DESIGN.md 1 and 2.5).
 * [tint] is the only state: full `ink` when active, `ink.dim` otherwise.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Glyph.colors.ink,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(GlyphDimens.minTouch)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

/** Fades the bottom [height] of scrolling content to transparent, so it dissolves behind the indicator and dock. */
fun Modifier.bottomFade(height: Dp = 130.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = height.toPx().coerceAtMost(size.height)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - fade,
                endY = size.height,
            ),
            topLeft = Offset(0f, size.height - fade),
            size = Size(size.width, fade),
            blendMode = BlendMode.DstIn,
        )
    }

/** DESIGN.md 2.4: 3 dp tall, square ends. */
@Composable
fun GlyphProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Glyph.colors.ink) {
    val c = Glyph.colors
    Box(modifier.fillMaxWidth().height(GlyphDimens.progressHeight).background(c.inkTrack)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}
