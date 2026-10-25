package me.shadovxw.glyph.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.IconTile

/**
 * DESIGN.md 6: the stock-style drawer. Rounded pill controls (the sharp-corner rule does not apply here),
 * All / Categories switch on top, live search at the bottom. Pulling down while the list is at the top closes it.
 */
@Composable
fun AppDrawer(
    state: DrawerState,
    heightPx: Float,
    apps: List<AppEntry>,
    suggested: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glyph.colors
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val connection = remember(state, heightPx) { drawerConnection(state, heightPx) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.screenBg)
            .nestedScroll(connection)
            .statusBarsPadding(),
    ) {
        TopBar(tab = tab, onTab = { tab = it })
        Box(Modifier.weight(1f)) {
            when {
                query.isNotBlank() -> SearchResults(apps, query, onLaunch)
                tab == 0 -> AllAppsView(apps, suggested, onLaunch)
                else -> CategoriesView(apps, onLaunch)
            }
        }
        SearchPill(
            query = query,
            onQuery = { query = it },
            onSubmit = {
                apps.firstOrNull { it.label.contains(query, ignoreCase = true) }?.let(onLaunch)
            },
        )
    }
}

@Composable
private fun TopBar(tab: Int, onTab: (Int) -> Unit) {
    val c = Glyph.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(44.dp))
        Row(
            modifier = Modifier.clip(CircleShape).background(c.ink.copy(alpha = 0.08f)).padding(3.dp),
        ) {
            listOf("All", "Categories").forEachIndexed { index, title ->
                val selected = index == tab
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) c.ink.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onTab(index) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(title, style = GlyphType.rowPrimary.copy(color = c.ink))
                }
            }
        }
        // TODO: overflow menu (hide apps, sort, settings).
        IconTile(GlyphIcons.MoreVert, "More options", onClick = {}, iconSize = 20.dp)
    }
}

@Composable
private fun SearchResults(apps: List<AppEntry>, query: String, onLaunch: (AppEntry) -> Unit) {
    val results = remember(apps, query) { apps.filter { it.label.contains(query, ignoreCase = true) } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(results, key = { it.key }) { AppCell(it, onLaunch) }
    }
}

@Composable
private fun SearchPill(query: String, onQuery: (String) -> Unit, onSubmit: () -> Unit) {
    val c = Glyph.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(c.ink.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = GlyphIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(c.inkDim),
        )
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = GlyphType.rowPrimary.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) BasicText("Search apps", style = GlyphType.rowPrimary.copy(color = c.inkDim))
                    inner()
                }
            },
        )
    }
}

/** Lets a drag on the list open or close the drawer: up opens it, down at the top of the list closes it. */
private fun drawerConnection(state: DrawerState, heightPx: Float) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < 0f && state.progress < 1f) {
            state.dragBy(-available.y / heightPx)
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0f && source == NestedScrollSource.UserInput) {
            state.dragBy(-available.y / heightPx)
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.progress > 0f && state.progress < 1f) {
            state.settle(-available.y / heightPx)
            return available
        }
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // A quick flick down while already at the top closes it.
        if (available.y > CloseFlingVelocity) {
            state.close()
            return available
        }
        return Velocity.Zero
    }
}

private const val CloseFlingVelocity = 1_500f
