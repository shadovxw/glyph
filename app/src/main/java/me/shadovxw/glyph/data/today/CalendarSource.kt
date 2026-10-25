package me.shadovxw.glyph.data.today

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** The next event on the phone's calendars within the coming week. */
class CalendarSource(private val context: Context) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Skips all-day events and ones you declined. An event already under way counts until it ends. */
    suspend fun nextEvent(): EventData? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .also { ContentUris.appendId(it, now); ContentUris.appendId(it, now + WindowMillis) }
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val allDay = cursor.getInt(3) == 1
                val declined = cursor.getInt(4) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                val end = cursor.getLong(2)
                if (allDay || declined || end <= now) continue
                val zone = ZoneId.systemDefault()
                return@withContext EventData(
                    title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Untitled",
                    begin = Instant.ofEpochMilli(cursor.getLong(1)).atZone(zone).toLocalDateTime(),
                    end = Instant.ofEpochMilli(end).atZone(zone).toLocalDateTime(),
                )
            }
        }
        null
    }

    private companion object {
        const val WindowMillis = 7L * 24 * 60 * 60 * 1000
    }
}
