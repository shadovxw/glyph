package me.shadovxw.glyph.data.today

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Screen time from Android's usage events (needs the special "Usage access" setting, which the user turns on).
 * It adds up the time apps spent in the foreground, leaving out launchers and System UI, and compares today
 * so far with the same stretch of the previous seven days.
 */
class ScreenTimeSource(private val context: Context) {
    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun read(): ScreenTimeData = withContext(Dispatchers.Default) {
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val ignored = ignoredPackages()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val midnight = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val elapsed = now - midnight

        val today = foregroundMillis(manager, midnight, now, ignored)
        // Previous days, each over the same time of day, so a morning is not compared with a whole day.
        val earlier = (1..HistoryDays).map { daysAgo ->
            val start = LocalDate.now().minusDays(daysAgo.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            foregroundMillis(manager, start, start + elapsed, ignored)
        }.filter { it > 0 }
        val delta = if (earlier.isEmpty()) {
            null
        } else {
            val average = earlier.average()
            ((today - average) / average * 100).roundToInt()
        }
        ScreenTimeData(today, delta)
    }

    private fun foregroundMillis(manager: UsageStatsManager, from: Long, to: Long, ignored: Set<String>): Long {
        val events = manager.queryEvents(from, to)
        val event = UsageEvents.Event()
        val resumedAt = HashMap<String, Long>()
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg in ignored) continue
            val key = pkg + "/" + event.className
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[key] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = resumedAt.remove(key)
                    if (start != null) total += event.timeStamp - start
                }
            }
        }
        // Anything still in the foreground at the end of the window.
        resumedAt.values.forEach { total += to - it }
        return total
    }

    private fun ignoredPackages(): Set<String> {
        val homes = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_ALL,
        ).map { it.activityInfo.packageName }
        return (homes + context.packageName + "com.android.systemui").toSet()
    }

    private companion object {
        const val HistoryDays = 7
    }
}
