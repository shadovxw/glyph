package me.shadovxw.glyph.tiles

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Hotspot QR (DESIGN.md 7): a button that opens the hotspot's QR code full screen. Never lit. */
class HotspotQrTile : TileService() {
    override fun onStartListening() = qsTile.show(Tile.STATE_INACTIVE, if (HotspotStore(this).isSet) null else "Set up")

    override fun onClick() = launchAndCollapse(Intent(this, HotspotQrActivity::class.java))
}
