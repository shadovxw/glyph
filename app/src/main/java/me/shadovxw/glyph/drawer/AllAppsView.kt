package me.shadovxw.glyph.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphType

private const val Columns = 5

/**
 * DESIGN.md 6, All view: suggested apps (2 rows of 5), a divider, then every app A-Z in a
 * 5-column grid, with a letter index down the right edge.
 */
@Composable
fun AllAppsView(
    apps: List<AppEntry>,
    suggested: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val c = Glyph.colors
    val scope = rememberCoroutineScope()
    val present = remember(apps) { apps.map { it.letter }.toSet() }
    val headerCount = if (suggested.isEmpty()) 0 else suggested.size + 1 // suggestions + divider

    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(Columns),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 28.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (suggested.isNotEmpty()) {
                items(suggested, key = { "suggested_" + it.key }) { AppCell(it, onLaunch) }
                item(key = "divider", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
                }
            }
            items(apps, key = { it.key }) { AppCell(it, onLaunch) }
        }
        LetterIndex(
            present = present,
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetter = { letter ->
                val index = apps.indexOfFirst { it.letter == letter }
                if (index >= 0) scope.launch { state.scrollToItem(headerCount + index) }
            },
        )
    }
}

private val IndexLetters: List<Char> = ('A'..'Z').toList() + '#'

/** A-Z and # down the right edge; tap or drag to jump. */
@Composable
private fun LetterIndex(present: Set<Char>, onLetter: (Char) -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    var heightPx by remember { mutableFloatStateOf(1f) }
    fun letterAt(y: Float): Char =
        IndexLetters[(y / heightPx * IndexLetters.size).toInt().coerceIn(0, IndexLetters.size - 1)]

    Column(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight(0.72f)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { onLetter(letterAt(it.y)) } }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onLetter(letterAt(change.position.y))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        IndexLetters.forEach { letter ->
            BasicText(
                text = letter.toString(),
                style = GlyphType.meta.copy(color = if (letter in present) c.ink else c.inkDim.copy(alpha = 0.35f)),
            )
        }
    }
}
