package com.hekalabs.antidumbscroll.ui.screens

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hekalabs.antidumbscroll.data.DailyStat
import com.hekalabs.antidumbscroll.data.StatsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

import com.hekalabs.antidumbscroll.data.SettingsRepository
import com.hekalabs.antidumbscroll.data.AppSettings
import com.hekalabs.antidumbscroll.R

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val statsDao: StatsDao,
    @Named("UsagePrefs") private val usagePrefs: SharedPreferences,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()
    
    private var currentSettings: AppSettings? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
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
        val settings = currentSettings ?: return
        
        val allEntries = usagePrefs.all
        val appUsages = mutableListOf<AppUsage>()
        var totalTime = 0L
        
        val limitMinutes = settings.sessionLimitMinutes
        val limitMs = limitMinutes * 60 * 1000L
        val monitoredSet = settings.monitoredApps
        
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

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = dateFormat.format(java.util.Date())

        viewModelScope.launch {
            if (totalTime > 0L) {
                withContext(Dispatchers.IO) {
                    statsDao.insertOrUpdate(DailyStat(todayStr, totalTime))
                }
            }

            val statsList = statsDao.getStatsForLast7Days()

            val calendar = java.util.Calendar.getInstance()
            val dayNameFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

            val last7Dates = mutableListOf<String>()
            val last7DayLabels = mutableListOf<String>()

            calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until 7) {
                last7Dates.add(dateFormat.format(calendar.time))
                last7DayLabels.add(dayNameFormat.format(calendar.time))
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val statsMap = statsList.associateBy { it.dateStr }
            val floatList = mutableListOf<Float>()

            for (dateStr in last7Dates) {
                val durationMs = if (dateStr == todayStr) {
                    maxOf(statsMap[dateStr]?.totalDurationMs ?: 0L, totalTime)
                } else {
                    statsMap[dateStr]?.totalDurationMs ?: 0L
                }
                floatList.add(durationMs / 60000f) // Dalam menit
            }

            _uiState.update {
                it.copy(
                    totalScreenTimeMs = totalTime,
                    appUsages = appUsages,
                    achievements = achievements,
                    isMonitoredAppsEmpty = monitoredSet.isEmpty(),
                    weeklyStats = floatList,
                    weeklyDayLabels = last7DayLabels,
                    focusDuration = settings.focusDuration,
                    shortBreakDuration = settings.shortBreakDuration
                )
            }
        }
    }

    private fun checkAchievements(totalTime: Long, appUsages: List<AppUsage>): List<Achievement> {
        val list = mutableListOf<Achievement>()
        val app = getApplication<Application>()

        list.add(
            Achievement(
                id = "first_step",
                title = app.getString(R.string.achievement_1_title),
                description = app.getString(R.string.achievement_1_desc),
                isUnlocked = totalTime > 0,
                iconResName = "Star"
            )
        )

        list.add(
            Achievement(
                id = "limit_reached",
                title = app.getString(R.string.achievement_2_title),
                description = app.getString(R.string.achievement_2_desc),
                isUnlocked = totalTime >= 15_000L,
                iconResName = "Warning"
            )
        )
        
        list.add(
            Achievement(
                id = "power_user",
                title = app.getString(R.string.achievement_3_title),
                description = app.getString(R.string.achievement_3_desc),
                isUnlocked = appUsages.size >= 2,
                iconResName = "Analytics"
            )
        )

        return list
    }
}
