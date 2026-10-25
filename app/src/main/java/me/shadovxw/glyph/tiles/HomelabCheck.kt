package me.shadovxw.glyph.tiles

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadovxw.glyph.BuildConfig
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Whether the homelab server is up. The address is `homelab.url` in `local.properties` (for example
 * http://192.168.1.10:8080); "up" means the host accepts a connection on that port. Used by the Homelab tile and the
 * shortcuts popup.
 */
object HomelabCheck {
    /** The configured address, or null when `homelab.url` is empty. */
    val address: Uri? = BuildConfig.HOMELAB_URL.trim().takeIf { it.isNotEmpty() }
        ?.let { Uri.parse(if ("://" in it) it else "http://$it") }
        ?.takeIf { it.host != null }

    /** True or false once checked; null when no address is configured. */
    suspend fun isUp(): Boolean? {
        val uri = address ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
                Socket().use { it.connect(InetSocketAddress(uri.host, port), 3_000) }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
