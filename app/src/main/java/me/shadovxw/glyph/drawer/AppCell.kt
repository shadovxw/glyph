package me.shadovxw.glyph.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphType

private val IconSize = 56.dp

/** The app's own icon, unmodified, unless it is on the custom list (then a Glyph line icon on a round tile). */
@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier, size: Dp = IconSize) {
    val c = Glyph.colors
    val custom = CustomIcons.forPackage(app.packageName)
    if (custom != null) {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(c.inkTrack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = custom,
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                colorFilter = ColorFilter.tint(c.ink),
            )
        }
    } else {
        // The Play Store icon fills its whole slot and so looks bigger than the line icons, which sit inside a round tile
        // of the same size. Drawing it a little smaller, centred in the same slot, makes them match without moving anything.
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(size * PlayIconScale))
        }
    }
}

/** How much of its slot a Play Store icon fills (the custom line icons' tile fills all of it). */
private const val PlayIconScale = 0.8f

/** Icon with its label below (11 sp), as in the stock drawer. */
@Composable
fun AppCell(app: AppEntry, onLaunch: (AppEntry) -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(
        modifier = modifier
            .clickable { onLaunch(app) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppIcon(app)
        BasicText(
            text = app.label,
            style = GlyphType.rowPrimary.copy(color = c.ink, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}
