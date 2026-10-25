package me.shadovxw.glyph.data.today

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Today's steps from Health Connect (whatever app writes steps there, for example OnePlus Health). */
class StepsSource(private val context: Context) {
    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun isAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client() = HealthConnectClient.getOrCreate(context)

    suspend fun hasPermission(): Boolean =
        client().permissionController.getGrantedPermissions().containsAll(permissions)

    /** Null when no app has written any steps today. */
    suspend fun today(): Long? {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val result = client().aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now()),
            ),
        )
        return result[StepsRecord.COUNT_TOTAL]
    }
}
