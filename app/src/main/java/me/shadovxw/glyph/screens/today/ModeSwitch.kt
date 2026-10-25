package me.shadovxw.glyph.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.ThemeMode
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.IconTile

/**
 * Start / stop for the live animation, beside the mode switch: play (animation running) over pause (stopped),
 * built exactly like [ModeSwitch]: 44 x 44, 8 gap, transparent, the active one in full ink.
 */
@Composable
fun MotionSwitch(motion: Boolean, onSetMotion: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconTile(
            icon = GlyphIcons.Play,
            description = "Start animation",
            onClick = { onSetMotion(true) },
            tint = if (motion) c.ink else c.inkDim,
        )
        IconTile(
            icon = GlyphIcons.Pause,
            description = "Stop animation",
            onClick = { onSetMotion(false) },
            tint = if (motion) c.inkDim else c.ink,
        )
    }
}

/**
 * DESIGN.md 3, screen 1: stacked 44 x 44 hit areas with transparent backgrounds, the active one in full ink:
 * **Auto** (follow the phone's dark / light setting, the default), **Dark** (moon) and **Light** (sun).
 */
@Composable
fun ModeSwitch(mode: ThemeMode, onSetMode: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier) {
        IconTile(
            icon = GlyphIcons.AutoTheme,
            description = "Follow the phone's dark mode setting",
            onClick = { onSetMode(ThemeMode.System) },
            tint = if (mode == ThemeMode.System) c.ink else c.inkDim,
        )
        IconTile(
            icon = GlyphIcons.Moon,
            description = "Dark mode",
            onClick = { onSetMode(ThemeMode.Dark) },
            tint = if (mode == ThemeMode.Dark) c.ink else c.inkDim,
        )
        IconTile(
            icon = GlyphIcons.Sun,
            description = "Light mode",
            onClick = { onSetMode(ThemeMode.Light) },
            tint = if (mode == ThemeMode.Light) c.ink else c.inkDim,
        )
    }
}
