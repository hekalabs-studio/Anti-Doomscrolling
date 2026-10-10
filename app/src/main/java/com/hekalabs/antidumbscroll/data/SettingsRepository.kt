package com.hekalabs.antidumbscroll.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_datastore")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private val SESSION_LIMIT = intPreferencesKey("session_limit_minutes")
    private val FOCUS_DURATION = intPreferencesKey("focus_duration")
    private val SHORT_BREAK = intPreferencesKey("short_break_duration")
    private val LONG_BREAK = intPreferencesKey("long_break_duration")
    private val LONG_BREAK_CYCLE = intPreferencesKey("long_break_cycle")
    private val AUTO_START = booleanPreferencesKey("auto_start_next_phase")
    private val IS_SCHEDULE_ENABLED = booleanPreferencesKey("is_schedule_enabled")
    private val SCHEDULE_START_HOUR = intPreferencesKey("schedule_start_hour")
    private val SCHEDULE_START_MINUTE = intPreferencesKey("schedule_start_minute")
    private val SCHEDULE_END_HOUR = intPreferencesKey("schedule_end_hour")
    private val SCHEDULE_END_MINUTE = intPreferencesKey("schedule_end_minute")
    private val MONITORED_APPS = stringSetPreferencesKey("monitored_apps")

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            sessionLimitMinutes = preferences[SESSION_LIMIT] ?: 15,
            focusDuration = preferences[FOCUS_DURATION] ?: 25,
            shortBreakDuration = preferences[SHORT_BREAK] ?: 5,
            longBreakDuration = preferences[LONG_BREAK] ?: 15,
            longBreakCycle = preferences[LONG_BREAK_CYCLE] ?: 4,
            autoStartNextPhase = preferences[AUTO_START] ?: false,
            isScheduleEnabled = preferences[IS_SCHEDULE_ENABLED] ?: false,
            scheduleStartHour = preferences[SCHEDULE_START_HOUR] ?: 22,
            scheduleStartMinute = preferences[SCHEDULE_START_MINUTE] ?: 0,
            scheduleEndHour = preferences[SCHEDULE_END_HOUR] ?: 6,
            scheduleEndMinute = preferences[SCHEDULE_END_MINUTE] ?: 0,
            monitoredApps = preferences[MONITORED_APPS] ?: setOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.google.android.youtube",
                "com.facebook.katana",
                "com.twitter.android"
            )
        )
    }

    suspend fun updateSessionLimit(minutes: Int) {
        dataStore.edit { it[SESSION_LIMIT] = minutes }
    }

    suspend fun updateFocusDuration(minutes: Int) {
        dataStore.edit { it[FOCUS_DURATION] = minutes }
    }

    suspend fun updateShortBreakDuration(minutes: Int) {
        dataStore.edit { it[SHORT_BREAK] = minutes }
    }

    suspend fun updateLongBreakDuration(minutes: Int) {
        dataStore.edit { it[LONG_BREAK] = minutes }
    }

    suspend fun updateLongBreakCycle(cycles: Int) {
        dataStore.edit { it[LONG_BREAK_CYCLE] = cycles }
    }

    suspend fun updateAutoStart(autoStart: Boolean) {
        dataStore.edit { it[AUTO_START] = autoStart }
    }

    suspend fun updateScheduleEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_SCHEDULE_ENABLED] = enabled }
    }

    suspend fun updateScheduleStart(hour: Int, minute: Int) {
        dataStore.edit { 
            it[SCHEDULE_START_HOUR] = hour
            it[SCHEDULE_START_MINUTE] = minute
        }
    }

    suspend fun updateScheduleEnd(hour: Int, minute: Int) {
        dataStore.edit { 
            it[SCHEDULE_END_HOUR] = hour
            it[SCHEDULE_END_MINUTE] = minute
        }
    }

    suspend fun updateMonitoredApps(apps: Set<String>) {
        dataStore.edit { it[MONITORED_APPS] = apps }
    }
}

data class AppSettings(
    val sessionLimitMinutes: Int,
    val focusDuration: Int,
    val shortBreakDuration: Int,
    val longBreakDuration: Int,
    val longBreakCycle: Int,
    val autoStartNextPhase: Boolean,
    val isScheduleEnabled: Boolean,
    val scheduleStartHour: Int,
    val scheduleStartMinute: Int,
    val scheduleEndHour: Int,
    val scheduleEndMinute: Int,
    val monitoredApps: Set<String>
)
