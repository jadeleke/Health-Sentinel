package com.healthsentinel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    available: Boolean,
    permissions: Set<String>,
    message: String?,
    onRequestPermissions: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Health Sentinel", style = MaterialTheme.typography.headlineSmall)
        Text("This app reads Health Connect data from your Oraimo sync to build local daily wellness summaries.")
        Text("Data requested: steps, heart rate, resting heart rate, sleep, body temperature if available, and oxygen saturation if available.")
        Text("Your data is stored locally unless you configure a backend and upload it.")
        Text("This app is for personal experimentation only and does not provide medical advice.")
        if (!available) {
            Text("Health Connect is not available. Android 14+ includes Health Connect; Android 13 and lower may require the Health Connect app.")
        } else {
            Button(onClick = onRequestPermissions) { Text("Grant Health Connect permissions") }
            Text("${permissions.size} permissions requested.")
        }
        if (message != null) Text(message)
    }
}
