package me.shadovxw.glyph.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GlyphTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGlyphColors provides if (dark) DarkColors else LightColors,
        content = content,
    )
}
