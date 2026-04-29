package com.healthsentinel.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthsentinel.analysis.BaselineAnalyzer
import com.healthsentinel.data.DailyHealthSummary
import com.healthsentinel.data.HealthDatabase
import com.healthsentinel.healthconnect.HealthAggregator
import com.healthsentinel.healthconnect.HealthConnectManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

data class HealthUiState(
    val latest: DailyHealthSummary? = null,
    val recent: List<DailyHealthSummary> = emptyList(),
    val loading: Boolean = false,
    val hasPermissions: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val message: String? = null,
    val backendUrl: String = "",
    val apiKey: String = ""
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HealthDatabase.get(application).dailyHealthDao()
    private val manager = HealthConnectManager(application)
    private val prefs = application.getSharedPreferences("health_sentinel", 0)
    private val client = OkHttpClient()

    private val mutable = kotlinx.coroutines.flow.MutableStateFlow(
        HealthUiState(
            healthConnectAvailable = manager.isAvailable,
            backendUrl = prefs.getString("backend_url", "") ?: "",
            apiKey = prefs.getString("api_key", "") ?: ""
        )
    )

    val uiState: StateFlow<HealthUiState> = combine(
        mutable,
        dao.observeLatest(),
        dao.observeRecent(30)
    ) { state, latest, recent ->
        state.copy(latest = latest, recent = recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mutable.value)

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            mutable.value = mutable.value.copy(
                healthConnectAvailable = manager.isAvailable,
                hasPermissions = manager.hasAllPermissions()
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, message = null)
            try {
                val zone = ZoneId.systemDefault()
                val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
                val start = LocalDate.now(zone).minusDays(29).atStartOfDay(zone).toInstant()
                val records = manager.readRecords(start, end)
                dao.upsertAll(HealthAggregator.aggregate(records, zone))
                dao.upsertAll(BaselineAnalyzer.analyze(dao.getAllAscending()))
                mutable.value = mutable.value.copy(loading = false, message = "Sync complete")
            } catch (error: Throwable) {
                mutable.value = mutable.value.copy(loading = false, message = error.message ?: "Sync failed")
            }
        }
    }

    fun saveBackendSettings(url: String, apiKey: String) {
        prefs.edit().putString("backend_url", url.trim()).putString("api_key", apiKey.trim()).apply()
        mutable.value = mutable.value.copy(backendUrl = url.trim(), apiKey = apiKey.trim(), message = "Backend settings saved")
    }

    fun uploadLatestSummaries() {
        viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, message = null)
            try {
                // TODO: cloud backup - replace this manual upload with a user-controlled backup flow.
                // TODO: export to CSV - add local export of DailyHealthSummary rows.
                val state = mutable.value
                require(state.backendUrl.isNotBlank()) { "Backend URL is required" }
                require(state.apiKey.isNotBlank()) { "API key is required" }
                val summaries = dao.getRecent(30)
                val json = JSONArray(summaries.reversed().map { it.toJson() }).toString()
                val request = Request.Builder()
                    .url(state.backendUrl.trimEnd('/') + "/health/bulk-summary")
                    .addHeader("X-Health-Sentinel-Key", state.apiKey)
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) error("Upload failed: HTTP ${it.code}")
                }
                mutable.value = mutable.value.copy(loading = false, message = "Upload complete")
            } catch (error: Throwable) {
                mutable.value = mutable.value.copy(loading = false, message = error.message ?: "Upload failed")
            }
        }
    }

    private fun DailyHealthSummary.toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        putNullable("steps", steps)
        putNullable("sleepMinutes", sleepMinutes)
        putNullable("restingHeartRate", restingHeartRate)
        putNullable("avgHeartRate", avgHeartRate)
        putNullable("minHeartRate", minHeartRate)
        putNullable("maxHeartRate", maxHeartRate)
        putNullable("bodyTemperatureCelsius", bodyTemperatureCelsius)
        putNullable("oxygenSaturationPercent", oxygenSaturationPercent)
        put("source", "health_connect")
    }

    private fun JSONObject.putNullable(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }
}
