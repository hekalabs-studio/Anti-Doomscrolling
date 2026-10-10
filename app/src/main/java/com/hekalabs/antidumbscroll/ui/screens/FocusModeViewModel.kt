package com.hekalabs.antidumbscroll.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hekalabs.antidumbscroll.services.PomodoroService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import com.hekalabs.antidumbscroll.data.SettingsRepository
import com.hekalabs.antidumbscroll.data.AppSettings

@HiltViewModel
class FocusModeViewModel @Inject constructor(
    application: Application,
    @Named("FocusPrefs") private val prefs: SharedPreferences,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _currentPhase = MutableStateFlow(
        PomodoroPhase.valueOf(prefs.getString("current_phase", PomodoroPhase.IDLE.name) ?: PomodoroPhase.IDLE.name)
    )
    val currentPhase: StateFlow<PomodoroPhase> = _currentPhase.asStateFlow()

    private val _currentCycle = MutableStateFlow(prefs.getInt("current_cycle", 1))
    val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()

    private val _completedSessionsToday = MutableStateFlow(prefs.getInt("completed_sessions_today", 0))
    val completedSessionsToday: StateFlow<Int> = _completedSessionsToday.asStateFlow()

    private val _isPaused = MutableStateFlow(prefs.getBoolean("is_paused", false))
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var currentSettings: AppSettings? = null

    val totalCycles: Int
        get() = currentSettings?.longBreakCycle ?: 4

    private var countdownJob: Job? = null
    private var currentEndTime = 0L

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "current_phase" -> {
                val phaseName = prefs.getString("current_phase", PomodoroPhase.IDLE.name) ?: PomodoroPhase.IDLE.name
                val newPhase = runCatching { PomodoroPhase.valueOf(phaseName) }.getOrDefault(PomodoroPhase.IDLE)
                _currentPhase.value = newPhase
                if (newPhase == PomodoroPhase.IDLE) {
                    countdownJob?.cancel()
                    _isPaused.value = false
                    _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)
                }
            }
            "is_paused" -> {
                val paused = prefs.getBoolean("is_paused", false)
                _isPaused.value = paused
                if (paused) {
                    countdownJob?.cancel()
                    _remainingTimeMs.value = prefs.getLong("remaining_time_when_paused", prefs.getLong("remaining_pause_ms", 0L))
                } else {
                    val endTime = prefs.getLong("focus_target_time_ms", 0L)
                    val currentTime = System.currentTimeMillis()
                    if (endTime > currentTime && _currentPhase.value != PomodoroPhase.IDLE) {
                        startCountdown(endTime)
                    }
                }
            }
            "remaining_time_when_paused", "remaining_pause_ms" -> {
                if (_isPaused.value) {
                    _remainingTimeMs.value = prefs.getLong("remaining_time_when_paused", prefs.getLong("remaining_pause_ms", 0L))
                }
            }
            "current_cycle" -> {
                _currentCycle.value = prefs.getInt("current_cycle", 1)
            }
            "completed_sessions_today" -> {
                _completedSessionsToday.value = prefs.getInt("completed_sessions_today", 0)
            }
            "focus_target_time_ms" -> {
                if (!_isPaused.value && _currentPhase.value != PomodoroPhase.IDLE) {
                    val endTime = prefs.getLong("focus_target_time_ms", 0L)
                    val currentTime = System.currentTimeMillis()
                    if (endTime > currentTime) {
                        startCountdown(endTime)
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
                // If IDLE, update remaining time to reflect new focus duration
                if (_currentPhase.value == PomodoroPhase.IDLE) {
                    _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        checkStatus()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private fun checkStatus() {
        if (_currentPhase.value == PomodoroPhase.IDLE) {
            _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)
            return
        }

        if (_isPaused.value) {
            _remainingTimeMs.value = prefs.getLong("remaining_time_when_paused", prefs.getLong("remaining_pause_ms", 0L))
        } else {
            val endTime = prefs.getLong("focus_target_time_ms", 0L)
            val currentTime = System.currentTimeMillis()

            if (endTime > currentTime) {
                startCountdown(endTime)
            } else if (endTime > 0) {
                _remainingTimeMs.value = 0L
            }
        }
    }

    private fun startCountdown(endTime: Long) {
        if (countdownJob?.isActive == true && currentEndTime == endTime) return
        currentEndTime = endTime
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_currentPhase.value != PomodoroPhase.IDLE && !_isPaused.value) {
                val currentTime = System.currentTimeMillis()
                val remaining = endTime - currentTime
                if (remaining <= 0) {
                    _remainingTimeMs.value = 0L
                    break
                }
                _remainingTimeMs.value = remaining
                delay(500)
            }
        }
    }

    private fun getPhaseDurationMs(phase: PomodoroPhase): Long {
        val settings = currentSettings
        val minutes = when (phase) {
            PomodoroPhase.FOKUS -> settings?.focusDuration ?: 25
            PomodoroPhase.ISTIRAHAT_PENDEK -> settings?.shortBreakDuration ?: 5
            PomodoroPhase.ISTIRAHAT_PANJANG -> settings?.longBreakDuration ?: 15
            PomodoroPhase.IDLE -> settings?.focusDuration ?: 25
        }
        return minutes * 60 * 1000L
    }

    private fun sendServiceIntent(action: String, targetTimeMs: Long = 0L, isFocusPhase: Boolean = true) {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            this.action = action
            putExtra(PomodoroService.EXTRA_TARGET_TIME_MS, targetTimeMs)
            putExtra(PomodoroService.EXTRA_IS_FOCUS_PHASE, isFocusPhase)
            putExtra("FROM_UI", true)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun startSession() {
        if (_currentPhase.value == PomodoroPhase.IDLE) {
            _currentPhase.value = PomodoroPhase.FOKUS
        }

        if (_isPaused.value) {
            resumeSession()
            return
        }

        countdownJob?.cancel()

        val durationMs = getPhaseDurationMs(_currentPhase.value)
        val endTime = System.currentTimeMillis() + durationMs

        _isPaused.value = false
        _remainingTimeMs.value = durationMs

        prefs.edit()
            .putString("current_phase", _currentPhase.value.name)
            .putBoolean("is_paused", false)
            .putLong("focus_target_time_ms", endTime)
            .putLong("remaining_time_when_paused", 0L)
            .putLong("remaining_pause_ms", 0L)
            .putInt("current_cycle", _currentCycle.value)
            .apply()

        startCountdown(endTime)
        sendServiceIntent(PomodoroService.ACTION_START, endTime, _currentPhase.value == PomodoroPhase.FOKUS)
    }

    fun pauseSession() {
        if (_currentPhase.value == PomodoroPhase.IDLE) return

        countdownJob?.cancel()

        _isPaused.value = true
        val remaining = _remainingTimeMs.value

        prefs.edit()
            .putString("current_phase", _currentPhase.value.name)
            .putBoolean("is_paused", true)
            .putLong("focus_target_time_ms", 0L)
            .putLong("remaining_time_when_paused", remaining)
            .putLong("remaining_pause_ms", remaining)
            .putInt("current_cycle", _currentCycle.value)
            .apply()

        sendServiceIntent(PomodoroService.ACTION_PAUSE)
    }

    fun resumeSession() {
        if (_currentPhase.value == PomodoroPhase.IDLE || !_isPaused.value) return

        countdownJob?.cancel()

        val durationMs = _remainingTimeMs.value
        val endTime = System.currentTimeMillis() + durationMs

        _isPaused.value = false

        prefs.edit()
            .putString("current_phase", _currentPhase.value.name)
            .putBoolean("is_paused", false)
            .putLong("focus_target_time_ms", endTime)
            .putLong("remaining_time_when_paused", 0L)
            .putLong("remaining_pause_ms", 0L)
            .putInt("current_cycle", _currentCycle.value)
            .apply()

        startCountdown(endTime)
        sendServiceIntent(PomodoroService.ACTION_RESUME, endTime, _currentPhase.value == PomodoroPhase.FOKUS)
    }

    fun skipPhase() {
        sendServiceIntent("com.hekalabs.antidumbscroll.ACTION_POMODORO_SKIP_PHASE")
    }

    fun endSession() {
        countdownJob?.cancel()

        _currentPhase.value = PomodoroPhase.IDLE
        _isPaused.value = false
        _currentCycle.value = 1
        _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)

        prefs.edit()
            .putString("current_phase", PomodoroPhase.IDLE.name)
            .putBoolean("is_paused", false)
            .putLong("focus_target_time_ms", 0L)
            .putLong("remaining_time_when_paused", 0L)
            .putLong("remaining_pause_ms", 0L)
            .putInt("current_cycle", 1)
            .apply()

        sendServiceIntent(PomodoroService.ACTION_END)
    }
}
