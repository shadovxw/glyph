package me.shadovxw.glyph.shortcuts

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The phone's torch (`CameraManager.setTorchMode`, no permission needed), with its on/off state kept in step with the system. */
class Flashlight(context: Context) {
    private val manager = context.getSystemService(CameraManager::class.java)

    private val cameraId: String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            val c = manager.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) _on.value = enabled
        }
    }

    /** Follow the torch while Glyph is showing (another app or the system tile may switch it too). */
    fun start() = manager.registerTorchCallback(callback, null)

    fun stop() = manager.unregisterTorchCallback(callback)

    fun toggle() {
        val id = cameraId ?: return
        try {
            manager.setTorchMode(id, !_on.value)
        } catch (_: Exception) {
            // The camera is busy (in use by another app); the state stays as it was.
        }
    }
}
