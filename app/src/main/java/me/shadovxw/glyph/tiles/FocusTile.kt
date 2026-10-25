package me.shadovxw.glyph.tiles

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.shadovxw.glyph.R

/** The Pomodoro behind the Focus tile: one end time, kept on the phone, and an alarm that says it is over. */
object FocusTimer {
    const val Minutes = 25
    private const val Prefs = "glyph_focus"
    private const val EndsAt = "ends_at"
    private const val Channel = "focus"
    private const val NotificationId = 3001

    /** Milliseconds left, or 0 when no timer runs (a finished one is cleared here). */
    fun remainingMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        val ends = prefs.getLong(EndsAt, 0L)
        val left = ends - System.currentTimeMillis()
        if (ends != 0L && left <= 0L) prefs.edit().remove(EndsAt).apply()
        return left.coerceAtLeast(0L)
    }

    fun start(context: Context) {
        val ends = System.currentTimeMillis() + Minutes * 60_000L
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putLong(EndsAt, ends).apply()
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ends, alarm(context))
    }

    fun cancel(context: Context) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().remove(EndsAt).apply()
        context.getSystemService(AlarmManager::class.java).cancel(alarm(context))
    }

    private fun alarm(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, FocusReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun announceDone(context: Context) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().remove(EndsAt).apply()
        // Without the notification permission the tile simply goes back to idle.
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(Channel, "Focus", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = android.app.Notification.Builder(context, Channel)
            .setSmallIcon(R.drawable.ic_tile_focus)
            .setContentTitle("Focus done")
            .setContentText("$Minutes minutes. Take a break.")
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationId, notification)
    }
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = FocusTimer.announceDone(context)
}

/** Focus (DESIGN.md 7): a tap starts a 25 minute timer, a second tap stops it. */
class FocusTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        if (FocusTimer.remainingMillis(this) > 0L) FocusTimer.cancel(this) else FocusTimer.start(this)
        render()
    }

    private fun render() {
        val left = FocusTimer.remainingMillis(this)
        if (left > 0L) {
            val minutes = ((left + 59_999L) / 60_000L).toInt()
            qsTile.show(Tile.STATE_ACTIVE, "$minutes min left")
        } else {
            qsTile.show(Tile.STATE_INACTIVE, "${FocusTimer.Minutes} min")
        }
    }
}
