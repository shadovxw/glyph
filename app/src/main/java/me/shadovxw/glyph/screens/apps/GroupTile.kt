package me.shadovxw.glyph.screens.apps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.drawer.AppIcon
import me.shadovxw.glyph.launcher.GlyphEasing
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType

/** Where a tile sits in the grid, in dp, so it can glide there. */
internal class TileBox(val x: Dp, val y: Dp, val w: Dp, val h: Dp)

internal val TileHeight = 112.dp
private val RowHeight = 78.dp
private const val Columns = 4

/** Height of a group's open panel for [apps] apps (the Add button takes one more cell). */
internal fun panelHeight(apps: Int): Dp = 14.dp + 46.dp + RowHeight * ((apps + 1 + Columns - 1) / Columns) + 12.dp

/**
 * One group on the Apps page (DESIGN.md 3, screen 5). Closed, it is a 3 x 3 preview with the name and count. Open, the
 * same box has grown to the full width and shows the apps, which rise in one after another. The box and the layout
 * around it are animated by the caller giving a new [box].
 */
@Composable
internal fun GroupTile(
    name: String,
    apps: List<AppEntry>,
    box: TileBox,
    isOpen: Boolean,
    dimmed: Boolean,
    editing: Boolean,
    gridWidth: Dp,
    columnWidth: Dp,
    onOpen: () -> Unit,
    onToggleEdit: () -> Unit,
    onRename: (String) -> Unit,
    onRemove: (AppEntry) -> Unit,
    onAdd: () -> Unit,
    onLaunch: (AppEntry, Rect) -> Unit,
) {
    val c = Glyph.colors
    val glide = tween<Dp>(520, easing = GlyphEasing)
    val x by animateDpAsState(box.x, glide, label = "tileX")
    val y by animateDpAsState(box.y, glide, label = "tileY")
    val w by animateDpAsState(box.w, glide, label = "tileW")
    val h by animateDpAsState(box.h, glide, label = "tileH")
    val dim by animateFloatAsState(if (dimmed) 0.35f else 1f, tween(300, easing = GlyphEasing), label = "tileDim")
    val dark = c.screenBg.luminance() < 0.5f
    val panelColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.72f)
    val fill by animateColorAsState(if (isOpen) panelColor else Color.Transparent, tween(300, easing = GlyphEasing), label = "tileFill")
    val count = "${apps.size} ${if (apps.size == 1) "app" else "apps"}"

    Box(
        Modifier
            .offset(x, y)
            .size(w, h)
            .graphicsLayer { alpha = dim }
            .background(fill)
            .clipToBounds(),
    ) {
        Preview(name, count, apps, isOpen, columnWidth, onOpen)

        val panelAlpha by animateFloatAsState(
            if (isOpen) 1f else 0f,
            tween(260, easing = GlyphEasing, delayMillis = if (isOpen) 140 else 0),
            label = "panelAlpha",
        )
        if (isOpen || panelAlpha > 0.01f) {
            Column(
                Modifier
                    .width(gridWidth)
                    .graphicsLayer { alpha = panelAlpha }
                    .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
            ) {
                PanelHeader(name, count, editing, isOpen, onToggleEdit, onRename)
                Column {
                    (apps.indices + apps.size).chunked(Columns).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { index ->
                                Box(Modifier.weight(1f).height(RowHeight)) {
                                    if (index < apps.size) {
                                        AppItem(apps[index], index, isOpen, editing, onLaunch) { onRemove(apps[index]) }
                                    } else {
                                        AddItem(index, isOpen, onAdd)
                                    }
                                }
                            }
                            repeat(Columns - row.size) { Box(Modifier.weight(1f).height(RowHeight)) }
                        }
                    }
                }
            }
        }
    }
}

/** The closed tile: 3 x 3 of the group's icons (a "+N" in the last cell when there are more than nine), name and count. */
@Composable
private fun Preview(name: String, count: String, apps: List<AppEntry>, isOpen: Boolean, width: Dp, onOpen: () -> Unit) {
    val c = Glyph.colors
    val alpha by animateFloatAsState(if (isOpen) 0f else 1f, tween(200, easing = GlyphEasing), label = "previewAlpha")
    val scale by animateFloatAsState(if (isOpen) 0.85f else 1f, tween(300, easing = GlyphEasing), label = "previewScale")
    Column(
        Modifier
            .width(width)
            .height(TileHeight)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = !isOpen, onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (col in 0 until 3) {
                        val i = row * 3 + col
                        when {
                            apps.size > 9 && i == 8 -> Box(Modifier.size(20.dp).background(c.inkTrack, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                BasicText("+${apps.size - 8}", style = GlyphType.meta.copy(color = c.ink))
                            }
                            i < apps.size -> AppIcon(apps[i], size = 20.dp)
                            else -> DashedCircle(20.dp)
                        }
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(name, style = GlyphType.rowPrimary.copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(count, style = GlyphType.meta.copy(color = c.inkDim))
        }
    }
}

@Composable
private fun PanelHeader(
    name: String,
    count: String,
    editing: Boolean,
    isOpen: Boolean,
    onToggleEdit: () -> Unit,
    onRename: (String) -> Unit,
) {
    val c = Glyph.colors
    Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (editing) {
                BasicTextField(
                    value = name,
                    onValueChange = onRename,
                    singleLine = true,
                    textStyle = GlyphType.screenTitle.copy(color = c.ink),
                    cursorBrush = SolidColor(c.ink),
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.ink))
            } else {
                BasicText(name, style = GlyphType.screenTitle.copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(count, style = GlyphType.meta.copy(color = c.inkDim))
            }
        }
        Box(
            Modifier
                .height(30.dp)
                .background(if (editing) c.ink else Color.Transparent)
                .clickable(enabled = isOpen, onClick = onToggleEdit)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(if (editing) "Done" else "Edit", style = GlyphType.label.copy(color = if (editing) c.screenBg else c.ink))
        }
    }
    Box(Modifier.height(10.dp))
}

/** An app in an open group. It rises in 35 ms after the one before it; tapping it starts the app from its icon. */
@Composable
private fun AppItem(
    app: AppEntry,
    index: Int,
    isOpen: Boolean,
    editing: Boolean,
    onLaunch: (AppEntry, Rect) -> Unit,
    onRemove: () -> Unit,
) {
    val c = Glyph.colors
    val delay = if (isOpen) 160 + index * 35 else 0
    val alpha by animateFloatAsState(if (isOpen) 1f else 0f, tween(320, easing = GlyphEasing, delayMillis = delay), label = "appAlpha")
    val lift by animateFloatAsState(if (isOpen) 0f else 10f, tween(420, easing = GlyphEasing, delayMillis = delay), label = "appLift")
    val scale by animateFloatAsState(if (isOpen) 1f else 0.9f, tween(420, easing = GlyphEasing, delayMillis = delay), label = "appScale")
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = lift.dp.toPx()
            scaleX = scale
            scaleY = scale
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = isOpen && !editing) { onLaunch(app, bounds) }
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            AppIcon(app, Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }, size = 48.dp)
            BasicText(
                app.label,
                style = GlyphType.label.copy(color = c.ink, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            )
        }
        if (editing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(24.dp)
                    .background(c.ink)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Image(GlyphIcons.Minus, contentDescription = "Remove ${app.label}", Modifier.size(12.dp), colorFilter = ColorFilter.tint(c.screenBg))
            }
        }
    }
}

/** The dashed "Add" cell after the last app. */
@Composable
private fun AddItem(index: Int, isOpen: Boolean, onAdd: () -> Unit) {
    val c = Glyph.colors
    val delay = if (isOpen) 160 + index * 35 else 0
    val alpha by animateFloatAsState(if (isOpen) 1f else 0f, tween(320, easing = GlyphEasing, delayMillis = delay), label = "addAlpha")
    val lift by animateFloatAsState(if (isOpen) 0f else 10f, tween(420, easing = GlyphEasing, delayMillis = delay), label = "addLift")
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = lift.dp.toPx()
            }
            .clickable(enabled = isOpen, onClick = onAdd)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            DashedCircle(48.dp, dim = true)
            Image(GlyphIcons.Plus, contentDescription = "Add apps", Modifier.size(18.dp), colorFilter = ColorFilter.tint(c.ink))
        }
        BasicText("Add", style = GlyphType.label.copy(color = c.ink))
    }
}

@Composable
private fun DashedCircle(size: Dp, dim: Boolean = false) {
    val c = Glyph.colors
    val color = if (dim) c.inkDim else c.ink.copy(alpha = 0.18f)
    Canvas(Modifier.size(size)) {
        val stroke = 1.dp.toPx()
        drawCircle(
            color = color,
            radius = (this.size.minDimension - stroke) / 2f,
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
        )
    }
}
