package me.shadovxw.glyph.screens.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.shadovxw.glyph.data.music.DeviceKind
import me.shadovxw.glyph.data.music.DeviceStatus
import me.shadovxw.glyph.data.music.DevicesState
import me.shadovxw.glyph.data.music.MusicActions
import me.shadovxw.glyph.data.music.OutputDevice
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphProgressBar
import me.shadovxw.glyph.theme.GlyphType
import kotlin.math.roundToInt

/**
 * DESIGN.md 3, screen 2: "Playing on". Each audio device is a 40 dp row with its icon, name, detail and status,
 * and a square marker (filled = the sound is going there). Tapping a row opens the Bluetooth settings page. The
 * volume row sits at the bottom.
 */
@Composable
fun DevicesCard(state: DevicesState, actions: MusicActions, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    Column(modifier) {
        BasicText("Playing on", style = GlyphType.label.copy(color = c.inkDim), modifier = Modifier.padding(bottom = 6.dp))
        // Three rows show at a time; any more scroll inside the list.
        val visibleRows = state.devices.size.coerceIn(1, VisibleDeviceRows)
        Column(Modifier.fillMaxWidth().height(DeviceRowHeight * visibleRows).verticalScroll(rememberScrollState())) {
            state.devices.forEach { DeviceRow(it, actions.openBluetoothSettings) }
        }
        if (state.devices.size > VisibleDeviceRows) {
            BasicText(
                "${state.devices.size - VisibleDeviceRows} more · scroll the row",
                style = GlyphType.meta.copy(color = c.inkDim),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!state.bluetoothAllowed) {
            BasicText(
                "Allow Bluetooth to see paired devices",
                style = GlyphType.rowSecondary.copy(color = c.ink),
                modifier = Modifier.clickable(onClick = actions.allowBluetooth).padding(vertical = 10.dp),
            )
        }
        VolumeRow(state, Modifier.padding(top = 6.dp))
    }
}

/** One device shows at a time; the rest scroll inside the list. */
private const val VisibleDeviceRows = 1
private val DeviceRowHeight = 40.dp

@Composable
private fun DeviceRow(device: OutputDevice, onClick: () -> Unit) {
    val c = Glyph.colors
    val word = when (device.status) {
        DeviceStatus.Playing -> "Playing"
        DeviceStatus.Ready -> if (device.builtIn) "Speaker" else "Connected"
        DeviceStatus.Off -> "Not connected"
    }
    val detail = listOf(device.battery, word).filter { it.isNotEmpty() }.joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().height(DeviceRowHeight).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = iconFor(device.kind),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(if (device.status == DeviceStatus.Off) c.inkDim else c.ink),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(device.name, style = GlyphType.rowPrimary.copy(color = if (device.status == DeviceStatus.Off) c.inkDim else c.ink), maxLines = 1)
            BasicText(detail, style = GlyphType.rowSecondary.copy(color = c.inkDim), maxLines = 1)
        }
        // Filled square = playing here, outlined = connected, faint outline = off.
        val marker = Modifier.size(10.dp)
        when (device.status) {
            DeviceStatus.Playing -> Box(marker.background(c.ink))
            DeviceStatus.Ready -> Box(marker.border(1.dp, c.ink))
            DeviceStatus.Off -> Box(marker.border(1.dp, c.inkDim.copy(alpha = 0.4f)))
        }
    }
}

private fun iconFor(kind: DeviceKind): ImageVector = when (kind) {
    DeviceKind.Earbuds -> GlyphIcons.Earbuds
    DeviceKind.Headphones -> GlyphIcons.Headphones
    DeviceKind.Speaker -> GlyphIcons.PhoneSpeaker
    DeviceKind.Other -> GlyphIcons.Volume
}

/** Media volume: speaker icon, a bar you can tap or drag, and the value. Follows the hardware volume keys too. */
@Composable
private fun VolumeRow(devices: DevicesState, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    // Each output remembers its own volume, so the bar is read again whenever the outputs change.
    val (volume, setVolume) = rememberMusicVolume(devices)
    var widthPx by remember { mutableFloatStateOf(1f) }
    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageVector = GlyphIcons.Volume,
            contentDescription = "Volume",
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(c.ink),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) { detectTapGestures { setVolume(it.x / widthPx) } }
                .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); setVolume(change.position.x / widthPx) } },
            contentAlignment = Alignment.Center,
        ) {
            GlyphProgressBar(volume.value)
        }
        Spacer(Modifier.width(12.dp))
        BasicText("${(volume.value * 100).roundToInt()}%", style = GlyphType.meta.copy(color = c.inkDim))
    }
}

/** Media volume as 0..1, kept in step with the hardware keys; the setter changes it. */
@Composable
private fun rememberMusicVolume(devices: DevicesState): Pair<State<Float>, (Float) -> Unit> {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val volume = remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()) }
    fun read() { volume.floatValue = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat() }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = read()
        }
        context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        onDispose { context.unregisterReceiver(receiver) }
    }
    // The outputs changed (headphones connected or removed): the volume that applies is a different one.
    LaunchedEffect(devices) { read() }
    // A light check once a second covers anything that does not send the broadcast (such as a route change).
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            read()
        }
    }
    val set: (Float) -> Unit = { fraction ->
        val level = (fraction.coerceIn(0f, 1f) * max).roundToInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        volume.floatValue = level / max.toFloat()
    }
    return volume to set
}
