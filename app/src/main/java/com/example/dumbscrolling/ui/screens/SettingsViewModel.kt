package com.example.dumbscrolling.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val longBreakCycle: Int = 4,
    val autoStartNextPhase: Boolean = false,
    val monitoredApps: List<AppItem> = emptyList(),
    val showResetDialog: Boolean = false,
    val showAppPicker: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    private val usagePrefs = application.getSharedPreferences("UsageStatsPrefs", Context.MODE_PRIVATE)
    private val focusPrefs = application.getSharedPreferences("FocusModePrefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val limit = prefs.getInt("session_limit_minutes", 15)
        val fDuration = prefs.getInt("focus_duration", 25)
        val sBreakDuration = prefs.getInt("short_break_duration", 5)
        val lBreakDuration = prefs.getInt("long_break_duration", 15)
        val lBreakCycle = prefs.getInt("long_break_cycle", 4)
        val autoStart = prefs.getBoolean("auto_start_next_phase", false)
        
        val monitoredSet = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        val apps = getInstalledApps(monitoredSet)
        
        _uiState.update { it.copy(
            sessionLimitMinutes = limit,
            focusDuration = fDuration,
            shortBreakDuration = sBreakDuration,
            longBreakDuration = lBreakDuration,
            longBreakCycle = lBreakCycle,
            autoStartNextPhase = autoStart,
            monitoredApps = apps
        ) }
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
        prefs.edit().putInt("session_limit_minutes", minutes).apply()
        _uiState.update { it.copy(sessionLimitMinutes = minutes) }
    }
    
    fun updateFocusDuration(minutes: Int) {
        prefs.edit().putInt("focus_duration", minutes).apply()
        _uiState.update { it.copy(focusDuration = minutes) }
    }

    fun updateShortBreakDuration(minutes: Int) {
        prefs.edit().putInt("short_break_duration", minutes).apply()
        _uiState.update { it.copy(shortBreakDuration = minutes) }
    }

    fun updateLongBreakDuration(minutes: Int) {
        prefs.edit().putInt("long_break_duration", minutes).apply()
        _uiState.update { it.copy(longBreakDuration = minutes) }
    }

    fun updateLongBreakCycle(cycles: Int) {
        prefs.edit().putInt("long_break_cycle", cycles).apply()
        _uiState.update { it.copy(longBreakCycle = cycles) }
    }

    fun updateAutoStartNextPhase(autoStart: Boolean) {
        prefs.edit().putBoolean("auto_start_next_phase", autoStart).apply()
        _uiState.update { it.copy(autoStartNextPhase = autoStart) }
    }
    
    fun restoreDefaultPomodoroSettings() {
        prefs.edit()
            .putInt("focus_duration", 25)
            .putInt("short_break_duration", 5)
            .putInt("long_break_duration", 15)
            .putInt("long_break_cycle", 4)
            .putBoolean("auto_start_next_phase", false)
            .apply()
            
        _uiState.update { it.copy(
            focusDuration = 25,
            shortBreakDuration = 5,
            longBreakDuration = 15,
            longBreakCycle = 4,
            autoStartNextPhase = false
        ) }
    }

    fun toggleAppMonitoring(packageName: String, isMonitored: Boolean) {
        val currentSet = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet()
        if (isMonitored) {
            newSet.add(packageName)
        } else {
            newSet.remove(packageName)
        }
        
        prefs.edit().putStringSet("monitored_apps", newSet).apply()
        
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
        focusPrefs.edit().putInt("focus_session_count", 0).apply()
        hideResetDialog()
    }
    
    fun showAppPicker() {
        _uiState.update { it.copy(showAppPicker = true) }
    }
    
    fun hideAppPicker() {
        _uiState.update { it.copy(showAppPicker = false) }
    }
}
