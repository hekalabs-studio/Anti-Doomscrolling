package com.example.dumbscrolling.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PomodoroPhase {
    IDLE, FOKUS, ISTIRAHAT_PENDEK, ISTIRAHAT_PANJANG
}

class FocusModeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("FocusModePrefs", Context.MODE_PRIVATE)
    private val settingsPrefs = application.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    
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
    
    val totalCycles: Int
        get() = settingsPrefs.getInt("long_break_cycle", 4)

    init {
        checkStatus()
    }

    private fun checkStatus() {
        if (_currentPhase.value == PomodoroPhase.IDLE) {
            _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)
            return
        }

        if (_isPaused.value) {
            _remainingTimeMs.value = prefs.getLong("remaining_time_when_paused", 0L)
        } else {
            val endTime = prefs.getLong("focus_target_time_ms", 0L)
            val currentTime = System.currentTimeMillis()
            
            if (endTime > currentTime) {
                startCountdown(endTime)
            } else if (endTime > 0) {
                completePhase()
            }
        }
    }

    private fun startCountdown(endTime: Long) {
        viewModelScope.launch {
            while (_currentPhase.value != PomodoroPhase.IDLE && !_isPaused.value) {
                val currentTime = System.currentTimeMillis()
                val remaining = endTime - currentTime
                if (remaining <= 0) {
                    completePhase()
                    break
                }
                _remainingTimeMs.value = remaining
                delay(100) // check more frequently for smooth UI
            }
        }
    }

    private fun getPhaseDurationMs(phase: PomodoroPhase): Long {
        val minutes = when (phase) {
            PomodoroPhase.FOKUS -> settingsPrefs.getInt("focus_duration", 25)
            PomodoroPhase.ISTIRAHAT_PENDEK -> settingsPrefs.getInt("short_break_duration", 5)
            PomodoroPhase.ISTIRAHAT_PANJANG -> settingsPrefs.getInt("long_break_duration", 15)
            PomodoroPhase.IDLE -> settingsPrefs.getInt("focus_duration", 25)
        }
        return minutes * 60 * 1000L
    }

    fun startSession() {
        if (_currentPhase.value == PomodoroPhase.IDLE) {
            setPhase(PomodoroPhase.FOKUS)
        }
        
        val durationMs = if (_isPaused.value) {
            _remainingTimeMs.value
        } else {
            getPhaseDurationMs(_currentPhase.value)
        }
        
        _isPaused.value = false
        prefs.edit().putBoolean("is_paused", false).apply()
        
        val endTime = System.currentTimeMillis() + durationMs
        prefs.edit().putLong("focus_target_time_ms", endTime).apply()
        
        startCountdown(endTime)
    }

    fun pauseSession() {
        if (_currentPhase.value == PomodoroPhase.IDLE) return
        
        _isPaused.value = true
        prefs.edit().putBoolean("is_paused", true).apply()
        prefs.edit().putLong("remaining_time_when_paused", _remainingTimeMs.value).apply()
    }

    fun skipPhase() {
        completePhase()
    }

    fun endSession() {
        setPhase(PomodoroPhase.IDLE)
        _isPaused.value = false
        _currentCycle.value = 1
        
        prefs.edit()
            .putLong("focus_target_time_ms", 0L)
            .putBoolean("is_paused", false)
            .putInt("current_cycle", 1)
            .apply()
            
        _remainingTimeMs.value = getPhaseDurationMs(PomodoroPhase.FOKUS)
    }

    private fun completePhase() {
        if (_currentPhase.value == PomodoroPhase.FOKUS) {
            val newCompleted = _completedSessionsToday.value + 1
            _completedSessionsToday.value = newCompleted
            prefs.edit().putInt("completed_sessions_today", newCompleted).apply()
        }

        val nextPhase = when (_currentPhase.value) {
            PomodoroPhase.FOKUS -> {
                if (_currentCycle.value >= totalCycles) {
                    PomodoroPhase.ISTIRAHAT_PANJANG
                } else {
                    PomodoroPhase.ISTIRAHAT_PENDEK
                }
            }
            PomodoroPhase.ISTIRAHAT_PENDEK -> {
                _currentCycle.value += 1
                prefs.edit().putInt("current_cycle", _currentCycle.value).apply()
                PomodoroPhase.FOKUS
            }
            PomodoroPhase.ISTIRAHAT_PANJANG -> {
                _currentCycle.value = 1
                prefs.edit().putInt("current_cycle", 1).apply()
                PomodoroPhase.FOKUS
            }
            PomodoroPhase.IDLE -> PomodoroPhase.FOKUS
        }

        setPhase(nextPhase)
        
        val autoStart = settingsPrefs.getBoolean("auto_start_next_phase", false)
        if (autoStart) {
            val durationMs = getPhaseDurationMs(nextPhase)
            val endTime = System.currentTimeMillis() + durationMs
            prefs.edit().putLong("focus_target_time_ms", endTime).apply()
            startCountdown(endTime)
        } else {
            _isPaused.value = true
            _remainingTimeMs.value = getPhaseDurationMs(nextPhase)
            prefs.edit()
                .putBoolean("is_paused", true)
                .putLong("remaining_time_when_paused", _remainingTimeMs.value)
                .apply()
        }
    }

    private fun setPhase(phase: PomodoroPhase) {
        _currentPhase.value = phase
        prefs.edit().putString("current_phase", phase.name).apply()
    }
}
