package me.shadovxw.glyph.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * DESIGN.md 4, "Glyph blend". `offset` is the page's distance from centre in the pager:
 * `page - (currentPage + currentPageOffsetFraction)`, so it is +1 for the page to the right.
 *
 * The page slide itself is the pager's normal slide; the background parallax is in [OrbitBackground].
 */
val LocalGlyphPager = compositionLocalOf<PagerState?> { null }
val LocalGlyphPage = compositionLocalOf { 0 }

private const val CascadeDp = 70
private const val CascadeStaggerMillis = 55
private const val CascadeSettleMillis = 520
private const val FadePerOffset = 0.12f
private const val BlurDpPerOffset = 2

fun pageOffsetOf(pager: PagerState, page: Int): Float =
    page - (pager.currentPage + pager.currentPageOffsetFraction)

/** Whole-page fade and light blur: `alpha = 1 - 0.12 x |offset|`, `blur = 2 dp x |offset|`. */
fun Modifier.glyphPage(pager: PagerState, page: Int): Modifier = graphicsLayer {
    val o = abs(pageOffsetOf(pager, page)).coerceAtMost(1f)
    alpha = 1f - FadePerOffset * o
    renderEffect = if (o > 0.01f) {
        val radius = BlurDpPerOffset.dp.toPx() * o
        BlurEffect(radius, radius, TileMode.Decal)
    } else {
        null
    }
}

/**
 * Card cascade: each card shifts by `-offset x (index + 1) x 70 dp`, so later cards trail further behind.
 * While the finger is down it follows the swipe directly; on release each card settles on its own
 * clock (55 ms stagger per index, 520 ms, glyph easing).
 *
 * Use it on every card of a screen, with `index` in top-to-bottom order starting at 0.
 */
@Composable
fun Modifier.glyphCard(index: Int): Modifier {
    val pager = LocalGlyphPager.current ?: return this
    val page = LocalGlyphPage.current
    val perCardPx = with(LocalDensity.current) { CascadeDp.dp.toPx() } * (index + 1)

    var settling by remember { mutableStateOf(false) }
    var releaseShift by remember { mutableFloatStateOf(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(pager, page, perCardPx) {
        val scope = this
        var settle: Job? = null
        pager.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    settle?.cancel()
                    settling = false
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    settle?.cancel()
                    releaseShift = -pageOffsetOf(pager, page) * perCardPx
                    progress.snapTo(0f)
                    settling = true
                    settle = scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = CascadeSettleMillis,
                                delayMillis = CascadeStaggerMillis * index,
                                easing = GlyphEasing,
                            ),
                        )
                        settling = false
                    }
                }
            }
        }
    }

    return this.graphicsLayer {
        val live = -pageOffsetOf(pager, page) * perCardPx
        translationX = if (settling) {
            val resting = -(page - pager.targetPage) * perCardPx
            lerp(releaseShift, resting, progress.value)
        } else {
            live
        }
    }
}
