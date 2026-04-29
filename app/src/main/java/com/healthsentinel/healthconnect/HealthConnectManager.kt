package com.healthsentinel.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

data class HealthRecordBundle(
    val steps: List<StepsRecord>,
    val heartRates: List<HeartRateRecord>,
    val restingHeartRates: List<RestingHeartRateRecord>,
    val sleepSessions: List<SleepSessionRecord>,
    val bodyTemperatures: List<BodyTemperatureRecord>,
    val oxygenSaturations: List<OxygenSaturationRecord>
)

class HealthConnectManager(private val context: Context) {
    val availability: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val isAvailable: Boolean
        get() = availability == HealthConnectClient.SDK_AVAILABLE

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission<StepsRecord>(),
        HealthPermission.getReadPermission<HeartRateRecord>(),
        HealthPermission.getReadPermission<RestingHeartRateRecord>(),
        HealthPermission.getReadPermission<SleepSessionRecord>(),
        HealthPermission.getReadPermission<BodyTemperatureRecord>(),
        HealthPermission.getReadPermission<OxygenSaturationRecord>()
    )

    val permissionContract = PermissionController.createRequestPermissionResultContract()

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable) return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readRecords(start: Instant, end: Instant): HealthRecordBundle {
        return HealthRecordBundle(
            steps = read<StepsRecord>(start, end),
            heartRates = read<HeartRateRecord>(start, end),
            restingHeartRates = read<RestingHeartRateRecord>(start, end),
            sleepSessions = read<SleepSessionRecord>(start, end),
            bodyTemperatures = optionalRead<BodyTemperatureRecord>(start, end),
            oxygenSaturations = optionalRead<OxygenSaturationRecord>(start, end)
        )
    }

    private suspend inline fun <reified T : androidx.health.connect.client.records.Record> read(
        start: Instant,
        end: Instant
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = T::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageToken = pageToken
                )
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private suspend inline fun <reified T : androidx.health.connect.client.records.Record> optionalRead(
        start: Instant,
        end: Instant
    ): List<T> = try {
        read<T>(start, end)
    } catch (_: Throwable) {
        emptyList()
    }
}
