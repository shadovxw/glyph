package me.shadovxw.glyph.drawer

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.shadovxw.glyph.launcher.GlyphEasing

/** How far the drawer is open: 0 = closed, 1 = fully open. Dragging and settling both go through here. */
@Stable
class DrawerState(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set
    private var job: Job? = null

    val isOpen: Boolean get() = progress > 0f

    /** Which way the last drag was going, so a slow release snaps the way the finger was heading. */
    private var lastDragOpened = true

    fun dragBy(delta: Float) {
        job?.cancel()
        if (delta != 0f) lastDragOpened = delta > 0f
        progress = (progress + delta).coerceIn(0f, 1f)
    }

    /**
     * [velocity] is in drawers per second; positive opens. Without a fling, it snaps open once it has been
     * pulled 30% of the way up, and snaps closed once it has been pushed 30% of the way down.
     */
    fun settle(velocity: Float) {
        val target = when {
            velocity > FlingVelocity -> 1f
            velocity < -FlingVelocity -> 0f
            lastDragOpened -> if (progress > OpenThreshold) 1f else 0f
            else -> if (progress < 1f - OpenThreshold) 0f else 1f
        }
        animateTo(target)
    }

    fun open() = animateTo(1f)

    fun close() = animateTo(0f)

    /** Closes without animation, for when an app is launched from the drawer. */
    fun snapClosed() {
        job?.cancel()
        progress = 0f
    }

    private fun animateTo(target: Float) {
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = tween(durationMillis = 320, easing = GlyphEasing),
            ) { value, _ -> progress = value }
        }
    }

    private companion object {
        const val FlingVelocity = 1.2f
        const val OpenThreshold = 0.3f
    }
}

@Composable
fun rememberDrawerState(): DrawerState {
    val scope = rememberCoroutineScope()
    return remember { DrawerState(scope) }
}
