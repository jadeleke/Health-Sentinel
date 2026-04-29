package com.healthsentinel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.healthsentinel.data.DailyHealthSummary

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    summaries: List<DailyHealthSummary>,
    loading: Boolean,
    message: String?,
    backendUrl: String,
    apiKey: String,
    onSaveBackendSettings: (String, String) -> Unit,
    onUpload: () -> Unit
) {
    var url by remember(backendUrl) { mutableStateOf(backendUrl) }
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Last 30 days")
        OutlinedTextField(url, { url = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            key,
            { key = it },
            label = { Text("API key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSaveBackendSettings(url, key) }, enabled = !loading) { Text("Save") }
            Button(onClick = onUpload, enabled = !loading) { Text("Upload latest summaries") }
        }
        if (message != null) Text(message)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(summaries) { summary ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${summary.date} - ${summary.status} - score ${summary.anomalyScore?.let { "%.2f".format(it) } ?: "missing"}")
                        Text("Steps: ${summary.steps ?: "missing"} | Sleep: ${summary.sleepMinutes ?: "missing"} min | RHR: ${summary.restingHeartRate?.let { "%.0f".format(it) } ?: "missing"}")
                    }
                }
            }
        }
    }
}
