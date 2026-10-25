package me.shadovxw.glyph.screens.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.launcher.SearchIntents
import me.shadovxw.glyph.launcher.openGoogleSearch
import me.shadovxw.glyph.launcher.openLens
import me.shadovxw.glyph.launcher.startSafely
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.IconTile

/**
 * DESIGN.md 3, screen 1: 48 tall, magnifier, "Search", then voice, Lens and incognito (44 x 44 each).
 * Transparent like every card (no fill, no border).
 */
@Composable
fun SearchBar(modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { context.openGoogleSearch() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                imageVector = GlyphIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(c.ink),
            )
            Spacer(Modifier.width(10.dp))
            BasicText("Search", style = GlyphType.rowPrimary.copy(color = c.inkDim))
        }
        IconTile(GlyphIcons.Mic, "Voice search", onClick = { context.startSafely(SearchIntents.voice()) })
        IconTile(GlyphIcons.Lens, "Google Lens", onClick = { context.openLens() })
        IconTile(GlyphIcons.Incognito, "Incognito tab", onClick = { context.startSafely(SearchIntents.incognito()) })
    }
}
