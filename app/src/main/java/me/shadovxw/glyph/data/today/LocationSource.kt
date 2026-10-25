package me.shadovxw.glyph.data.today

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.shadovxw.glyph.data.SettingsRepository
import kotlin.coroutines.resume

class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Approximate (coarse) location for weather. Uses the fused provider; falls back to the last known fix and then to
 * the last coordinates Glyph saved, so weather still works offline from the same place.
 */
class LocationSource(private val context: Context, private val settings: SettingsRepository) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun current(): Coordinates? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        val fix: Location? = if (provider == null) {
            null
        } else {
            withTimeoutOrNull(FixTimeoutMillis) { requestFix(manager, provider) }
                ?: runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        if (fix != null) {
            settings.saveCoordinates(fix.latitude, fix.longitude)
            return Coordinates(fix.latitude, fix.longitude)
        }
        return settings.savedCoordinates()
    }

    private suspend fun requestFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }

    private companion object {
        const val FixTimeoutMillis = 8_000L
    }
}
