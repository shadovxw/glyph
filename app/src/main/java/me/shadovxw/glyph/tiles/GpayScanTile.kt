package me.shadovxw.glyph.tiles

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

private const val GPay = "com.google.android.apps.nbu.paisa.user"

/** GPay scan (DESIGN.md 7): a button, never lit. It opens GPay straight on its QR scanner. */
class GpayScanTile : TileService() {
    override fun onStartListening() = qsTile.show(Tile.STATE_INACTIVE, null)

    override fun onClick() = launchAndCollapse(Intent(this, GpayScanActivity::class.java))
}

/**
 * See-through stop between the tile and GPay. As the Home app, Glyph may start GPay's own "Scan any QR code" shortcut
 * (`QR_SCANNER`); an app cannot do that from a tile directly, only from a screen that is showing. If GPay does not
 * offer the shortcut, it just opens GPay.
 */
class GpayScanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!startScanner()) {
            packageManager.getLaunchIntentForPackage(GPay)?.let { startActivity(it) }
        }
        finish()
    }

    private fun startScanner(): Boolean = try {
        val apps = getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setPackage(GPay)
            .setShortcutIds(listOf("QR_SCANNER"))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val shortcut = apps.getShortcuts(query, Process.myUserHandle())?.firstOrNull()
        if (shortcut != null) {
            apps.startShortcut(shortcut, null, null)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false // not the Home app, or GPay is not installed
    }
}
