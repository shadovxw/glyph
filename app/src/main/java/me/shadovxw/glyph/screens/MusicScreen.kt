package me.shadovxw.glyph.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.shadovxw.glyph.data.music.DevicesState
import me.shadovxw.glyph.data.music.MusicActions
import me.shadovxw.glyph.data.music.MusicState
import me.shadovxw.glyph.launcher.glyphCard
import me.shadovxw.glyph.screens.music.DevicesCard
import me.shadovxw.glyph.screens.music.PlayerCard
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.bottomFade

/** Screen 2 (DESIGN.md 3): the player and "Playing on". Scrolls vertically like Today. */
@Composable
fun MusicScreen(music: MusicState, devices: DevicesState, actions: MusicActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .bottomFade()
            .verticalScroll(rememberScrollState())
            .padding(
                start = GlyphDimens.screenSide,
                end = GlyphDimens.screenSide,
                top = GlyphDimens.screenTop,
                bottom = GlyphDimens.homeBottomClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap * 2),
    ) {
        PlayerCard(music, actions, Modifier.glyphCard(0))
        DevicesCard(devices, actions, Modifier.glyphCard(1))
    }
}
