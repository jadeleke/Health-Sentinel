package com.healthsentinel.analysis

import com.healthsentinel.data.DailyHealthSummary

object BaselineAnalyzer {
    fun analyze(summariesAscending: List<DailyHealthSummary>): List<DailyHealthSummary> {
        // TODO: TimesFM forecasting - compare these rule-based baselines with forecasted trends.
        return summariesAscending.mapIndexed { index, summary ->
            val previous = summariesAscending
                .take(index)
                .takeLast(7)

            val restingDelta = delta(summary.restingHeartRate, previous.mapNotNull { it.restingHeartRate })
            val sleepDelta = delta(summary.sleepMinutes?.toDouble(), previous.mapNotNull { it.sleepMinutes?.toDouble() })
            val stepsDelta = delta(summary.steps?.toDouble(), previous.mapNotNull { it.steps?.toDouble() })
            val tempDelta = delta(summary.bodyTemperatureCelsius, previous.mapNotNull { it.bodyTemperatureCelsius })

            val score = score(restingDelta, sleepDelta, stepsDelta, tempDelta, summary.oxygenSaturationPercent)
            val status = when {
                score >= 0.60 -> "Elevated"
                score >= 0.30 -> "Watch"
                else -> "Normal"
            }

            summary.copy(
                restingHrDelta7d = restingDelta,
                sleepDelta7d = sleepDelta,
                stepsDelta7d = stepsDelta,
                temperatureDelta7d = tempDelta,
                anomalyScore = score,
                status = status,
                insight = InsightGenerator.generate(status),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun delta(value: Double?, previousValues: List<Double>): Double? {
        if (value == null || previousValues.isEmpty()) return null
        return value - previousValues.average()
    }

    private fun score(
        restingDelta: Double?,
        sleepDelta: Double?,
        stepsDelta: Double?,
        tempDelta: Double?,
        oxygenSaturationPercent: Double?
    ): Double {
        var score = 0.0
        if (restingDelta != null) {
            score += when {
                restingDelta >= 8.0 -> 0.35
                restingDelta >= 5.0 -> 0.20
                else -> 0.0
            }
        }
        if (sleepDelta != null && sleepDelta <= -60.0) score += 0.20
        if (stepsDelta != null && stepsDelta <= -2500.0) score += 0.15
        if (tempDelta != null && tempDelta >= 0.3) score += 0.20
        if (oxygenSaturationPercent != null && oxygenSaturationPercent < 95.0) score += 0.10
        return score.coerceIn(0.0, 1.0)
    }
}
