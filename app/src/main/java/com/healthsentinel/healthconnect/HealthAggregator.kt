package com.healthsentinel.healthconnect

import com.healthsentinel.data.DailyHealthSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HealthAggregator {
    fun aggregate(bundle: HealthRecordBundle, zoneId: ZoneId = ZoneId.systemDefault()): List<DailyHealthSummary> {
        val dates = mutableSetOf<LocalDate>()
        bundle.steps.forEach { dates.add(it.startTime.atZone(zoneId).toLocalDate()) }
        bundle.heartRates.forEach { dates.add(it.startTime.atZone(zoneId).toLocalDate()) }
        bundle.restingHeartRates.forEach { dates.add(it.time.atZone(zoneId).toLocalDate()) }
        bundle.sleepSessions.forEach { dates.add(it.startTime.atZone(zoneId).toLocalDate()) }
        bundle.bodyTemperatures.forEach { dates.add(it.time.atZone(zoneId).toLocalDate()) }
        bundle.oxygenSaturations.forEach { dates.add(it.time.atZone(zoneId).toLocalDate()) }

        val now = System.currentTimeMillis()
        return dates.sorted().map { date ->
            val start = date.atStartOfDay(zoneId).toInstant()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()

            val steps = bundle.steps
                .filter { overlaps(it.startTime, it.endTime, start, end) }
                .sumOf { it.count }
                .takeIf { it > 0L }

            val heartRateSamples = bundle.heartRates
                .filter { overlaps(it.startTime, it.endTime, start, end) }
                .flatMap { it.samples }
                .filter { it.time >= start && it.time < end }
                .map { it.beatsPerMinute.toDouble() }

            val resting = bundle.restingHeartRates
                .filter { it.time >= start && it.time < end }
                .map { it.beatsPerMinute.toDouble() }
                .takeIf { it.isNotEmpty() }
                ?.average()

            val sleepMinutes = bundle.sleepSessions
                .filter { overlaps(it.startTime, it.endTime, start, end) }
                .sumOf {
                    // Sleep stages are represented inside SleepSessionRecord in current SDKs.
                    val stageMinutes = it.stages.sumOf { stage ->
                        clippedMinutes(stage.startTime, stage.endTime, start, end)
                    }
                    stageMinutes.takeIf { minutes -> minutes > 0L }
                        ?: clippedMinutes(it.startTime, it.endTime, start, end)
                }
                .takeIf { it > 0L }

            val temps = bundle.bodyTemperatures
                .filter { it.time >= start && it.time < end }
                .map { it.temperature.inCelsius }

            val spo2 = bundle.oxygenSaturations
                .filter { it.time >= start && it.time < end }
                .map { it.percentage.value }

            DailyHealthSummary(
                date = date.toString(),
                steps = steps,
                sleepMinutes = sleepMinutes,
                restingHeartRate = resting,
                avgHeartRate = heartRateSamples.takeIf { it.isNotEmpty() }?.average(),
                minHeartRate = heartRateSamples.minOrNull(),
                maxHeartRate = heartRateSamples.maxOrNull(),
                bodyTemperatureCelsius = temps.takeIf { it.isNotEmpty() }?.average(),
                oxygenSaturationPercent = spo2.takeIf { it.isNotEmpty() }?.average(),
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun overlaps(recordStart: Instant, recordEnd: Instant, rangeStart: Instant, rangeEnd: Instant): Boolean {
        return recordStart < rangeEnd && recordEnd > rangeStart
    }

    private fun clippedMinutes(recordStart: Instant, recordEnd: Instant, rangeStart: Instant, rangeEnd: Instant): Long {
        val start = maxOf(recordStart, rangeStart)
        val end = minOf(recordEnd, rangeEnd)
        return Duration.between(start, end).toMinutes().coerceAtLeast(0)
    }
}
