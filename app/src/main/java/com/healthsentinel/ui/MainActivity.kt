package com.healthsentinel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.healthsentinel.healthconnect.HealthConnectManager
import com.healthsentinel.viewmodel.HealthViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HealthViewModel by viewModels()
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var manager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = HealthConnectManager(this)
        permissionLauncher = registerForActivityResult(manager.permissionContract) {
            viewModel.refreshPermissions()
        }
        setContent {
            MaterialTheme {
                HealthSentinelApp(viewModel, manager.permissions) { permissionLauncher.launch(manager.permissions) }
            }
        }
    }
}

@Composable
private fun HealthSentinelApp(
    viewModel: HealthViewModel,
    permissions: Set<String>,
    onRequestPermissions: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf("Dashboard") }

    if (!state.hasPermissions || !state.healthConnectAvailable) {
        PermissionScreen(
            available = state.healthConnectAvailable,
            permissions = permissions,
            message = state.message,
            onRequestPermissions = onRequestPermissions
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Dashboard", "History").forEach { label ->
                    NavigationBarItem(
                        selected = tab == label,
                        onClick = { tab = label },
                        label = { Text(label) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            "History" -> HistoryScreen(
                modifier = Modifier.padding(padding),
                summaries = state.recent,
                loading = state.loading,
                message = state.message,
                backendUrl = state.backendUrl,
                apiKey = state.apiKey,
                onSaveBackendSettings = viewModel::saveBackendSettings,
                onUpload = viewModel::uploadLatestSummaries
            )
            else -> DashboardScreen(
                modifier = Modifier.padding(padding),
                summary = state.latest,
                loading = state.loading,
                message = state.message,
                onSync = viewModel::syncNow
            )
        }
    }
}
