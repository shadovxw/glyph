package me.shadovxw.glyph.drawer

import androidx.compose.ui.graphics.vector.ImageVector
import me.shadovxw.glyph.theme.GlyphIcons

/**
 * Apps that get a Glyph line icon in the drawer instead of their Play Store icon (DESIGN.md 6).
 * Keep it configurable: add a package here to give it a line icon.
 */
object CustomIcons {
    private val byPackage: Map<String, ImageVector> = buildMap {
        listOf("com.google.android.dialer", "com.android.dialer", "com.oplus.dialer", "com.oneplus.dialer")
            .forEach { put(it, GlyphIcons.Phone) }
        listOf("com.oplus.camera", "com.oneplus.camera", "com.android.camera", "com.google.android.GoogleCamera")
            .forEach { put(it, GlyphIcons.Camera) }
        listOf("com.whatsapp", "com.whatsapp.w4b").forEach { put(it, GlyphIcons.WhatsApp) }
        put("com.instagram.android", GlyphIcons.Instagram)
        put("com.chess", GlyphIcons.Chess)
        put("com.pinterest", GlyphIcons.Pinterest)
    }

    fun forPackage(packageName: String): ImageVector? = byPackage[packageName]
}
