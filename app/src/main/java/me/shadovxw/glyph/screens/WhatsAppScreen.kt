package me.shadovxw.glyph.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.chat.Chat
import me.shadovxw.glyph.data.chat.ChatActions
import me.shadovxw.glyph.data.chat.ChatsState
import me.shadovxw.glyph.launcher.glyphCard
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.IconTile
import me.shadovxw.glyph.theme.bottomFade
import me.shadovxw.glyph.widgets.HostedWidget
import me.shadovxw.glyph.widgets.WidgetHostController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class ChatFilter(val label: String) { All("All"), Unread("Unread"), Groups("Groups") }

/**
 * Screen 3 (DESIGN.md 3): the WhatsApp chats. It lists the chats WhatsApp has notified Glyph about (Android gives no
 * app the full chat list), newest first, with unread counts. The list scrolls; the header and filters stay put.
 */
@Composable
fun WhatsAppScreen(state: ChatsState, actions: ChatActions, widgets: WidgetHostController, widgetId: Int?) {
    // With WhatsApp's own widget added, it is the page: it has every chat, drawn by WhatsApp.
    if (widgetId != null) {
        WidgetPage(widgets, widgetId, actions)
        return
    }
    val widgetAvailable = remember { widgets.isAvailable() }
    var filter by remember { mutableStateOf(ChatFilter.All) }
    val shown = remember(state.chats, filter) {
        when (filter) {
            ChatFilter.All -> state.chats
            ChatFilter.Unread -> state.chats.filter { it.unread > 0 }
            ChatFilter.Groups -> state.chats.filter { it.isGroup }
        }
    }

    // Tapping anywhere on the page opens WhatsApp itself (not a particular chat). The filters, the "new chat" button and
    // the "allow access" prompt take their own taps first.
    Column(
        Modifier
            .fillMaxSize()
            .clickable(onClick = actions.openWhatsApp)
            .padding(start = GlyphDimens.screenSide, end = GlyphDimens.screenSide, top = GlyphDimens.screenTop),
    ) {
        Header(state, actions, Modifier.glyphCard(0))
        Spacer(Modifier.height(GlyphDimens.cardGap))
        Filters(filter, { filter = it }, Modifier.glyphCard(1))
        Spacer(Modifier.height(8.dp))
        if (widgetAvailable) {
            // Android hides the full chat list from apps; WhatsApp's own widget has it.
            BasicText(
                "Show all chats: add WhatsApp's own widget",
                style = GlyphType.rowSecondary.copy(color = Glyph.colors.ink),
                modifier = Modifier.clickable(onClick = actions.addWidget).padding(vertical = 8.dp),
            )
        }

        when {
            !state.whatsAppInstalled -> Notice("WhatsApp is not installed", "Install it to see your chats here", null)
            !state.hasAccess -> Notice("Allow notification access", "Needed to see your WhatsApp messages", actions.allowAccess)
            shown.isEmpty() -> Notice(
                if (state.chats.isEmpty()) "No chats yet" else "Nothing here",
                "Chats appear as WhatsApp messages arrive",
                null,
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().glyphCard(2).bottomFade(),
                contentPadding = PaddingValues(bottom = GlyphDimens.homeBottomClearance),
            ) {
                items(shown, key = { it.key }) { chat ->
                    ChatRow(chat)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Glyph.colors.inkLine))
                }
            }
        }
    }
}

/** WhatsApp's own chats widget, filling the page between the top margin and the indicator and dock. */
@Composable
private fun WidgetPage(widgets: WidgetHostController, widgetId: Int, actions: ChatActions) {
    val c = Glyph.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = GlyphDimens.screenSide, end = GlyphDimens.screenSide, top = GlyphDimens.screenTop),
    ) {
        Row(Modifier.fillMaxWidth().glyphCard(0), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText("WhatsApp", style = GlyphType.screenTitle.copy(color = c.ink))
            BasicText(
                "Remove widget",
                style = GlyphType.meta.copy(color = c.inkDim),
                modifier = Modifier.clickable(onClick = actions.removeWidget).padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(GlyphDimens.cardGap))
        HostedWidget(
            controller = widgets,
            id = widgetId,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glyphCard(1)
                .padding(bottom = GlyphDimens.homeBottomClearance),
        )
    }
}

@Composable
private fun Header(state: ChatsState, actions: ChatActions, modifier: Modifier) {
    val c = Glyph.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText("WhatsApp", style = GlyphType.screenTitle.copy(color = c.ink))
            if (state.hasAccess) {
                BasicText(
                    "${state.totalUnread} unread · ${state.unreadChats} chats",
                    style = GlyphType.meta.copy(color = c.inkDim),
                )
            }
        }
        IconTile(GlyphIcons.Camera, "Open WhatsApp", actions.openWhatsApp)
        // New chat: a 44 dp filled square.
        Box(
            Modifier.size(GlyphDimens.minTouch).background(c.chipBg).clickable(onClick = actions.newChat),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = GlyphIcons.NewChat,
                contentDescription = "New chat",
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(c.chipText),
            )
        }
    }
}

@Composable
private fun Filters(selected: ChatFilter, onSelect: (ChatFilter) -> Unit, modifier: Modifier) {
    val c = Glyph.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChatFilter.entries.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .height(32.dp)
                    .background(if (on) c.chipBg else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(option.label, style = GlyphType.rowPrimary.copy(color = if (on) c.chipText else c.ink))
            }
        }
    }
}

/** One chat: 26 dp square avatar with the initial, name and last message, time, and the unread badge. 40 dp tall. */
@Composable
private fun ChatRow(chat: Chat) {
    val c = Glyph.colors
    Row(
        Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).background(c.chipBg), contentAlignment = Alignment.Center) {
            val initial = chat.name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
            BasicText(initial, style = GlyphType.rowPrimary.copy(color = c.chipText))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                chat.name,
                style = GlyphType.rowPrimary.copy(color = c.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                chat.preview,
                style = GlyphType.rowSecondary.copy(color = c.inkDim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            BasicText(timeLabel(chat.timeMillis), style = GlyphType.meta.copy(color = c.inkDim))
            // Hidden when there is nothing unread.
            if (chat.unread > 0) {
                Box(
                    Modifier.height(18.dp).widthIn(min = 18.dp).background(c.chipBg).padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(chat.unread.toString(), style = GlyphType.meta.copy(color = c.chipText))
                }
            }
        }
    }
}

@Composable
private fun Notice(title: String, detail: String, onClick: (() -> Unit)?) {
    val c = Glyph.colors
    Column(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp),
    ) {
        BasicText(title, style = GlyphType.screenTitle.copy(color = c.ink))
        BasicText(detail, style = GlyphType.rowSecondary.copy(color = c.inkDim))
    }
}

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/** Today as a clock time, then "Yest", then the weekday for the past week, then the date. */
private fun timeLabel(millis: Long): String {
    val moment = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val day = moment.toLocalDate()
    val today = LocalDate.now()
    return when {
        day == today -> moment.format(clockFormat)
        day == today.minusDays(1) -> "Yest"
        day.isAfter(today.minusDays(7)) -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        else -> moment.format(dateFormat)
    }
}
