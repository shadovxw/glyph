package me.shadovxw.glyph.tiles

import android.content.Context
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * The hotspot's name and password, typed once into Glyph because Android does not tell apps what they are.
 * Kept in the app's private storage.
 */
class HotspotStore(context: Context) {
    private val prefs = context.getSharedPreferences("glyph_hotspot", Context.MODE_PRIVATE)

    val ssid: String get() = prefs.getString("ssid", "") ?: ""
    val password: String get() = prefs.getString("password", "") ?: ""
    val isSet: Boolean get() = ssid.isNotEmpty()

    fun save(ssid: String, password: String) {
        prefs.edit().putString("ssid", ssid.trim()).putString("password", password).apply()
    }

    /** The text phones read from a Wi-Fi QR code. */
    fun payload(): String {
        fun esc(text: String) = text.replace(Regex("""([\\;,:"])"""), """\\$1""")
        return if (password.isEmpty()) "WIFI:T:nopass;S:${esc(ssid)};;" else "WIFI:T:WPA;S:${esc(ssid)};P:${esc(password)};;"
    }

    fun qr(): BitMatrix = QRCodeWriter().encode(
        payload(),
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
    )
}
