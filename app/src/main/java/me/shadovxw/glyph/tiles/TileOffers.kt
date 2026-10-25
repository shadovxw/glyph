package me.shadovxw.glyph.tiles

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import me.shadovxw.glyph.R

/**
 * One-tap adding (DESIGN.md 7): the first time Glyph shows, the system asks "Add tile?" for each of the five tiles, one
 * after another. Saying no is fine; the tiles can still be added by hand from the Control Centre's edit screen.
 */
object TileOffers {
    private class Offer(val service: Class<*>, val label: String, val icon: Int)

    private val offers = listOf(
        Offer(HotspotQrTile::class.java, "Hotspot QR", R.drawable.ic_tile_hotspot_qr),
        Offer(GlyphModeTile::class.java, "Glyph mode", R.drawable.ic_tile_mode),
        Offer(FocusTile::class.java, "Focus", R.drawable.ic_tile_focus),
        Offer(HomelabTile::class.java, "Homelab", R.drawable.ic_tile_homelab),
        Offer(GpayScanTile::class.java, "GPay scan", R.drawable.ic_tile_scan),
    )

    private const val Prefs = "glyph_tiles"
    private const val Offered = "offered"

    /** Asks once per install; [onDone] runs after the last dialog (or at once when there is nothing to ask). */
    fun offerOnce(activity: Activity, onDone: () -> Unit) {
        val prefs = activity.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT < 33 || prefs.getBoolean(Offered, false)) {
            onDone()
            return
        }
        prefs.edit().putBoolean(Offered, true).apply()
        val manager = activity.getSystemService(StatusBarManager::class.java)
        fun ask(index: Int) {
            if (index >= offers.size) {
                onDone()
                return
            }
            val offer = offers[index]
            manager.requestAddTileService(
                ComponentName(activity, offer.service),
                offer.label,
                Icon.createWithResource(activity, offer.icon),
                activity.mainExecutor,
            ) { ask(index + 1) }
        }
        ask(0)
    }

    /** Debug builds: forget that the tiles were offered, so the next launch asks again. */
    fun reset(context: Context) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().remove(Offered).apply()
    }
}
