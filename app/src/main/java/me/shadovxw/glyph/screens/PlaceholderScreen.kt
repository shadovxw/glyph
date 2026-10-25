package me.shadovxw.glyph.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.shadovxw.glyph.launcher.glyphCard
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphType

/** Stand-in for screens 2-7 until their sample-data step. */
@Composable
fun PlaceholderScreen(title: String) {
    val c = Glyph.colors
    Column(
        Modifier
            .glyphCard(0)
            .fillMaxSize()
            .padding(horizontal = GlyphDimens.screenSide, vertical = GlyphDimens.screenTop)
    ) {
        BasicText(title, style = GlyphType.screenTitle.copy(color = c.ink))
    }
}
