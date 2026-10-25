package me.shadovxw.glyph.tiles

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Homelab (DESIGN.md 7): lit while the server answers (see [HomelabCheck]). A tap opens its address. */
class HomelabTile : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var check: Job? = null

    override fun onStartListening() {
        if (HomelabCheck.address == null) {
            qsTile.show(Tile.STATE_INACTIVE, "Not set up")
            return
        }
        qsTile.show(Tile.STATE_INACTIVE, "Checking")
        check?.cancel()
        check = scope.launch {
            val up = HomelabCheck.isUp() == true
            qsTile.show(if (up) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE, if (up) "Up" else "Down")
        }
    }

    override fun onClick() {
        val uri = HomelabCheck.address ?: return
        launchAndCollapse(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
