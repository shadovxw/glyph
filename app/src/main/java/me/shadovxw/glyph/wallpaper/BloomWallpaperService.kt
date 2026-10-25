package me.shadovxw.glyph.wallpaper

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.SettingsRepository
import me.shadovxw.glyph.data.ThemeMode

/**
 * DESIGN.md 5B: the Bloom and wind live wallpaper. It is the system wallpaper, so it shows on the lock
 * screen and behind the recents cards; the opaque launcher covers it on the home screen.
 *
 * Capped at 30 fps and stopped whenever it is not visible. Light/dark follows Glyph's mode setting.
 */
class BloomWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BloomEngine()

    private inner class BloomEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val renderer = BloomRenderer(applicationContext)
        private val choreographer = Choreographer.getInstance()
        private var themeMode = ThemeMode.System
        private var dark = systemIsDark()
        private var visible = false
        private var motion = true
        private var lastFrameNanos = 0L

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!visible) return
                // Cap at 30 fps: skip display frames that come too soon.
                if (frameTimeNanos - lastFrameNanos >= FrameIntervalNanos - Slack) {
                    lastFrameNanos = frameTimeNanos
                    drawFrame()
                }
                choreographer.postFrameCallback(this)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            renderer.setDark(dark)
            val settings = SettingsRepository(applicationContext)
            // Light or dark: Glyph's own setting, which by default follows the phone. The phone switching
            // (a configuration change) is picked up here too.
            this@BloomWallpaperService.registerReceiver(themeReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
            scope.launch {
                settings.themeMode.collect { mode ->
                    themeMode = mode
                    applyTheme()
                }
            }
            // The same start/stop switch as the home screen's Orbit background.
            scope.launch {
                settings.motion.collect { on ->
                    motion = on
                    syncLoop()
                }
            }
        }

        private val themeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = applyTheme()
        }

        private fun systemIsDark(): Boolean =
            (applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        private fun applyTheme() {
            val isDark = themeMode.resolve(systemIsDark())
            if (isDark == dark) return
            dark = isDark
            renderer.setDark(isDark)
            notifyColorsChanged()
            if (!visible) drawFrame()
        }

        /** Runs the 30 fps loop only while the wallpaper is visible and the animation is on; otherwise one still frame. */
        private fun syncLoop() {
            choreographer.removeFrameCallback(frameCallback)
            if (visible && motion) {
                lastFrameNanos = 0L
                choreographer.postFrameCallback(frameCallback)
            } else if (visible) {
                drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer.resize(width, height)
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            syncLoop()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            choreographer.removeFrameCallback(frameCallback)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            runCatching { this@BloomWallpaperService.unregisterReceiver(themeReceiver) }
            visible = false
            choreographer.removeFrameCallback(frameCallback)
            scope.cancel()
            super.onDestroy()
        }

        /** Neutral greys only, so system accents follow the monochrome theme. The red is not reported. */
        override fun onComputeColors(): WallpaperColors {
            val primary = if (dark) Color.WHITE else Color.BLACK
            return WallpaperColors(Color.valueOf(primary), Color.valueOf(0xFF7A7A7A.toInt()), null)
        }

        private fun drawFrame() {
            val canvas = try {
                surfaceHolder.lockHardwareCanvas()
            } catch (_: Exception) {
                null
            } ?: return
            try {
                renderer.draw(canvas, SystemClock.uptimeMillis())
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private companion object {
        const val FrameIntervalNanos = 1_000_000_000L / 30
        const val Slack = 2_000_000L
    }
}
