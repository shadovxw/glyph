package me.shadovxw.glyph.shortcuts

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.shadovxw.glyph.launcher.GlyphEasing

/**
 * The shortcuts popup that a short pull down reveals. While the finger is down it just [follow]s the pull; on release it
 * either latches open ([open]) or slides away ([close]). [fade] dims it near the end of a long pull, which hands over
 * to the system notification shade.
 */
@Stable
class ShortcutsPopupState(private val scope: CoroutineScope) {
    /** 0 = hidden, 1 = fully shown. */
    var progress by mutableFloatStateOf(0f)
        private set
    var fade by mutableFloatStateOf(1f)
        private set

    /** True once it has latched open: it then takes touches and stays until closed. */
    var isOpen by mutableStateOf(false)
        private set

    val isVisible: Boolean get() = progress > 0f

    private var job: Job? = null

    fun follow(progress: Float, fade: Float) {
        job?.cancel()
        this.progress = progress
        this.fade = fade
    }

    fun open() {
        isOpen = true
        fade = 1f
        animateTo(1f)
    }

    fun close() {
        isOpen = false
        animateTo(0f)
    }

    private fun animateTo(target: Float) {
        job?.cancel()
        job = scope.launch {
            animate(progress, target, animationSpec = tween(260, easing = GlyphEasing)) { value, _ -> progress = value }
            if (target == 0f) fade = 1f
        }
    }
}

@Composable
fun rememberShortcutsPopupState(): ShortcutsPopupState {
    val scope = rememberCoroutineScope()
    return remember { ShortcutsPopupState(scope) }
}
