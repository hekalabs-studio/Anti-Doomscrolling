package com.hekalabs.antidumbscroll.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hekalabs.antidumbscroll.MainActivity
import com.hekalabs.antidumbscroll.widget.FocusWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import android.content.SharedPreferences

import com.hekalabs.antidumbscroll.data.SettingsRepository
import com.hekalabs.antidumbscroll.data.AppSettings

@AndroidEntryPoint
class PomodoroService : Service() {

    @Inject
    @Named("FocusPrefs")
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var currentSettings: AppSettings? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val targetTimeMs = intent.getLongExtra(EXTRA_TARGET_TIME_MS, 0L).let {
                    if (it > 0) it else prefs.getLong("focus_target_time_ms", 0L)
                }
                val isFocusPhase = intent.getBooleanExtra(
                    EXTRA_IS_FOCUS_PHASE,
                    prefs.getString("current_phase", "FOKUS") == "FOKUS"
                )
                startOrUpdateForeground(targetTimeMs, isFocusPhase, isPaused = false)
            }
            ACTION_PAUSE -> {
                val fromUI = intent.getBooleanExtra("FROM_UI", false)
                if (!fromUI) {
                    val targetTimeMs = prefs.getLong("focus_target_time_ms", 0L)
                    val remainingMs = if (targetTimeMs > 0L) {
                        maxOf(0L, targetTimeMs - System.currentTimeMillis())
                    } else {
                        prefs.getLong("remaining_time_when_paused", 0L)
                    }
                    prefs.edit()
                        .putBoolean("is_paused", true)
                        .putLong("remaining_pause_ms", remainingMs)
                        .putLong("remaining_time_when_paused", remainingMs)
                        .putLong("focus_target_time_ms", 0L)
                        .apply()
                }

                val remainingMs = prefs.getLong("remaining_time_when_paused", 0L)
                val isFocusPhase = prefs.getString("current_phase", "FOKUS") == "FOKUS"
                startOrUpdateForeground(0L, isFocusPhase, isPaused = true, remainingMs = remainingMs)
            }
            ACTION_RESUME -> {
                val fromUI = intent.getBooleanExtra("FROM_UI", false)
                if (!fromUI) {
                    val remainingMs = prefs.getLong("remaining_time_when_paused", prefs.getLong("remaining_pause_ms", 0L))
                    val targetTimeMs = System.currentTimeMillis() + remainingMs
                    prefs.edit()
                        .putBoolean("is_paused", false)
                        .putLong("focus_target_time_ms", targetTimeMs)
                        .putLong("remaining_time_when_paused", 0L)
                        .putLong("remaining_pause_ms", 0L)
                        .apply()
                }

                val targetTimeMs = prefs.getLong("focus_target_time_ms", 0L)
                val isFocusPhase = prefs.getString("current_phase", "FOKUS") == "FOKUS"
                startOrUpdateForeground(targetTimeMs, isFocusPhase, isPaused = false)
            }
            ACTION_END -> {
                prefs.edit()
                    .putString("current_phase", "IDLE")
                    .putLong("focus_target_time_ms", 0L)
                    .putBoolean("is_paused", false)
                    .apply()
                timerJob?.cancel()
                FocusWidgetProvider.updateWidget(this, "Siap", "--:--")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SKIP_PHASE -> {
                completePhase()
            }
            ACTION_PHASE_CHANGED -> {
                playPhaseChangeFeedback()
                val currentPhase = prefs.getString("current_phase", "IDLE") ?: "IDLE"
                if (currentPhase == "IDLE") {
                    timerJob?.cancel()
                    FocusWidgetProvider.updateWidget(this, "Siap", "--:--")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val isFocusPhase = currentPhase == "FOKUS"
                    val isPaused = prefs.getBoolean("is_paused", false)
                    if (isPaused) {
                        val remainingMs = prefs.getLong("remaining_time_when_paused", prefs.getLong("remaining_pause_ms", 0L))
                        startOrUpdateForeground(0L, isFocusPhase, isPaused = true, remainingMs = remainingMs)
                    } else {
                        var targetTimeMs = intent.getLongExtra(EXTRA_TARGET_TIME_MS, 0L)
                        if (targetTimeMs == 0L) {
                            targetTimeMs = prefs.getLong("focus_target_time_ms", 0L)
                        }
                        startOrUpdateForeground(targetTimeMs, isFocusPhase, isPaused = false)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startOrUpdateForeground(
        targetTimeMs: Long,
        isFocusPhase: Boolean,
        isPaused: Boolean,
        remainingMs: Long = 0L
    ) {
        val notification = createNotification(targetTimeMs, isFocusPhase, isPaused, remainingMs)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        updateWidgetState(targetTimeMs, isPaused, remainingMs)
    }

    private fun updateWidgetState(targetTimeMs: Long, isPaused: Boolean, remainingMs: Long) {
        timerJob?.cancel()
        val phase = prefs.getString("current_phase", "IDLE") ?: "IDLE"
        
        if (phase == "IDLE") {
            FocusWidgetProvider.updateWidget(this, "Siap", "--:--")
        } else if (isPaused) {
            val timeToDisplay = if (remainingMs > 0) remainingMs else {
                val rem = prefs.getLong("remaining_time_when_paused", 0L)
                if (rem > 0) rem else 0L
            }
            FocusWidgetProvider.updateWidget(this, "Di-jeda", formatTime(timeToDisplay))
        } else {
            timerJob = serviceScope.launch {
                while (true) {
                    val current = System.currentTimeMillis()
                    val rem = targetTimeMs - current
                    if (rem <= 0) {
                        FocusWidgetProvider.updateWidget(this@PomodoroService, "Siap", "--:--")
                        completePhase()
                        break
                    }
                    FocusWidgetProvider.updateWidget(this@PomodoroService, phase, formatTime(rem))
                    delay(1000)
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun completePhase() {
        val currentPhase = prefs.getString("current_phase", "IDLE") ?: "IDLE"
        var completedSessions = prefs.getInt("completed_sessions_today", 0)
        if (currentPhase == "FOKUS") {
            completedSessions++
        }
        
        val settings = currentSettings ?: return
        val totalCycles = settings.longBreakCycle
        var currentCycle = prefs.getInt("current_cycle", 1)
        
        val nextPhase = when (currentPhase) {
            "FOKUS" -> if (currentCycle >= totalCycles) "ISTIRAHAT_PANJANG" else "ISTIRAHAT_PENDEK"
            "ISTIRAHAT_PENDEK" -> { currentCycle++; "FOKUS" }
            "ISTIRAHAT_PANJANG" -> { currentCycle = 1; "FOKUS" }
            else -> "FOKUS"
        }
        
        if (nextPhase == "IDLE") {
            prefs.edit().putString("current_phase", "IDLE").putLong("focus_target_time_ms", 0L).putBoolean("is_paused", false).apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        
        val autoStart = settings.autoStartNextPhase
        val durationMs = getPhaseDurationMs(nextPhase, settings)
        val targetTime = if (autoStart) System.currentTimeMillis() + durationMs else 0L
        val isPaused = !autoStart
        
        prefs.edit()
            .putString("current_phase", nextPhase)
            .putBoolean("is_paused", isPaused)
            .putLong("focus_target_time_ms", targetTime)
            .putLong("remaining_time_when_paused", durationMs)
            .putLong("remaining_pause_ms", durationMs)
            .putInt("current_cycle", currentCycle)
            .putInt("completed_sessions_today", completedSessions)
            .apply()
            
        playPhaseChangeFeedback()
        val isFocus = nextPhase == "FOKUS"
        startOrUpdateForeground(targetTime, isFocus, isPaused, durationMs)
    }

    private fun getPhaseDurationMs(phase: String, settings: AppSettings): Long {
        val minutes = when (phase) {
            "FOKUS" -> settings.focusDuration
            "ISTIRAHAT_PENDEK" -> settings.shortBreakDuration
            "ISTIRAHAT_PANJANG" -> settings.longBreakDuration
            else -> settings.focusDuration
        }
        return minutes * 60 * 1000L
    }

    private fun createNotification(
        targetTimeMs: Long,
        isFocusPhase: Boolean,
        isPaused: Boolean,
        remainingMs: Long = 0L
    ): Notification {
        val title = if (isFocusPhase) "Fokus Pomodoro" else "Istirahat Pomodoro"

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_END
        }
        val endPendingIntent = PendingIntent.getService(
            this, 2, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, POMODORO_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)

        if (isPaused) {
            val resumeIntent = Intent(this, PomodoroService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 3, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.setContentText("Di-jeda")
                .setUsesChronometer(false)
                .setShowWhen(false)
                .addAction(android.R.drawable.ic_media_play, "Lanjut", resumePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Akhiri", endPendingIntent)
        } else {
            val pauseIntent = Intent(this, PomodoroService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.setContentText("Waktu tersisa")
                .setUsesChronometer(true)
                .setWhen(targetTimeMs)
                .setShowWhen(true)
                .setChronometerCountDown(true)
                .addAction(android.R.drawable.ic_media_pause, "Jeda", pausePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Akhiri", endPendingIntent)
        }

        return builder.build()
    }

    private fun playPhaseChangeFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pomodoroChannel = NotificationChannel(
                POMODORO_CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi hitung mundur Pomodoro"
            }

            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "App Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi pemantauan aplikasi aktif"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(pomodoroChannel)
            notificationManager?.createNotificationChannel(trackingChannel)
        }
    }

    companion object {
        const val POMODORO_CHANNEL_ID = "pomodoro_channel"
        const val TRACKING_CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "com.hekalabs.antidumbscroll.ACTION_POMODORO_START"
        const val ACTION_PAUSE = "com.hekalabs.antidumbscroll.ACTION_POMODORO_PAUSE"
        const val ACTION_RESUME = "com.hekalabs.antidumbscroll.ACTION_POMODORO_RESUME"
        const val ACTION_END = "com.hekalabs.antidumbscroll.ACTION_POMODORO_END"
        const val ACTION_PHASE_CHANGED = "com.hekalabs.antidumbscroll.ACTION_POMODORO_PHASE_CHANGED"
        const val ACTION_SKIP_PHASE = "com.hekalabs.antidumbscroll.ACTION_POMODORO_SKIP_PHASE"

        const val EXTRA_TARGET_TIME_MS = "extra_target_time_ms"
        const val EXTRA_IS_FOCUS_PHASE = "extra_is_focus_phase"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
