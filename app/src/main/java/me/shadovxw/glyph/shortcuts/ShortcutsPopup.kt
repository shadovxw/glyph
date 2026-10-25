package me.shadovxw.glyph.shortcuts

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.tiles.FocusTimer
import me.shadovxw.glyph.tiles.HomelabCheck

/** What the popup's buttons do that only the Activity can (start screens, toggle the torch). */
class ShortcutActions(
    val showHotspotQr: () -> Unit,
    val openHotspotSettings: () -> Unit,
    val toggleTorch: () -> Unit,
    val scanGpay: () -> Unit,
    val openHomelab: () -> Unit,
) {
    companion object {
        val None = ShortcutActions({}, {}, {}, {}, {})
    }
}

private class Shortcut(
    val label: String,
    val icon: ImageVector,
    val lit: Boolean,
    val subtitle: String?,
    /** Buttons that open another screen close the popup; switches (torch, mode, focus) leave it up. */
    val closesPopup: Boolean,
    val onClick: () -> Unit,
)

/**
 * Seven shortcuts that a short pull down on home reveals (DESIGN.md 7). The same actions as the Quick Settings tiles,
 * drawn the Glyph way. Once latched open, a tap outside or a drag up closes it, and a long drag down goes on to the shade.
 */
@Composable
fun ShortcutsPopup(
    state: ShortcutsPopupState,
    dark: Boolean,
    onToggleMode: () -> Unit,
    torchOn: Boolean,
    actions: ShortcutActions,
    onOpenShade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glyph.colors
    val context = LocalContext.current

    // Things worth re-reading each time the popup comes into view.
    var focusLeft by remember { mutableLongStateOf(0L) }
    var homelabUp by remember { mutableStateOf<Boolean?>(null) }
    var hotspotOn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state.isVisible) {
        if (!state.isVisible) return@LaunchedEffect
        hotspotOn = hotspotEnabled(context)
        launch { homelabUp = HomelabCheck.isUp() }
        while (true) {
            focusLeft = FocusTimer.remainingMillis(context)
            delay(5_000)
        }
    }

    val shortcuts = listOf(
        Shortcut("Hotspot QR", GlyphIcons.Qr, false, null, true, actions.showHotspotQr),
        Shortcut("Hotspot", GlyphIcons.Hotspot, hotspotOn == true, if (hotspotOn == true) "On" else null, true, actions.openHotspotSettings),
        Shortcut("Flashlight", GlyphIcons.Flashlight, torchOn, if (torchOn) "On" else null, false, actions.toggleTorch),
        Shortcut("Glyph mode", GlyphIcons.AutoTheme, dark, if (dark) "Dark" else "Light", false, onToggleMode),
        Shortcut(
            "Focus",
            GlyphIcons.Timer,
            focusLeft > 0L,
            if (focusLeft > 0L) "${(focusLeft + 59_999L) / 60_000L} min left" else "${FocusTimer.Minutes} min",
            false,
        ) {
            if (FocusTimer.remainingMillis(context) > 0L) FocusTimer.cancel(context) else FocusTimer.start(context)
            focusLeft = FocusTimer.remainingMillis(context)
        },
        Shortcut(
            "Homelab",
            GlyphIcons.Server,
            homelabUp == true,
            when {
                HomelabCheck.address == null -> "Not set up"
                homelabUp == null -> "Checking"
                homelabUp == true -> "Up"
                else -> "Down"
            },
            true,
            actions.openHomelab,
        ),
        Shortcut("GPay scan", GlyphIcons.Scan, false, null, true, actions.scanGpay),
    )

    var panelHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(modifier.fillMaxSize()) {
        if (state.isOpen) {
            // Latched open: everything outside the panel is a scrim. Tap or drag up closes; a long drag down goes on to the shade.
            var dragged by remember { mutableFloatStateOf(0f) }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { state.close() } }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { dragged += it },
                        onDragStarted = { dragged = 0f },
                        onDragStopped = {
                            with(density) {
                                when {
                                    dragged < -40.dp.toPx() -> state.close()
                                    dragged > 120.dp.toPx() -> {
                                        state.close()
                                        onOpenShade()
                                    }
                                }
                            }
                        },
                    ),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { panelHeightPx = it.height.toFloat() }
                .graphicsLayer {
                    translationY = -(1f - state.progress) * panelHeightPx
                    alpha = if (panelHeightPx == 0f) 0f else state.fade
                }
                .background(c.screenBg)
                .statusBarsPadding()
                .pointerInput(Unit) { detectTapGestures { } } // taps on the panel's gaps do not fall through to the scrim
                .padding(start = GlyphDimens.screenSide, end = GlyphDimens.screenSide, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText("Shortcuts", style = GlyphType.label.copy(color = c.inkDim))
            shortcuts.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { item ->
                        Cell(item, Modifier.weight(1f)) {
                            item.onClick()
                            if (item.closesPopup) state.close()
                        }
                    }
                    repeat(4 - rowItems.size) { Box(Modifier.weight(1f)) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
        }
    }
}

@Composable
private fun Cell(item: Shortcut, modifier: Modifier, onClick: () -> Unit) {
    val c = Glyph.colors
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(56.dp).background(if (item.lit) c.chipBg else c.inkTrack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(if (item.lit) c.chipText else c.ink),
            )
        }
        BasicText(item.label, style = GlyphType.rowSecondary.copy(color = c.ink), modifier = Modifier.padding(top = 6.dp))
        BasicText(item.subtitle ?: " ", style = GlyphType.meta.copy(color = c.inkDim))
    }
}

/** Whether the hotspot is on. Android hides this from apps, so it is read the way the system does and may come back unknown. */
private fun hotspotEnabled(context: Context): Boolean? = try {
    val wifi = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
    wifi.javaClass.getMethod("isWifiApEnabled").invoke(wifi) as? Boolean
} catch (_: Exception) {
    null
}
