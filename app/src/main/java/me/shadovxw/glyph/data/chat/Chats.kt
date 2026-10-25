package me.shadovxw.glyph.data.chat

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/**
 * The WhatsApp chat list (DESIGN.md 3, screen 3). Android gives apps no access to WhatsApp's chat list, so this is
 * built from WhatsApp's own notifications, which Glyph can read once "Notification access" is on:
 *
 *  - a chat appears (or moves up) when a message notification arrives, with its unread count;
 *  - when that notification goes away (you opened the chat, or read it on another device) the chat stays in the
 *    list as read;
 *  - the list is kept on the phone (the chat name, the last message and its time), up to [MaxChats] chats.
 *
 * So it shows the chats that have notified Glyph, not every chat you have ever had. Muted chats do not notify, so
 * they do not appear. It lives in the process-wide object because the notification service and the screen share it.
 */
object Chats {
    private val WhatsAppPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
    private const val MaxChats = 60
    private const val PrefsName = "glyph_chats"
    private const val ChatsKey = "chats"

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(ChatsState())
    val state: StateFlow<ChatsState> = _state.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        _state.update { it.copy(chats = load()) }
    }

    fun setStatus(installed: Boolean, hasAccess: Boolean) {
        _state.update { it.copy(whatsAppInstalled = installed, hasAccess = hasAccess) }
    }

    // ---- fed by the notification service ----

    /** The service just connected: take what is on screen now, and treat every other known chat as read. */
    fun onConnected(active: Array<StatusBarNotification>) {
        val live = active.mapNotNull(::parse)
        val liveKeys = live.map { it.key }.toSet()
        update { known ->
            val merged = known.associateBy { it.key }.toMutableMap()
            known.filter { it.key !in liveKeys }.forEach { merged[it.key] = it.withUnread(0) }
            live.forEach { merged[it.key] = it }
            merged.values.toList()
        }
    }

    fun onPosted(sbn: StatusBarNotification) {
        val chat = parse(sbn) ?: return
        update { known -> known.filter { it.key != chat.key } + chat }
    }

    /** The notification went away: the chat was opened or read elsewhere, so it is read now. */
    fun onRemoved(sbn: StatusBarNotification) {
        val chat = parse(sbn) ?: return
        update { known -> known.map { if (it.key == chat.key) it.withUnread(0) else it } }
    }

    /** For testing the layout on a device with no WhatsApp notifications; used by the debug build only. */
    fun debugReplace(chats: List<Chat>) = update { chats }

    // ---- parsing ----

    /** Returns null for anything that is not a WhatsApp chat message (calls, "checking for messages", summaries). */
    private fun parse(sbn: StatusBarNotification): Chat? {
        if (sbn.packageName !in WhatsAppPackages) return null
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        if (n.category != Notification.CATEGORY_MESSAGE) return null

        val extras = n.extras
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val name = (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()).orEmpty().trim()
        if (name.isEmpty() || name.equals("WhatsApp", ignoreCase = true)) return null

        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.filterIsInstance<Bundle>().orEmpty()
        val last = messages.lastOrNull()
        val text = (last?.getCharSequence("text") ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val sender = last?.getCharSequence("sender")?.toString()?.takeIf { isGroup && it.isNotBlank() }
        val preview = if (sender != null) "$sender: $text" else text
        val time = last?.getLong("time")?.takeIf { it > 0 } ?: sbn.postTime

        // WhatsApp lists only the latest few messages, so also trust its own count ("5 new messages").
        val fromText = Regex("(\\d+)\\s+new messages?").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val unread = max(1, max(messages.size, max(n.number, fromText)))

        val key = sbn.tag?.takeIf { it.isNotBlank() } ?: n.shortcutId ?: name
        return Chat(key = key, name = name, preview = preview, timeMillis = time, unread = unread, isGroup = isGroup)
    }

    private fun Chat.withUnread(count: Int) = Chat(key, name, preview, timeMillis, count, isGroup)

    // ---- storage ----

    private fun update(transform: (List<Chat>) -> List<Chat>) {
        _state.update { current ->
            val next = transform(current.chats).sortedByDescending { it.timeMillis }.take(MaxChats)
            save(next)
            current.copy(chats = next)
        }
    }

    private fun load(): List<Chat> {
        val raw = prefs?.getString(ChatsKey, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Chat(
                    key = o.getString("key"),
                    name = o.getString("name"),
                    preview = o.optString("preview"),
                    timeMillis = o.getLong("time"),
                    unread = o.optInt("unread"),
                    isGroup = o.optBoolean("group"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(chats: List<Chat>) {
        val array = JSONArray()
        chats.forEach {
            array.put(
                JSONObject()
                    .put("key", it.key).put("name", it.name).put("preview", it.preview)
                    .put("time", it.timeMillis).put("unread", it.unread).put("group", it.isGroup),
            )
        }
        prefs?.edit()?.putString(ChatsKey, array.toString())?.apply()
    }
}
