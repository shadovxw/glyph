package me.shadovxw.glyph.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import me.shadovxw.glyph.data.chat.Chats
import me.shadovxw.glyph.data.orders.Orders

/**
 * Switched on with "Notification access". Android only shows other apps' media sessions (what is playing) and their
 * notifications to an app that has an enabled listener. Glyph uses it for the Music page, for the WhatsApp page (it
 * reads WhatsApp's chat notifications) and for live Zomato and Blinkit orders (kept ready, but no page shows them since the World page was removed). Nothing else is read.
 */
class GlyphNotificationListener : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        Chats.init(applicationContext)
    }

    override fun onListenerConnected() {
        val active = activeNotifications ?: emptyArray()
        Chats.onConnected(active)
        Orders.onConnected(active)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Chats.onPosted(sbn)
        Orders.onPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Chats.onRemoved(sbn)
        Orders.onRemoved(sbn)
    }
}
