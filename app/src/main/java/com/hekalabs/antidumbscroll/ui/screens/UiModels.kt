package com.hekalabs.antidumbscroll.ui.screens

/**
 * Represents the phases of a Pomodoro timer session.
 */
enum class PomodoroPhase {
    IDLE, FOKUS, ISTIRAHAT_PENDEK, ISTIRAHAT_PANJANG
}

/**
 * Represents an achievement the user can unlock.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconResName: String
)

/**
 * Represents tracked usage of a single app.
 */
data class AppUsage(
    val packageName: String,
    val usageTimeMs: Long,
    val limitMs: Long = DEFAULT_SESSION_LIMIT_MS
) {
    companion object {
        const val DEFAULT_SESSION_LIMIT_MS = 15 * 60 * 1000L
    }
}

/**
 * UI state for the Dashboard screen.
 */
data class DashboardState(
    val totalScreenTimeMs: Long = 0L,
    val appUsages: List<AppUsage> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val isMonitoredAppsEmpty: Boolean = true,
    val weeklyStats: List<Float> = emptyList(),
    val weeklyDayLabels: List<String> = emptyList(),
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5
)

/**
 * Represents an installed app that can be monitored.
 */
data class AppItem(
    val packageName: String,
    val name: String,
    val isMonitored: Boolean
)

/**
 * UI state for the Settings screen.
 */
data class SettingsState(
    val sessionLimitMinutes: Int = 15,
    val focusDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val longBreakCycle: Int = 4,
    val autoStartNextPhase: Boolean = false,
    val isScheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 22,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 6,
    val scheduleEndMinute: Int = 0,
    val monitoredApps: List<AppItem> = emptyList(),
    val showResetDialog: Boolean = false,
    val showAppPicker: Boolean = false
)
