package me.shadovxw.glyph.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GeistMono

/** After a scrub: glide to the nearest page, the same way a swipe settles. */
private suspend fun settle(state: PagerState, count: Int) {
    val nearest = (state.currentPage + state.currentPageOffsetFraction).roundToInt().coerceIn(0, count - 1)
    state.animateScrollToPage(nearest, animationSpec = tween(GlyphSettleMillis, easing = GlyphEasing))
}

/** Page-settle easing from DESIGN.md 2.6 and 4. */
val GlyphEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
const val GlyphSettleMillis = 480

private const val LabelHideDelayMillis = 1_100L

/**
 * DESIGN.md 2.6. A thin track with a segment that follows the swipe live; the page number,
 * total and name appear only while swiping and fade out 1.1 s after it ends.
 *
 * The indicator is also a control: press and drag it sideways and the pages move with your finger (the segment is
 * under the finger, so the whole track is all five pages), then settle on release; a tap calls [onTap], which opens
 * the page switcher.
 */
@Composable
fun PageIndicator(state: PagerState, names: List<String>, onTap: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val count = names.size
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var labelShown by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    val swiping = state.isScrollInProgress || scrubbing
    LaunchedEffect(swiping) {
        if (swiping) {
            labelShown = true
        } else {
            delay(LabelHideDelayMillis)
            labelShown = false
        }
    }

    val labelAlpha by animateFloatAsState(if (labelShown) 1f else 0f, tween(260), label = "indicatorLabelAlpha")
    val labelLift by animateDpAsState(if (labelShown) 0.dp else 4.dp, tween(260), label = "indicatorLabelLift")
    val trackAlpha by animateFloatAsState(if (swiping) 1f else 0.55f, tween(260), label = "indicatorTrackAlpha")

    val monoStyle = androidx.compose.ui.text.TextStyle(
        fontFamily = GeistMono,
        letterSpacing = 0.04.em,
        color = c.ink,
    )

    // The touch area is the label row plus the track with 20 dp / 10 dp of room around it, so a thumb can find the 2 dp track.
    Column(
        modifier = modifier
            .pointerInput(onTap) { detectTapGestures { onTap() } }
            .pointerInput(count) {
                val segmentPx = with(density) { GlyphDimens.indicatorTrackWidth.toPx() } / count
                detectHorizontalDragGestures(
                    onDragStart = { scrubbing = true },
                    onDragEnd = {
                        scrubbing = false
                        scope.launch { settle(state, count) }
                    },
                    onDragCancel = {
                        scrubbing = false
                        scope.launch { settle(state, count) }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    // One segment of finger travel is one page of scrolling.
                    val pageSpan = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
                    state.dispatchRawDelta(dragAmount * pageSpan / segmentPx)
                }
            }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The label row always takes its space so the track never jumps.
        Row(
            Modifier
                .height(14.dp)
                .alpha(labelAlpha)
                .graphicsLayer { translationY = labelLift.toPx() },
            verticalAlignment = Alignment.Bottom,
        ) {
            val target = state.targetPage.coerceIn(0, count - 1)
            BasicText(
                text = "%02d".format(target + 1),
                style = monoStyle.copy(fontSize = 9.5.sp),
                modifier = Modifier.alignByBaseline(),
            )
            BasicText(
                text = "/%02d".format(count),
                style = monoStyle.copy(fontSize = 6.5.sp, color = c.ink.copy(alpha = 0.5f)),
                modifier = Modifier.padding(start = 2.dp).alignByBaseline(),
            )
            AnimatedVisibility(
                visible = labelShown,
                enter = expandHorizontally(tween(320)),
                exit = shrinkHorizontally(tween(320)),
                modifier = Modifier.alignByBaseline(),
            ) {
                Row {
                    Spacer(Modifier.width(7.dp))
                    BasicText(
                        text = names[target],
                        style = monoStyle.copy(fontSize = 9.5.sp),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
        Spacer(Modifier.height(GlyphDimens.indicatorLabelGap))
        Box(
            Modifier
                .width(GlyphDimens.indicatorTrackWidth)
                .height(GlyphDimens.indicatorTrackHeight)
                .alpha(trackAlpha)
                .background(c.inkTrack)
        ) {
            Box(
                Modifier
                    .width(GlyphDimens.indicatorTrackWidth / count)
                    .height(GlyphDimens.indicatorTrackHeight)
                    .graphicsLayer {
                        val segment = size.width
                        val position = (state.currentPage + state.currentPageOffsetFraction) * segment
                        translationX = position.coerceIn(0f, segment * (count - 1))
                    }
                    .background(c.ink)
            )
        }
    }
}
