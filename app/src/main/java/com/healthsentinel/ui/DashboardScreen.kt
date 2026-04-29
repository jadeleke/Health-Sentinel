package com.healthsentinel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthsentinel.data.DailyHealthSummary

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    summary: DailyHealthSummary?,
    loading: Boolean,
    message: String?,
    onSync: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Health Sentinel", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onSync, enabled = !loading) { Text("Sync Now") }
        }
        Text("This app is for personal experimentation only and does not provide medical advice.")
        if (loading) CircularProgressIndicator()
        if (message != null) Text(message)
        if (summary == null) {
            Text("No summaries yet. Tap Sync Now.")
        } else {
            Text("Today: ${summary.date}", style = MaterialTheme.typography.titleMedium)
            MetricCard("Wellness status", summary.status)
            MetricCard("Steps", summary.steps?.let { "%,d".format(it) } ?: "missing")
            MetricCard("Sleep duration", summary.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "missing")
            MetricCard("Resting heart rate", summary.restingHeartRate?.let { "%.0f bpm".format(it) } ?: "missing")
            MetricCard("Average heart rate", summary.avgHeartRate?.let { "%.0f bpm".format(it) } ?: "missing")
            MetricCard("Minimum heart rate", summary.minHeartRate?.let { "%.0f bpm".format(it) } ?: "missing")
            MetricCard("Maximum heart rate", summary.maxHeartRate?.let { "%.0f bpm".format(it) } ?: "missing")
            MetricCard("Body temperature", summary.bodyTemperatureCelsius?.let { "%.1f C".format(it) } ?: "missing")
            MetricCard("Oxygen saturation", summary.oxygenSaturationPercent?.let { "%.1f%%".format(it) } ?: "missing")
            Text(summary.insight)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
