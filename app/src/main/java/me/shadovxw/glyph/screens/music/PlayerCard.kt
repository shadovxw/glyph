package me.shadovxw.glyph.screens.music

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.shadovxw.glyph.data.music.MusicAccess
import me.shadovxw.glyph.data.music.MusicActions
import me.shadovxw.glyph.data.music.MusicSession
import me.shadovxw.glyph.data.music.MusicState
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphProgressBar
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.IconTile

/** Guard rails for odd pictures: very tall or very wide art is letterboxed rather than taking over the page. */
private const val MinArtRatio = 0.6f
private const val MaxArtRatio = 2.0f

/**
 * DESIGN.md 3, screen 2: the player. Transparent card with no inner padding. The art is greyscale. Without
 * notification access, or with nothing playing, it says so and offers the one tap that fixes it.
 */
@Composable
fun PlayerCard(state: MusicState, actions: MusicActions, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val session = state.session
    // Tapping anywhere on the card opens the music app; the buttons inside take their own taps first.
    Column(modifier.clickable(onClick = actions.openMusicApp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText(session?.appName ?: "Music", style = GlyphType.label.copy(color = c.inkDim))
            if (session?.isPlaying == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.ink))
                    Spacer(Modifier.size(6.dp))
                    BasicText("Playing", style = GlyphType.label.copy(color = c.inkDim))
                }
            }
        }

        // The art is shown whole, at the shape the music app gives it (a square cover is a square, a wide video
        // thumbnail is wide): the box takes the picture's own aspect ratio, so nothing is cropped.
        val art = session?.art
        val shape = if (art != null) (art.width.toFloat() / art.height).coerceIn(MinArtRatio, MaxArtRatio) else 1f
        Box(Modifier.fillMaxWidth().aspectRatio(shape).background(c.inkTrack)) {
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                )
            }
        }

        when {
            state.access == MusicAccess.NeedsNotificationAccess ->
                Prompt("Allow notification access", "Needed to see what is playing", actions.allowNotificationAccess)
            session == null -> Prompt("Nothing playing", "Open YouTube Music", actions.openMusicApp)
            // (The whole card opens the app too, so "Nothing playing" works as a button as well.)
            else -> NowPlaying(session, actions)
        }
    }
}

@Composable
private fun Prompt(title: String, action: String, onClick: () -> Unit) {
    val c = Glyph.colors
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)) {
        BasicText(title, style = GlyphType.screenTitle.copy(color = c.ink))
        BasicText(action, style = GlyphType.rowSecondary.copy(color = c.inkDim))
    }
}

@Composable
private fun NowPlaying(session: MusicSession, actions: MusicActions) {
    val c = Glyph.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    session.title,
                    style = GlyphType.screenTitle.copy(color = c.ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOf(session.artist, session.album).filter { it.isNotBlank() }.joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    BasicText(
                        subtitle,
                        style = GlyphType.rowSecondary.copy(color = c.inkDim),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconTile(
                GlyphIcons.ThumbDown, "Dislike", actions.dislike,
                tint = stateTint(session.canDislike, session.disliked),
            )
            IconTile(
                GlyphIcons.ThumbUp, "Like", actions.like,
                tint = stateTint(session.canLike, session.liked),
            )
        }

        Progress(session)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconTile(GlyphIcons.Shuffle, "Shuffle", actions.shuffle, tint = stateTint(session.canShuffle, session.shuffleOn))
            IconTile(GlyphIcons.Previous, "Previous", actions.previous)
            // Pause/play: a 64 x 64 filled square, as the design asks.
            Box(
                Modifier.size(64.dp).background(c.chipBg).clickable(onClick = actions.playPause),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = if (session.isPlaying) GlyphIcons.Pause else GlyphIcons.Play,
                    contentDescription = if (session.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(c.chipText),
                )
            }
            IconTile(GlyphIcons.Next, "Next", actions.next)
            IconTile(GlyphIcons.Repeat, "Repeat", actions.repeat, tint = stateTint(session.canRepeat, session.repeatOn))
        }
    }
}

/** Ink when on, dim when off, and fainter still when the music app does not offer it. */
@Composable
private fun stateTint(available: Boolean, on: Boolean) = when {
    !available -> Glyph.colors.inkDim.copy(alpha = 0.35f)
    on -> Glyph.colors.ink
    else -> Glyph.colors.inkDim
}

@Composable
private fun Progress(session: MusicSession) {
    val c = Glyph.colors
    // Ticks twice a second while playing; the position itself is worked out from the last report.
    val now by produceState(SystemClock.elapsedRealtime(), session.isPlaying, session.reportedAt, session.positionMs) {
        value = SystemClock.elapsedRealtime()
        while (isActive && session.isPlaying) {
            delay(500)
            value = SystemClock.elapsedRealtime()
        }
    }
    val position = session.positionAt(now)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlyphProgressBar(if (session.durationMs > 0) position / session.durationMs.toFloat() else 0f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(clock(position), style = GlyphType.meta.copy(color = c.inkDim))
            BasicText(if (session.durationMs > 0) clock(session.durationMs) else "", style = GlyphType.meta.copy(color = c.inkDim))
        }
    }
}

private fun clock(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
