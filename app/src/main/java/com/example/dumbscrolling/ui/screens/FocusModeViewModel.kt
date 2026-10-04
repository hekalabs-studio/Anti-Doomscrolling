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

class FocusModeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("FocusModePrefs", Context.MODE_PRIVATE)
    
    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _isFocusModeActive = MutableStateFlow(false)
    val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()

    private val _completedSessions = MutableStateFlow(prefs.getInt("completed_sessions", 0))
    val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()
    
    init {
        checkFocusModeStatus()
    }

    private fun checkFocusModeStatus() {
        val endTime = prefs.getLong("focus_mode_end_time_ms", 0L)
        val currentTime = System.currentTimeMillis()
        
        if (endTime > currentTime) {
            _isFocusModeActive.value = true
            startCountdown(endTime)
        } else if (endTime > 0) {
            // It finished while app was closed
            completeSession()
        }
    }

    private fun startCountdown(endTime: Long) {
        viewModelScope.launch {
            while (_isFocusModeActive.value) {
                val currentTime = System.currentTimeMillis()
                val remaining = endTime - currentTime
                if (remaining <= 0) {
                    completeSession()
                    break
                }
                _remainingTimeMs.value = remaining
                delay(1000)
            }
        }
    }

    fun startFocusMode(durationMinutes: Int) {
        val durationMs = durationMinutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMs
        prefs.edit().putLong("focus_mode_end_time_ms", endTime).apply()
        _isFocusModeActive.value = true
        startCountdown(endTime)
    }

    fun stopFocusMode() {
        prefs.edit().putLong("focus_mode_end_time_ms", 0L).apply()
        _isFocusModeActive.value = false
        _remainingTimeMs.value = 0L
        // We do not increment completed sessions if stopped manually
    }

    private fun completeSession() {
        prefs.edit().putLong("focus_mode_end_time_ms", 0L).apply()
        
        val newCount = _completedSessions.value + 1
        prefs.edit().putInt("completed_sessions", newCount).apply()
        
        _completedSessions.value = newCount
        _isFocusModeActive.value = false
        _remainingTimeMs.value = 0L
    }
}
