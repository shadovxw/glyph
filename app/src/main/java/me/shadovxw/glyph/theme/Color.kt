package me.shadovxw.glyph.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** DESIGN.md 2.1. Black and white only; the one exception is [accentRed]. */
@Immutable
data class GlyphColors(
    val isDark: Boolean,
    val screenBg: Color,
    val ink: Color,
    val inkDim: Color,
    val inkLine: Color,
    val inkTrack: Color,
    val chipBg: Color,
    val chipText: Color,
    val accentRed: Color,
)

private val Red = Color(0xFFD7262E)

val LightColors = GlyphColors(
    isDark = false,
    screenBg = Color(0xFFF6F6F6),
    ink = Color(0xFF000000),
    inkDim = Color(0xFF000000).copy(alpha = 0.62f),
    inkLine = Color(0xFF000000).copy(alpha = 0.09f),
    inkTrack = Color(0xFF000000).copy(alpha = 0.14f),
    chipBg = Color(0xFF000000),
    chipText = Color(0xFFFFFFFF),
    accentRed = Red,
)

val DarkColors = GlyphColors(
    isDark = true,
    screenBg = Color(0xFF000000),
    ink = Color(0xFFFFFFFF),
    inkDim = Color(0xFFFFFFFF).copy(alpha = 0.62f),
    inkLine = Color(0xFFFFFFFF).copy(alpha = 0.09f),
    inkTrack = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    chipBg = Color(0xFFFFFFFF),
    chipText = Color(0xFF000000),
    accentRed = Red,
)

val LocalGlyphColors = staticCompositionLocalOf { LightColors }

object Glyph {
    val colors: GlyphColors
        @androidx.compose.runtime.Composable
        @ReadOnlyComposable
        get() = LocalGlyphColors.current
}
