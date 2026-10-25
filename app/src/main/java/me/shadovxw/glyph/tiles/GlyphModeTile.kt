package me.shadovxw.glyph.tiles

import android.content.res.Configuration
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.SettingsRepository
import me.shadovxw.glyph.data.ThemeMode

/** Glyph mode (DESIGN.md 7): lit while Glyph is dark; a tap flips it to the other mode and leaves "Auto". */
class GlyphModeTile : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        scope.launch { render() }
    }

    override fun onClick() {
        scope.launch {
            val settings = SettingsRepository(applicationContext)
            val dark = settings.themeMode.first().resolve(systemDark())
            settings.setThemeMode(if (dark) ThemeMode.Light else ThemeMode.Dark)
            render()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun render() {
        val mode = SettingsRepository(applicationContext).themeMode.first()
        val label = when (mode) {
            ThemeMode.System -> "Auto"
            ThemeMode.Dark -> "Dark"
            ThemeMode.Light -> "Light"
        }
        qsTile.show(if (mode.resolve(systemDark())) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE, label)
    }

    private fun systemDark() =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
