package me.shadovxw.glyph.launcher

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.IconTile

/**
 * DESIGN.md 2.4: two plain icons (Phone, Camera), centred, no background, 52 x 52 hit area, 22 icon.
 * There is no drawer button; the drawer opens with a swipe up.
 */
@Composable
fun Dock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(72.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = GlyphIcons.Phone,
            description = "Phone",
            onClick = { context.startSafely(Intent(Intent.ACTION_DIAL)) },
            modifier = Modifier.dockHit(),
        )
        IconTile(
            icon = GlyphIcons.Camera,
            description = "Camera",
            onClick = { context.startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            modifier = Modifier.dockHit(),
        )
    }
}

/** The outer size wins over IconTile's own 44 dp, giving the 52 dp dock hit area. */
private fun Modifier.dockHit(): Modifier = this.size(GlyphDimens.dockHit)
