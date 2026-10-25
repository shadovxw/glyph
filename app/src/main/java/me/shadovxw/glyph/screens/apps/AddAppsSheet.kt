package me.shadovxw.glyph.screens.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.groups.AppGroup
import me.shadovxw.glyph.drawer.AppIcon
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType

/** The sheet that slides up from the bottom to tick apps in or out of a group. The caller positions and animates it. */
@Composable
fun AddAppsSheet(
    group: AppGroup,
    apps: List<AppEntry>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Glyph.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.screenBg)
            .pointerInput(Unit) { detectTapGestures { } }, // taps on the sheet do not fall through to the page
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText("Add to ${group.name}", style = GlyphType.screenTitle.copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("${group.packages.size} selected", style = GlyphType.meta.copy(color = c.inkDim))
            }
            Box(
                Modifier.height(32.dp).background(c.ink).clickable(onClick = onDone).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("Done", style = GlyphType.rowPrimary.copy(color = c.screenBg))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            items(apps, key = { it.key }) { app ->
                val on = app.packageName in group.packages
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clickable { onToggle(app.packageName) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppIcon(app, size = 26.dp)
                    BasicText(app.label, style = GlyphType.rowPrimary.copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(18.dp)
                            .then(if (on) Modifier.background(c.ink) else Modifier.border(1.5.dp, c.inkDim)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) {
                            Image(GlyphIcons.Check, contentDescription = "Selected", Modifier.size(10.dp), colorFilter = ColorFilter.tint(c.screenBg))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
            }
            item { Box(Modifier.height(24.dp)) }
        }
    }
}
