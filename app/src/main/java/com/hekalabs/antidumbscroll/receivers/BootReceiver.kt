package com.hekalabs.antidumbscroll.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.hekalabs.antidumbscroll.services.PomodoroService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    @Named("FocusPrefs")
    lateinit var focusPrefs: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val currentPhase = focusPrefs.getString("current_phase", "IDLE") ?: "IDLE"
            val isPaused = focusPrefs.getBoolean("is_paused", false)
            val focusTargetTimeMs = focusPrefs.getLong("focus_target_time_ms", 0L)
            
            if (currentPhase != "IDLE" && !isPaused && focusTargetTimeMs > System.currentTimeMillis()) {
                val serviceIntent = Intent(context, PomodoroService::class.java).apply {
                    action = PomodoroService.ACTION_START
                    putExtra(PomodoroService.EXTRA_TARGET_TIME_MS, focusTargetTimeMs)
                    putExtra(PomodoroService.EXTRA_IS_FOCUS_PHASE, currentPhase == "FOKUS")
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
