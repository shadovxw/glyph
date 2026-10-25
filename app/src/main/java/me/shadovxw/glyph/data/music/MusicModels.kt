package me.shadovxw.glyph.data.music

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class MusicAccess { NeedsNotificationAccess, Ready }

/** The song that is playing (or paused) in the music app. */
@Immutable
class MusicSession(
    val appName: String,
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    /** Position when the state was last reported; use [positionAt] for the live value. */
    val positionMs: Long,
    val durationMs: Long,
    /** SystemClock.elapsedRealtime() at the moment [positionMs] was reported. */
    val reportedAt: Long,
    val speed: Float,
    val canLike: Boolean,
    val canDislike: Boolean,
    val liked: Boolean,
    val disliked: Boolean,
    val canShuffle: Boolean,
    val shuffleOn: Boolean,
    val canRepeat: Boolean,
    val repeatOn: Boolean,
) {
    fun positionAt(elapsedRealtime: Long): Long {
        if (!isPlaying) return positionMs.coerceIn(0, durationOrMax())
        val advanced = ((elapsedRealtime - reportedAt) * speed).toLong()
        return (positionMs + advanced).coerceIn(0, durationOrMax())
    }

    private fun durationOrMax() = if (durationMs > 0) durationMs else Long.MAX_VALUE
}

@Immutable
class MusicState(
    val access: MusicAccess = MusicAccess.NeedsNotificationAccess,
    /** Null when access is granted but nothing is playing. */
    val session: MusicSession? = null,
)

enum class DeviceKind { Earbuds, Headphones, Speaker, Other }

/** Playing = audio is going there now; Ready = connected; Off = paired but not connected. */
enum class DeviceStatus { Playing, Ready, Off }

@Immutable
class OutputDevice(
    val name: String,
    val kind: DeviceKind,
    val status: DeviceStatus,
    /** Battery, for example "80%", when the phone can read it; otherwise empty. */
    val battery: String,
    /** True for the phone's own speaker. */
    val builtIn: Boolean = false,
)

@Immutable
class DevicesState(
    val devices: List<OutputDevice> = emptyList(),
    /** False until the user allows the Bluetooth permission needed to list paired devices. */
    val bluetoothAllowed: Boolean = true,
)

/** What the Music page's buttons do. */
@Immutable
class MusicActions(
    val allowNotificationAccess: () -> Unit,
    val allowBluetooth: () -> Unit,
    val openMusicApp: () -> Unit,
    val openBluetoothSettings: () -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val like: () -> Unit,
    val dislike: () -> Unit,
    val shuffle: () -> Unit,
    val repeat: () -> Unit,
) {
    companion object {
        val None = MusicActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
