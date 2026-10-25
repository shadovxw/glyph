package me.shadovxw.glyph.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphTheme
import me.shadovxw.glyph.theme.GlyphType

/** Health Connect shows this when asked why Glyph wants your steps. Tap anywhere to close. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphTheme(dark = true) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Glyph.colors.screenBg)
                        .clickable { finish() }
                        .padding(GlyphDimens.screenSide),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Glyph reads today's step count from Health Connect to show it on the Today screen. " +
                            "It stays on your phone.",
                        style = GlyphType.rowPrimary.copy(color = Glyph.colors.ink),
                    )
                }
            }
        }
    }
}
