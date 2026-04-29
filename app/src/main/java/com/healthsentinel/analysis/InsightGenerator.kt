package com.healthsentinel.analysis

object InsightGenerator {
    fun generate(status: String): String = when (status) {
        "Watch" -> "Some signals are outside your recent baseline. This may reflect stress, poor recovery, or early illness. Monitor how you feel."
        "Elevated" -> "Several signals are elevated compared with your recent baseline. This may indicate physiological stress. Consider rest and monitor symptoms. This is not a medical diagnosis."
        else -> "Your current signals are close to your recent baseline."
    }
}
