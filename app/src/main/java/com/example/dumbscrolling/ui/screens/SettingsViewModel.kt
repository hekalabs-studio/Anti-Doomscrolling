package com.example.dumbscrolling.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppItem(
    val packageName: String,
    val name: String,
    val isMonitored: Boolean
)

data class SettingsState(
    val sessionLimitMinutes: Int = 15,
    val monitoredApps: List<AppItem> = emptyList(),
    val showResetDialog: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    private val usagePrefs = application.getSharedPreferences("UsageStatsPrefs", Context.MODE_PRIVATE)
    private val focusPrefs = application.getSharedPreferences("FocusModePrefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    private val defaultApps = listOf(
        AppItem("com.instagram.android", "Instagram", true),
        AppItem("com.zhiliaoapp.musically", "TikTok", true),
        AppItem("com.google.android.youtube", "YouTube", true),
        AppItem("com.facebook.katana", "Facebook", true),
        AppItem("com.twitter.android", "X (Twitter)", true)
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val limit = prefs.getInt("session_limit_minutes", 15)
        
        val apps = defaultApps.map { defaultApp ->
            // Use saved value if exists, otherwise default
            val isMonitored = prefs.getBoolean("monitored_${defaultApp.packageName}", defaultApp.isMonitored)
            defaultApp.copy(isMonitored = isMonitored)
        }
        
        _uiState.update { it.copy(sessionLimitMinutes = limit, monitoredApps = apps) }
    }

    fun updateSessionLimit(minutes: Int) {
        prefs.edit().putInt("session_limit_minutes", minutes).apply()
        _uiState.update { it.copy(sessionLimitMinutes = minutes) }
    }

    fun toggleAppMonitoring(packageName: String, isMonitored: Boolean) {
        prefs.edit().putBoolean("monitored_$packageName", isMonitored).apply()
        _uiState.update { state ->
            val updatedApps = state.monitoredApps.map {
                if (it.packageName == packageName) it.copy(isMonitored = isMonitored) else it
            }
            state.copy(monitoredApps = updatedApps)
        }
    }
    
    fun showResetDialog() {
        _uiState.update { it.copy(showResetDialog = true) }
    }
    
    fun hideResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun resetData() {
        usagePrefs.edit().clear().apply()
        focusPrefs.edit().putInt("focus_session_count", 0).apply() // Assuming this is how it's stored
        hideResetDialog()
    }
}
