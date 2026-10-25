package me.shadovxw.glyph.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.DrawerCategory
import me.shadovxw.glyph.data.categoryOf
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphType

private const val Columns = 5

private sealed interface CategoryRow {
    data class Header(val category: DrawerCategory) : CategoryRow
    data class Apps(val category: DrawerCategory, val index: Int, val apps: List<AppEntry>) : CategoryRow
}

/** DESIGN.md 6, Categories view: a section per category, each a 5-column grid. Empty sections are skipped. */
@Composable
fun CategoriesView(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit, modifier: Modifier = Modifier) {
    val c = Glyph.colors
    val rows = remember(apps) {
        val grouped = apps.groupBy { categoryOf(it) }
        DrawerCategory.entries.flatMap { category ->
            val inCategory = grouped[category].orEmpty()
            if (inCategory.isEmpty()) {
                emptyList()
            } else {
                listOf<CategoryRow>(CategoryRow.Header(category)) +
                    inCategory.chunked(Columns).mapIndexed { i, chunk -> CategoryRow.Apps(category, i, chunk) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = {
            when (it) {
                is CategoryRow.Header -> "h_${it.category.name}"
                is CategoryRow.Apps -> "r_${it.category.name}_${it.index}"
            }
        }) { row ->
            when (row) {
                is CategoryRow.Header -> BasicText(
                    text = row.category.title,
                    style = GlyphType.screenTitle.copy(color = c.ink),
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                )
                is CategoryRow.Apps -> Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    row.apps.forEach { AppCell(it, onLaunch, Modifier.weight(1f)) }
                    repeat(Columns - row.apps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
