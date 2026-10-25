package me.shadovxw.glyph.screens.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.launcher.startSafely
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType

private class Shortcut(val label: String, val icon: ImageVector, val packages: List<String>)

/** DESIGN.md 3, screen 1: the shortcut order is fixed. The first installed package in each list is opened. */
private val Shortcuts = listOf(
    Shortcut("Instagram", GlyphIcons.Instagram, listOf("com.instagram.android")),
    Shortcut("Chess.com", GlyphIcons.Chess, listOf("com.chess")),
    Shortcut("WhatsApp", GlyphIcons.WhatsApp, listOf("com.whatsapp")),
    Shortcut("GPay", GlyphIcons.GPay, listOf("com.google.android.apps.nbu.paisa.user")),
    Shortcut("Maps", GlyphIcons.MapPin, listOf("com.google.android.apps.maps")),
    Shortcut("Teams", GlyphIcons.Teams, listOf("com.microsoft.teams")),
    Shortcut("Outlook", GlyphIcons.Outlook, listOf("com.microsoft.office.outlook")),
    Shortcut("Gmail", GlyphIcons.Gmail, listOf("com.google.android.gm")),
    Shortcut("Authenticator", GlyphIcons.Authenticator, listOf("com.azure.authenticator", "com.google.android.apps.authenticator2")),
)

private const val Columns = 5

/** Plain 24 dp line icons with a label (9.5 sp), 5 per row, opening the real apps. */
@Composable
fun AppShortcuts(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Shortcuts.chunked(Columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { ShortcutCell(it, Modifier.weight(1f)) }
                repeat(Columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ShortcutCell(shortcut: Shortcut, modifier: Modifier) {
    val c = Glyph.colors
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clickable {
                shortcut.packages
                    .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
                    ?.let { context.startSafely(it) }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            imageVector = shortcut.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(c.ink),
        )
        BasicText(
            text = shortcut.label,
            style = GlyphType.rowSecondary.copy(color = c.inkDim, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
