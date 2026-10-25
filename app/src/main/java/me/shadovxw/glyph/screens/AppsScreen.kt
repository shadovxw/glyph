package me.shadovxw.glyph.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.groups.AppGroup
import me.shadovxw.glyph.data.groups.GroupActions
import me.shadovxw.glyph.screens.apps.GroupTile
import me.shadovxw.glyph.screens.apps.TileBox
import me.shadovxw.glyph.screens.apps.TileHeight
import me.shadovxw.glyph.screens.apps.panelHeight
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.bottomFade

private const val Columns = 3
private val GapX = 8.dp
private val GapY = 12.dp

/**
 * Screen 5, Apps (DESIGN.md 3): the user's groups of apps. Tap a group and it grows to the full width, pushing the groups
 * after it down; tap outside to fold it back. Tap an app to start it from its icon. Edit renames a group and removes
 * apps; Add (and the + at the top, for a new group) asks the page above to show the add-apps sheet.
 */
@Composable
fun AppsScreen(
    groups: List<AppGroup>,
    apps: List<AppEntry>,
    actions: GroupActions,
    onLaunch: (AppEntry, Rect) -> Unit,
    onPickApps: (String) -> Unit,
) {
    val c = Glyph.colors
    var openId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }

    val byPackage = remember(apps) { apps.groupBy { it.packageName }.mapValues { it.value.first() } }
    val resolved = remember(groups, byPackage) { groups.map { g -> g to g.packages.mapNotNull { byPackage[it] } } }
    val distinctApps = resolved.flatMap { (_, list) -> list.map { it.packageName } }.toSet().size

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = GlyphDimens.screenSide, end = GlyphDimens.screenSide, top = GlyphDimens.screenTop),
    ) {
        Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    "%02d groups · %d apps".format(groups.size, distinctApps),
                    style = GlyphType.meta.copy(color = c.inkDim),
                )
                BasicText("Groups", style = GlyphType.bigStat.copy(color = c.ink))
            }
            Box(
                Modifier
                    .size(GlyphDimens.minTouch)
                    .background(c.ink)
                    .clickable {
                        val id = actions.newGroup()
                        editing = false
                        openId = id
                        onPickApps(id)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(GlyphIcons.Plus, contentDescription = "New group", Modifier.size(18.dp), colorFilter = ColorFilter.tint(c.screenBg))
            }
        }
        Box(Modifier.height(22.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().bottomFade()) {
            val gridWidth = maxWidth
            val viewportHeight = maxHeight
            val columnWidth = (gridWidth - GapX * (Columns - 1)) / Columns

            val openIndex = resolved.indexOfFirst { it.first.id == openId }
            val boxes = tileBoxes(resolved.map { it.second.size }, openIndex, gridWidth, columnWidth)
            val contentHeight = (boxes.maxOfOrNull { it.y + it.h } ?: 0.dp) + 12.dp + GlyphDimens.homeBottomClearance

            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(maxOf(contentHeight, viewportHeight))) {
                    if (openId != null) {
                        // Tapping anywhere that is not the open group folds it back.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        openId = null
                                        editing = false
                                    }
                                },
                        )
                    }
                    resolved.forEachIndexed { index, (group, list) ->
                        val isOpen = index == openIndex
                        GroupTile(
                            name = group.name,
                            apps = list,
                            box = boxes[index],
                            isOpen = isOpen,
                            dimmed = openIndex >= 0 && !isOpen,
                            editing = isOpen && editing,
                            gridWidth = gridWidth,
                            columnWidth = columnWidth,
                            onOpen = {
                                openId = group.id
                                editing = false
                            },
                            onToggleEdit = { editing = !editing },
                            onRename = { actions.rename(group.id, it) },
                            onRemove = { actions.removeApp(group.id, it.packageName) },
                            onAdd = {
                                editing = false
                                onPickApps(group.id)
                            },
                            onLaunch = onLaunch,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Where every tile goes. Closed: three columns. With a group open, the rows above it stay, it takes the full width at its
 * row, and the groups after it start again below it. Each tile animates to its new box, so they slide.
 */
private fun tileBoxes(counts: List<Int>, openIndex: Int, gridWidth: Dp, columnWidth: Dp): List<TileBox> {
    fun cell(position: Int, top: Dp) = TileBox(
        x = (columnWidth + GapX) * (position % Columns),
        y = top + (TileHeight + GapY) * (position / Columns),
        w = columnWidth,
        h = TileHeight,
    )
    if (openIndex < 0) return counts.indices.map { cell(it, 0.dp) }

    val openRow = openIndex / Columns
    val openTop = (TileHeight + GapY) * openRow
    val openHeight = panelHeight(counts[openIndex])
    var after = 0
    return counts.indices.map { k ->
        when {
            k < openRow * Columns -> cell(k, 0.dp)
            k == openIndex -> TileBox(0.dp, openTop, gridWidth, openHeight)
            else -> cell(after++, openTop + openHeight + GapY)
        }
    }
}
