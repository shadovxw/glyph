package me.shadovxw.glyph.tiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.shadovxw.glyph.data.SettingsRepository
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphTheme
import me.shadovxw.glyph.theme.GlyphType

/**
 * Opened by the Hotspot QR tile: a full-screen QR that another phone can scan to join the hotspot. The first time
 * (and from "Edit") it asks for the hotspot's name and password.
 */
class HotspotQrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = HotspotStore(applicationContext)
        val mode = runBlocking { SettingsRepository(applicationContext).themeMode.first() }
        setContent {
            GlyphTheme(dark = mode.resolve(isSystemInDarkTheme())) {
                var editing by remember { mutableStateOf(!store.isSet) }
                Box(Modifier.fillMaxSize().background(Glyph.colors.screenBg).systemBarsPadding().imePadding()) {
                    if (editing) {
                        EditHotspot(store, onSaved = { editing = false })
                    } else {
                        ShowHotspot(store, onEdit = { editing = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowHotspot(store: HotspotStore, onEdit: () -> Unit) {
    val c = Glyph.colors
    val matrix = remember(store.ssid, store.password) { store.qr() }
    Column(
        Modifier.fillMaxSize().padding(horizontal = GlyphDimens.screenSide),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText("Hotspot", style = GlyphType.screenTitle.copy(color = c.ink))
        BasicText("Scan to join", style = GlyphType.rowSecondary.copy(color = c.inkDim), modifier = Modifier.padding(top = 2.dp, bottom = 24.dp))
        // Always black on white, whatever the mode, so every phone camera reads it.
        Box(Modifier.fillMaxWidth().background(Color.White).padding(20.dp)) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val cell = size.width / matrix.width
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        if (matrix[x, y]) {
                            drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                        }
                    }
                }
            }
        }
        BasicText(store.ssid, style = GlyphType.monoStat.copy(color = c.ink), modifier = Modifier.padding(top = 20.dp))
        BasicText(
            "Edit",
            style = GlyphType.rowPrimary.copy(color = c.inkDim),
            modifier = Modifier.padding(top = 20.dp).clickable(onClick = onEdit).padding(12.dp),
        )
    }
}

@Composable
private fun EditHotspot(store: HotspotStore, onSaved: () -> Unit) {
    val c = Glyph.colors
    var ssid by remember { mutableStateOf(store.ssid) }
    var password by remember { mutableStateOf(store.password) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = GlyphDimens.screenSide),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText("Hotspot", style = GlyphType.screenTitle.copy(color = c.ink))
        BasicText(
            "Android does not share the hotspot's name and password with apps, so type them here once.",
            style = GlyphType.rowSecondary.copy(color = c.inkDim),
            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp),
        )
        Field("Name", ssid, { ssid = it }, VisualTransformation.None, KeyboardType.Text)
        Field("Password", password, { password = it }, PasswordVisualTransformation(), KeyboardType.Password)
        Box(
            Modifier
                .padding(top = 28.dp)
                .height(40.dp)
                .background(if (ssid.isBlank()) c.inkTrack else c.chipBg)
                .clickable(enabled = ssid.isNotBlank()) {
                    store.save(ssid, password)
                    onSaved()
                }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("Save", style = GlyphType.rowPrimary.copy(color = c.chipText))
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, transform: VisualTransformation, type: KeyboardType) {
    val c = Glyph.colors
    BasicText(label, style = GlyphType.label.copy(color = c.inkDim), modifier = Modifier.padding(top = 14.dp))
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = GlyphType.screenTitle.copy(color = c.ink),
        cursorBrush = SolidColor(c.ink),
        visualTransformation = transform,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
    )
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
}
