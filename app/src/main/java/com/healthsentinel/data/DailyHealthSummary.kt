package com.healthsentinel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_summaries")
data class DailyHealthSummary(
    @PrimaryKey val date: String,
    val steps: Long? = null,
    val sleepMinutes: Long? = null,
    val restingHeartRate: Double? = null,
    val avgHeartRate: Double? = null,
    val minHeartRate: Double? = null,
    val maxHeartRate: Double? = null,
    val bodyTemperatureCelsius: Double? = null,
    val oxygenSaturationPercent: Double? = null,
    val restingHrDelta7d: Double? = null,
    val sleepDelta7d: Double? = null,
    val stepsDelta7d: Double? = null,
    val temperatureDelta7d: Double? = null,
    val anomalyScore: Double? = null,
    val status: String = "Normal",
    val insight: String = "Your current signals are close to your recent baseline.",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
