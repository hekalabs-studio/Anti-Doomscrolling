package com.example.dumbscrolling.services

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
import com.example.dumbscrolling.MainActivity

class PomodoroService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val targetTimeMs = intent.getLongExtra(EXTRA_TARGET_TIME_MS, 0L)
                val isFocusPhase = intent.getBooleanExtra(EXTRA_IS_FOCUS_PHASE, true)
                startForegroundService(targetTimeMs, isFocusPhase)
            }
            ACTION_PAUSE -> {
                // Handle pause logic
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_END -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PHASE_CHANGED -> {
                playPhaseChangeFeedback()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(targetTimeMs: Long, isFocusPhase: Boolean) {
        val notification = createNotification(targetTimeMs, isFocusPhase)
        
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
    }

    private fun createNotification(targetTimeMs: Long, isFocusPhase: Boolean): Notification {
        val title = if (isFocusPhase) "Fokus Pomodoro" else "Istirahat Pomodoro"

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_END
        }
        val endPendingIntent = PendingIntent.getService(
            this, 2, endIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, POMODORO_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Waktu tersisa")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: use real app icon
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Jeda", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Akhiri", endPendingIntent)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(targetTimeMs)
            .setShowWhen(true)
        
        builder.setChronometerCountDown(true)

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

        const val ACTION_START = "com.example.dumbscrolling.ACTION_POMODORO_START"
        const val ACTION_PAUSE = "com.example.dumbscrolling.ACTION_POMODORO_PAUSE"
        const val ACTION_END = "com.example.dumbscrolling.ACTION_POMODORO_END"
        const val ACTION_PHASE_CHANGED = "com.example.dumbscrolling.ACTION_POMODORO_PHASE_CHANGED"

        const val EXTRA_TARGET_TIME_MS = "extra_target_time_ms"
        const val EXTRA_IS_FOCUS_PHASE = "extra_is_focus_phase"
    }
}