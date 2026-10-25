package me.shadovxw.glyph.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Starts an activity from a tile click and pulls the Control Centre back up. */
@Suppress("DEPRECATION")
fun TileService.launchAndCollapse(intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= 34) {
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        startActivityAndCollapse(pending)
    } else {
        startActivityAndCollapse(intent)
    }
}

/** Sets a tile's state and its one-line subtitle in one go. */
fun Tile?.show(state: Int, subtitle: String?) {
    this ?: return
    this.state = state
    this.subtitle = subtitle
    updateTile()
}
