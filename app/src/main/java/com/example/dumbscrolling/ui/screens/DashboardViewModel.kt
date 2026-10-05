package com.example.dumbscrolling.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconResName: String
)

data class AppUsage(
    val packageName: String,
    val usageTimeMs: Long,
    val limitMs: Long = 15 * 60 * 1000L
)

data class DashboardState(
    val totalScreenTimeMs: Long = 0L,
    val appUsages: List<AppUsage> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val isMonitoredAppsEmpty: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()
    
    private val usagePrefs = application.getSharedPreferences("UsageStatsPrefs", Context.MODE_PRIVATE)
    private val settingsPrefs = application.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    init {
        loadData()
        startPeriodicUpdates()
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                loadData()
                delay(2000L) // Update every 2 seconds
            }
        }
    }

    private fun loadData() {
        val allEntries = usagePrefs.all
        val appUsages = mutableListOf<AppUsage>()
        var totalTime = 0L
        
        val limitMinutes = settingsPrefs.getInt("session_limit_minutes", 15)
        val limitMs = limitMinutes * 60 * 1000L
        val monitoredSet = settingsPrefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        
        val editor = usagePrefs.edit()
        var madeChanges = false

        for ((key, value) in allEntries) {
            if (key.startsWith("usage_") && value is Long) {
                val packageName = key.substringAfter("usage_")
                if (monitoredSet.contains(packageName)) {
                    appUsages.add(AppUsage(packageName, value, limitMs))
                    totalTime += value
                } else {
                    // Hapus data lama yang menghitung app tidak dipantau
                    editor.remove(key)
                    madeChanges = true
                }
            }
        }
        
        if (madeChanges) {
            editor.apply()
        }
        
        // Sort by usage time descending
        appUsages.sortByDescending { it.usageTimeMs }

        val achievements = checkAchievements(totalTime, appUsages)

        _uiState.update {
            it.copy(
                totalScreenTimeMs = totalTime,
                appUsages = appUsages,
                achievements = achievements,
                isMonitoredAppsEmpty = monitoredSet.isEmpty()
            )
        }
    }

    private fun checkAchievements(totalTime: Long, appUsages: List<AppUsage>): List<Achievement> {
        val list = mutableListOf<Achievement>()

        list.add(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Track your first app usage.",
                isUnlocked = totalTime > 0,
                iconResName = "Star"
            )
        )

        list.add(
            Achievement(
                id = "limit_reached",
                title = "Awareness",
                description = "Reach 15 seconds of tracked time.",
                isUnlocked = totalTime >= 15_000L,
                iconResName = "Warning"
            )
        )
        
        list.add(
            Achievement(
                id = "power_user",
                title = "Data Collector",
                description = "Track usage across at least 2 apps.",
                isUnlocked = appUsages.size >= 2,
                iconResName = "Analytics"
            )
        )

        return list
    }
}
