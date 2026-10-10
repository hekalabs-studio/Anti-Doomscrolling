package com.hekalabs.antidumbscroll.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hekalabs.antidumbscroll.data.StatsDao
import com.hekalabs.antidumbscroll.data.SettingsRepository
import com.hekalabs.antidumbscroll.data.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    @Named("UsagePrefs") private val usagePrefs: SharedPreferences,
    @Named("FocusPrefs") private val focusPrefs: SharedPreferences,
    private val statsDao: StatsDao
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        sessionLimitMinutes = settings.sessionLimitMinutes,
                        focusDuration = settings.focusDuration,
                        shortBreakDuration = settings.shortBreakDuration,
                        longBreakDuration = settings.longBreakDuration,
                        longBreakCycle = settings.longBreakCycle,
                        autoStartNextPhase = settings.autoStartNextPhase,
                        isScheduleEnabled = settings.isScheduleEnabled,
                        scheduleStartHour = settings.scheduleStartHour,
                        scheduleStartMinute = settings.scheduleStartMinute,
                        scheduleEndHour = settings.scheduleEndHour,
                        scheduleEndMinute = settings.scheduleEndMinute,
                        monitoredApps = getInstalledApps(settings.monitoredApps)
                    )
                }
            }
        }
    }

    private fun getInstalledApps(monitoredSet: Set<String>): List<AppItem> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        
        return resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(pm).toString()
            
            val appInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            
            val isSystemApp = appInfo?.let { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
            val isAllowedSystemApp = packageName == "com.google.android.youtube" || packageName == "com.android.chrome"
            
            val isBlacklisted = packageName == getApplication<Application>().packageName ||
                packageName == "com.android.settings" ||
                packageName.contains("dialer") ||
                packageName.contains("contacts") ||
                packageName.contains("messaging") ||
                packageName.contains("mms") ||
                packageName.contains("maps") ||
                packageName.contains("camera") ||
                packageName.contains("telecom")
            
            if (isBlacklisted || (isSystemApp && !isAllowedSystemApp)) {
                null
            } else {
                AppItem(packageName, name, monitoredSet.contains(packageName))
            }
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }

    fun updateSessionLimit(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateSessionLimit(minutes) }
    }
    
    fun updateFocusDuration(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateFocusDuration(minutes) }
    }

    fun updateShortBreakDuration(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateShortBreakDuration(minutes) }
    }

    fun updateLongBreakDuration(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateLongBreakDuration(minutes) }
    }

    fun updateLongBreakCycle(cycles: Int) {
        viewModelScope.launch { settingsRepository.updateLongBreakCycle(cycles) }
    }

    fun updateAutoStartNextPhase(autoStart: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoStart(autoStart) }
    }
    
    fun updateScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateScheduleEnabled(enabled) }
    }
    
    fun updateScheduleStartTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.updateScheduleStart(hour, minute) }
    }
    
    fun updateScheduleEndTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.updateScheduleEnd(hour, minute) }
    }
    
    fun restoreDefaultPomodoroSettings() {
        viewModelScope.launch {
            settingsRepository.updateFocusDuration(25)
            settingsRepository.updateShortBreakDuration(5)
            settingsRepository.updateLongBreakDuration(15)
            settingsRepository.updateLongBreakCycle(4)
            settingsRepository.updateAutoStart(false)
        }
    }

    fun toggleAppMonitoring(packageName: String, isMonitored: Boolean) {
        val currentSet = _uiState.value.monitoredApps
            .filter { it.isMonitored }
            .map { it.packageName }
            .toMutableSet()
            
        if (isMonitored) {
            currentSet.add(packageName)
        } else {
            currentSet.remove(packageName)
        }
        
        viewModelScope.launch { settingsRepository.updateMonitoredApps(currentSet) }
    }
    
    fun showResetDialog() {
        _uiState.update { it.copy(showResetDialog = true) }
    }
    
    fun hideResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun resetData() {
        usagePrefs.edit().clear().apply()

        val focusEditor = focusPrefs.edit()
        focusEditor.putInt("focus_session_count", 0)
        focusEditor.putInt("completed_sessions_today", 0)
        focusPrefs.all.keys.filter { it.startsWith("continue_count_") }.forEach { key ->
            focusEditor.remove(key)
        }
        focusEditor.apply()

        viewModelScope.launch(Dispatchers.IO) {
            statsDao.deleteAllStats()
        }

        hideResetDialog()
    }
    
    fun showAppPicker() {
        _uiState.update { it.copy(showAppPicker = true) }
    }
    
    fun hideAppPicker() {
        _uiState.update { it.copy(showAppPicker = false) }
    }
}
