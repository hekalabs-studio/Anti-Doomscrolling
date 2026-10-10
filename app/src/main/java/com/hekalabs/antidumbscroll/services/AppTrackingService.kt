package com.hekalabs.antidumbscroll.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hekalabs.antidumbscroll.MainActivity
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.hekalabs.antidumbscroll.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.hekalabs.antidumbscroll.data.SettingsRepository

@AndroidEntryPoint
class AppTrackingService : AccessibilityService() {
    @Inject lateinit var settingsRepository: SettingsRepository
    private var activePackageName: String? = null
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var isScreenOn = true

    // Limits
    private var timeLimitMs = 15 * 60 * 1000L // default 15 mins
    private var usageStats = mutableMapOf<String, Long>()
    
    // Blacklisted apps that are never tracked
    private val blacklistedApps = setOf(
        "com.android.systemui",
        "android",
        "com.android.settings",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.messaging",
        "com.google.android.apps.messaging",
        "com.google.android.apps.maps",
        "com.hekalabs.antidumbscroll"
    )
    
    private var targetApps = setOf<String>()
    
    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var prefs: SharedPreferences
    private val sessionClosedFlags = mutableSetOf<String>()

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    removeOverlay()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    checkDailyReset()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("FocusModePrefs", MODE_PRIVATE)
        
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                targetApps = settings.monitoredApps
                timeLimitMs = settings.sessionLimitMinutes * 60 * 1000L
                updateTrackingNotification()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        
        checkDailyReset()
        
        // Restore usage stats
        val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
        targetApps.forEach { packageName ->
            val savedUsage = usagePrefs.getLong("usage_$packageName", 0L)
            if (savedUsage > 0) {
                usageStats[packageName] = savedUsage
            }
        }
        
        Log.d("AppTrackingService", "Service connected")
    }

    private fun updateTrackingNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val trackingChannel = android.app.NotificationChannel(
                PomodoroService.TRACKING_CHANNEL_ID,
                "App Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi pemantauan aplikasi aktif"
            }
            notificationManager.createNotificationChannel(trackingChannel)
        }

        if (targetApps.isEmpty()) {
            notificationManager.cancel(102)
            return
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, PomodoroService.TRACKING_CHANNEL_ID)
            .setContentTitle("DumbScrolling aktif")
            .setContentText("Memantau ${targetApps.size} aplikasi")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        notificationManager.notify(102, builder.build())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (blacklistedApps.contains(packageName)) return
            
            if (packageName != activePackageName) {
                Log.d("AppTrackingService", "Active app changed to: $packageName")
                activePackageName = packageName
                sessionClosedFlags.remove(packageName)
                handleAppChange(packageName)
            }
        }
    }
    
    private fun checkDailyReset() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString("last_recorded_date", "")

        if (lastDate != today) {
            val editor = prefs.edit()
            editor.putString("last_recorded_date", today)
            prefs.all.keys.filter { it.startsWith("usage_") || it.startsWith("continue_count_") }.forEach { key ->
                editor.remove(key)
            }
            editor.apply()

            val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
            val usageEditor = usagePrefs.edit()
            usagePrefs.all.keys.filter { it.startsWith("usage_") }.forEach { key ->
                usageEditor.remove(key)
            }
            usageEditor.apply()
            usageStats.clear()
        }
    }

    private fun handleAppChange(packageName: String) {
        checkDailyReset()
        trackingJob?.cancel()
        removeOverlay()

        if (targetApps.contains(packageName)) {
            val currentPhase = prefs.getString("current_phase", "IDLE")
            val isPaused = prefs.getBoolean("is_paused", false)
            
            if (currentPhase == "FOKUS" && !isPaused) {
                showFocusModeOverlay(packageName)
                return
            }

            trackingJob = scope.launch {
                var warned80Percent = false
                var tickCount = 0L
                while (isActive) {
                    if (!isScreenOn) {
                        delay(1000)
                        continue
                    }
                    delay(1000)
                    tickCount++
                    checkDailyReset()
                    val currentUsage = usageStats.getOrDefault(packageName, 0L) + 1000L
                    usageStats[packageName] = currentUsage
                    
                    // Persist to disk every 10 seconds instead of every second
                    if (tickCount % 10 == 0L) {
                        val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
                        usagePrefs.edit().putLong("usage_$packageName", currentUsage).apply()
                    }
                    
                    if (!sessionClosedFlags.contains(packageName) && currentUsage >= timeLimitMs) {
                        showOverlay(packageName)
                        trackingJob?.cancel()
                    } else if (!warned80Percent && !sessionClosedFlags.contains(packageName) && currentUsage >= timeLimitMs * 0.8) {
                        warned80Percent = true
                        val remainingMinutes = ((timeLimitMs - currentUsage) / 60000).coerceAtLeast(1)
                        val appName = getAppName(packageName)
                        android.widget.Toast.makeText(applicationContext, "Sisa $remainingMinutes menit di $appName", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }
    }

    private fun showFocusModeOverlay(packageName: String) {
        if (overlayView != null) return
        
        Log.d("AppTrackingService", "Showing focus mode overlay for $packageName")
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_blocker, null)
        
        val titleText = overlayView?.findViewById<TextView>(R.id.overlayTitle)
        val messageText = overlayView?.findViewById<TextView>(R.id.overlayMessage)
        val btnClose = overlayView?.findViewById<Button>(R.id.btnCloseOverlay)
        val timerText = overlayView?.findViewById<TextView>(R.id.overlayTimer)
        val btnSecondary = overlayView?.findViewById<Button>(R.id.btnSecondary)
        
        // Hide secondary elements
        btnSecondary?.visibility = View.GONE
        overlayView?.findViewById<View>(R.id.confirmEndLayout)?.visibility = View.GONE
        
        val appName = getAppName(packageName)
        titleText?.text = "Fokus sedang berjalan"
        messageText?.text = "Sisa waktu:"
        
        timerText?.visibility = View.VISIBLE
        btnClose?.visibility = View.VISIBLE
        btnClose?.text = "Tutup $appName"
        
        btnClose?.setOnClickListener {
            val success = this@AppTrackingService.performGlobalAction(GLOBAL_ACTION_HOME)
            removeOverlay()
            if (!success) {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(homeIntent)
                } catch (e: Exception) {
                    Log.e("AppTrackingService", "Failed to start home intent", e)
                }
            }
            scope.launch(Dispatchers.IO) {
                delay(1000)
                try {
                    val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(packageName)
                } catch (e: Exception) {
                    Log.e("AppTrackingService", "Failed to kill background process", e)
                }
            }
        }
        
        overlayView?.setOnTouchListener { _, _ -> true }
        overlayView?.isFocusableInTouchMode = true
        overlayView?.requestFocus()
        overlayView?.setOnKeyListener { _, keyCode, _ ->
            keyCode == android.view.KeyEvent.KEYCODE_BACK
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e("AppTrackingService", "Failed to add focus overlay", e)
        }

        val focusTargetTimeMs = prefs.getLong("focus_target_time_ms", 0L)

        trackingJob = scope.launch {
            while (isActive) {
                val currentPhase = prefs.getString("current_phase", "IDLE")
                val isPaused = prefs.getBoolean("is_paused", false)
                
                if (currentPhase != "FOKUS" || isPaused) {
                    removeOverlay()
                    handleAppChange(packageName)
                    break
                }
                
                val currentTime = System.currentTimeMillis()
                val remainingMs = focusTargetTimeMs - currentTime
                
                if (remainingMs <= 0) {
                    removeOverlay()
                    handleAppChange(packageName)
                    break
                }
                
                val minutes = remainingMs / 60000
                val seconds = (remainingMs % 60000) / 1000
                timerText?.text = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
                
                delay(1000)
            }
        }
    }

    private fun showOverlay(packageName: String) {
        if (overlayView != null) return
        
        Log.d("AppTrackingService", "Showing overlay for $packageName")
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_blocker, null)
        
        val appName = getAppName(packageName)
        val usageMs = usageStats.getOrDefault(packageName, 0L)
        val usageMinutes = usageMs / 60000
        val usageSeconds = (usageMs % 60000) / 1000
        val limitMinutes = timeLimitMs / 60000
        
        val appIconView = overlayView?.findViewById<android.widget.ImageView>(R.id.overlayAppIcon)
        val titleText = overlayView?.findViewById<TextView>(R.id.overlayTitle)
        val messageText = overlayView?.findViewById<TextView>(R.id.overlayMessage)
        val limitText = overlayView?.findViewById<TextView>(R.id.overlaySessionLimit)
        val totalText = overlayView?.findViewById<TextView>(R.id.overlayTotalToday)
        val continueText = overlayView?.findViewById<TextView>(R.id.overlayContinueCount)
        val btnClose = overlayView?.findViewById<Button>(R.id.btnCloseOverlay)
        val btnSecondary = overlayView?.findViewById<Button>(R.id.btnSecondary)
        
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            appIconView?.setImageDrawable(icon)
            appIconView?.visibility = View.VISIBLE
        } catch (e: Exception) {
            // ignore
        }
        
        val timeString = "${usageMinutes}m ${usageSeconds}s"
        titleText?.text = getString(R.string.overlay_title)
        messageText?.text = getString(R.string.overlay_time_spent, timeString, appName)
        
        limitText?.text = getString(R.string.overlay_session_limit, limitMinutes)
        limitText?.visibility = View.VISIBLE
        
        totalText?.text = getString(R.string.overlay_total_today, usageMinutes)
        totalText?.visibility = View.VISIBLE
        
        val continueCount = prefs.getInt("continue_count_$packageName", 0)
        if (continueCount > 0) {
            continueText?.text = getString(R.string.overlay_continue_count, continueCount)
            continueText?.visibility = View.VISIBLE
        }
        
        btnClose?.text = getString(R.string.overlay_close_app_btn, appName)
        btnClose?.setOnClickListener {
            val success = this@AppTrackingService.performGlobalAction(GLOBAL_ACTION_HOME)
            sessionClosedFlags.add(packageName)
            removeOverlay()

            if (!success) {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(homeIntent)
                } catch (e: Exception) {
                    Log.e("AppTrackingService", "Failed to start home intent", e)
                }
            }

            scope.launch(Dispatchers.IO) {
                delay(1000)
                try {
                    val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(packageName)
                } catch (e: Exception) {
                    Log.e("AppTrackingService", "Failed to kill background process", e)
                }
            }
        }
        
        btnSecondary?.visibility = View.VISIBLE
        btnSecondary?.isEnabled = false
        val originalText = getString(R.string.overlay_button_extend)
        var waitTimeSec = 5 + (continueCount * 5)
        btnSecondary?.text = getString(R.string.overlay_wait_seconds, waitTimeSec)

        scope.launch {
            while (waitTimeSec > 0) {
                delay(1000)
                waitTimeSec--
                if (overlayView == null) break
                if (waitTimeSec > 0) {
                    btnSecondary?.text = getString(R.string.overlay_wait_seconds, waitTimeSec)
                } else {
                    btnSecondary?.text = originalText
                    btnSecondary?.isEnabled = true
                }
            }
        }

        btnSecondary?.setOnClickListener {
            prefs.edit().putInt("continue_count_$packageName", continueCount + 1).apply()
            removeOverlay()
            startGracePeriod(packageName)
        }

        overlayView?.setOnTouchListener { _, _ -> true }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e("AppTrackingService", "Failed to add overlay", e)
        }
    }
    
    private fun startGracePeriod(packageName: String) {
        val gracePeriodMs = 30_000L
        var remainingMs = gracePeriodMs
        
        trackingJob = scope.launch {
            while (remainingMs > 0 && isActive) {
                if (!isScreenOn) {
                    delay(1000)
                    continue
                }
                if (remainingMs % 10000L == 0L) {
                    val appName = getAppName(packageName)
                    android.widget.Toast.makeText(applicationContext, "Sisa ${remainingMs / 1000} detik di $appName", android.widget.Toast.LENGTH_SHORT).show()
                }
                delay(1000)
                remainingMs -= 1000
                
                val currentUsage = usageStats.getOrDefault(packageName, 0L) + 1000L
                usageStats[packageName] = currentUsage
                // Persist to disk every 10 seconds
                if (remainingMs % 10000L == 0L) {
                    val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
                    usagePrefs.edit().putLong("usage_$packageName", currentUsage).apply()
                }
            }
            if (isActive) {
                showOverlay(packageName)
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e("AppTrackingService", "Failed to remove overlay", e)
            }
            overlayView = null
        }
    }

    override fun onInterrupt() {}
    
    override fun onDestroy() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            Log.e("AppTrackingService", "Failed to unregister receiver", e)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(102)
        trackingJob?.cancel()
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }
}
