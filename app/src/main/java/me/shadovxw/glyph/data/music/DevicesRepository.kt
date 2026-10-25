package me.shadovxw.glyph.data.music

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the sound can go (DESIGN.md 3, screen 2): connected audio outputs, the one music is routed to now, and
 * paired audio devices that are switched off. Listing paired devices needs the Bluetooth permission.
 */
class DevicesRepository(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(DevicesState())
    val state: StateFlow<DevicesState> = _state.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
    }

    fun start() {
        audio.registerAudioDeviceCallback(callback, null)
        refresh()
    }

    fun stop() {
        audio.unregisterAudioDeviceCallback(callback)
    }

    fun hasBluetoothPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun refresh() {
        val allowed = hasBluetoothPermission()
        val bonded: List<BluetoothDevice> = if (allowed) {
            runCatching {
                context.getSystemService(BluetoothManager::class.java).adapter?.bondedDevices.orEmpty().toList()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val bondedByAddress = bonded.associateBy { it.address }

        val outputs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val routed = audio.getAudioDevicesForAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
        ).map { it.id }.toSet()
        val musicActive = audio.isMusicActive

        val connected = outputs.filter { it.type in ExternalTypes }
        val result = mutableListOf<OutputDevice>()

        connected.forEach { info ->
            val bt = bondedByAddress[info.address]
            val isRouted = info.id in routed
            result += OutputDevice(
                name = bt?.displayName() ?: info.productName.toString().ifBlank { "Audio device" },
                kind = kindOf(bt, info),
                status = if (isRouted && musicActive) DeviceStatus.Playing else DeviceStatus.Ready,
                battery = bt?.batteryText().orEmpty(),
            )
        }

        // Paired audio devices that are not connected right now.
        val connectedAddresses = connected.map { it.address }.toSet()
        bonded.filter { it.isAudio() && it.address !in connectedAddresses }.forEach { bt ->
            result += OutputDevice(bt.displayName(), kindOf(bt, null), DeviceStatus.Off, bt.batteryText())
        }

        // The phone itself, so there is always somewhere for the sound to be.
        val phoneRouted = connected.none { it.id in routed }
        result += OutputDevice(
            name = "This phone",
            kind = DeviceKind.Speaker,
            status = if (phoneRouted && musicActive) DeviceStatus.Playing else DeviceStatus.Ready,
            battery = "",
            builtIn = true,
        )

        _state.value = DevicesState(
            devices = result.sortedBy { it.status.ordinal },
            bluetoothAllowed = allowed,
        )
    }

    private fun BluetoothDevice.displayName(): String = runCatching { alias ?: name }.getOrNull() ?: "Bluetooth device"

    private fun BluetoothDevice.isAudio(): Boolean =
        runCatching { bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO }.getOrDefault(false)

    /** Battery is not part of Android's public API; this asks for it where the phone allows it and says nothing otherwise. */
    private fun BluetoothDevice.batteryText(): String = runCatching {
        val level = BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(this) as Int
        if (level in 0..100) "$level%" else ""
    }.getOrDefault("")

    private fun kindOf(bt: BluetoothDevice?, info: AudioDeviceInfo?): DeviceKind {
        val name = (bt?.displayName() ?: info?.productName?.toString()).orEmpty().lowercase()
        val cls = runCatching { bt?.bluetoothClass?.deviceClass }.getOrNull()
        return when {
            listOf("buds", "pods", "earbud", "tws").any { it in name } -> DeviceKind.Earbuds
            cls == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES || listOf("headphone", "wh-", "xm4", "xm5").any { it in name } -> DeviceKind.Headphones
            cls == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceKind.Earbuds
            cls == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER || cls == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO ||
                listOf("speaker", "boom", "jbl", "soundbar").any { it in name } -> DeviceKind.Speaker
            info?.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || info?.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                info?.type == AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceKind.Headphones
            // A Bluetooth audio device we cannot tell apart is most likely headphones.
            info?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || info?.type == AudioDeviceInfo.TYPE_BLE_HEADSET -> DeviceKind.Headphones
            else -> DeviceKind.Other
        }
    }

    private companion object {
        /** Outputs that are not the phone's own speaker or earpiece. */
        val ExternalTypes = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
        )
    }
}
