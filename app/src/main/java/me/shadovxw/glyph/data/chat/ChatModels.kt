package me.shadovxw.glyph.data.chat

import androidx.compose.runtime.Immutable

/** One WhatsApp conversation, as far as its notifications tell us. */
@Immutable
class Chat(
    /** Stable per conversation (WhatsApp's notification tag, or the shortcut id). */
    val key: String,
    val name: String,
    /** The latest message; in groups it starts with the sender's name. */
    val preview: String,
    val timeMillis: Long,
    /** Messages not yet read. Zero once the notification is gone (opened, or read elsewhere). */
    val unread: Int,
    val isGroup: Boolean,
)

@Immutable
data class ChatsState(
    val whatsAppInstalled: Boolean = true,
    /** False until "Notification access" is switched on for Glyph. */
    val hasAccess: Boolean = false,
    /** Newest first. */
    val chats: List<Chat> = emptyList(),
) {
    val totalUnread: Int get() = chats.sumOf { it.unread }
    val unreadChats: Int get() = chats.count { it.unread > 0 }
}

/** What the WhatsApp page does. Tapping anywhere on it opens WhatsApp (not a particular chat). */
@Immutable
class ChatActions(
    val allowAccess: () -> Unit,
    val openWhatsApp: () -> Unit,
    val newChat: () -> Unit,
    /** Adds WhatsApp's own chats widget to this page (asks the user once). */
    val addWidget: () -> Unit,
    val removeWidget: () -> Unit,
) {
    companion object {
        val None = ChatActions({}, {}, {}, {}, {})
    }
}
