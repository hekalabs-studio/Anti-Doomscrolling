package com.example.dumbscrolling.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import com.example.dumbscrolling.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppTrackingService : AccessibilityService() {
    private var activePackageName: String? = null
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager

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
        "com.example.dumbscrolling"
    )
    
    private var targetApps = setOf<String>()
    
    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var prefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("FocusModePrefs", MODE_PRIVATE)
        settingsPrefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        
        loadSettings()
        
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
    
    private fun loadSettings() {
        // Load target apps
        val defaultApps = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.twitter.android"
        )
        val monitored = mutableSetOf<String>()
        for (app in defaultApps) {
            if (settingsPrefs.getBoolean("monitored_$app", true)) {
                monitored.add(app)
            }
        }
        targetApps = monitored
        
        // Load time limit
        val limitMinutes = settingsPrefs.getInt("session_limit_minutes", 15)
        timeLimitMs = limitMinutes * 60 * 1000L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Reload settings on each app change in case user changed them
            loadSettings()
            
            if (blacklistedApps.contains(packageName)) return
            
            if (packageName != activePackageName) {
                Log.d("AppTrackingService", "Active app changed to: $packageName")
                activePackageName = packageName
                handleAppChange(packageName)
            }
        }
    }
    
    private fun handleAppChange(packageName: String) {
        trackingJob?.cancel()
        removeOverlay()

        if (targetApps.contains(packageName)) {
            val focusModeEndTime = prefs.getLong("focus_mode_end_time_ms", 0L)
            if (System.currentTimeMillis() < focusModeEndTime) {
                // Focus mode is active, block immediately
                showFocusModeOverlay(packageName, focusModeEndTime)
                return
            }

            trackingJob = scope.launch {
                var warned80Percent = false
                while (isActive) {
                    delay(1000) // Count every second
                    val currentUsage = usageStats.getOrDefault(packageName, 0L) + 1000L
                    usageStats[packageName] = currentUsage
                    
                    // Save to SharedPreferences for the Dashboard
                    val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
                    usagePrefs.edit().putLong("usage_$packageName", currentUsage).apply()
                    
                    if (currentUsage >= timeLimitMs) {
                        showOverlay(packageName)
                        trackingJob?.cancel()
                    } else if (!warned80Percent && currentUsage >= timeLimitMs * 0.8) {
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

    private fun showFocusModeOverlay(packageName: String, focusModeEndTime: Long) {
        if (overlayView != null) return
        
        Log.d("AppTrackingService", "Showing focus mode overlay for $packageName")
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Removed FLAG_NOT_FOCUSABLE so it intercepts back gestures and doesn't disappear
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_blocker, null)
        
        val titleText = overlayView?.findViewById<TextView>(R.id.overlayTitle)
        val messageText = overlayView?.findViewById<TextView>(R.id.overlayMessage)
        val btnClose = overlayView?.findViewById<Button>(R.id.btnCloseOverlay)
        
        val timerText = overlayView?.findViewById<TextView>(R.id.overlayTimer)
        val confirmEndLayout = overlayView?.findViewById<android.widget.LinearLayout>(R.id.confirmEndLayout)
        val btnCancelEnd = overlayView?.findViewById<Button>(R.id.btnCancelEnd)
        val btnConfirmEnd = overlayView?.findViewById<Button>(R.id.btnConfirmEnd)
        
        titleText?.text = getString(R.string.focus_overlay_title)
        messageText?.text = getString(R.string.focus_overlay_message)
        
        // Show timer and End Session button
        timerText?.visibility = View.VISIBLE
        btnClose?.visibility = View.VISIBLE
        btnClose?.text = "Akhiri Sesi"
        
        btnClose?.setOnClickListener {
            btnClose.visibility = View.GONE
            confirmEndLayout?.visibility = View.VISIBLE
        }
        
        btnCancelEnd?.setOnClickListener {
            confirmEndLayout?.visibility = View.GONE
            btnClose?.visibility = View.VISIBLE
        }
        
        btnConfirmEnd?.setOnClickListener {
            // End session prematurely
            prefs.edit().putLong("focus_mode_end_time_ms", 0L).apply()
            removeOverlay()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        overlayView?.setOnTouchListener { _, _ -> true }
        
        // Handle back button to prevent it from going to the underlying app
        overlayView?.isFocusableInTouchMode = true
        overlayView?.requestFocus()
        overlayView?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                // Consume back press so it doesn't close the underlying app
                true
            } else {
                false
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e("AppTrackingService", "Failed to add focus overlay", e)
        }

        // Start a job to remove the overlay when focus mode ends and update timer
        trackingJob = scope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val remainingMs = focusModeEndTime - currentTime
                if (remainingMs <= 0) {
                    removeOverlay()
                    handleAppChange(packageName)
                    break
                }
                
                val minutes = remainingMs / 60000
                val seconds = (remainingMs % 60000) / 1000
                timerText?.text = String.format("%02d:%02d", minutes, seconds)
                
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
        val usageMinutes = (usageStats.getOrDefault(packageName, 0L) / 60000).coerceAtLeast(1)
        val limitMinutes = timeLimitMs / 60000
        
        val titleText = overlayView?.findViewById<TextView>(R.id.overlayTitle)
        val messageText = overlayView?.findViewById<TextView>(R.id.overlayMessage)
        val btnClose = overlayView?.findViewById<Button>(R.id.btnCloseOverlay)
        
        titleText?.text = "Time's Up!"
        messageText?.text = "Kamu sudah $usageMinutes menit di $appName (batas $limitMinutes menit)"
        
        // We will modify the button text later or use a custom layout.
        // For now, assume btnClose is the primary button, and we need to add a secondary button.
        btnClose?.text = "Ke Beranda"
        btnClose?.setOnClickListener {
            removeOverlay()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        
        // Add a secondary button programmatically if it doesn't exist in XML yet
        val container = btnClose?.parent as? android.widget.LinearLayout
        if (container != null) {
            val secondaryBtn = Button(this).apply {
                text = "Beri saya 30 detik"
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                setOnClickListener {
                    removeOverlay()
                    startGracePeriod(packageName)
                }
            }
            container.addView(secondaryBtn)
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
        
        // Let's do a countdown via coroutine
        trackingJob = scope.launch {
            while (remainingMs > 0 && isActive) {
                if (remainingMs % 10000L == 0L) {
                    val appName = getAppName(packageName)
                    android.widget.Toast.makeText(applicationContext, "Sisa ${remainingMs / 1000} detik di $appName", android.widget.Toast.LENGTH_SHORT).show()
                }
                delay(1000)
                remainingMs -= 1000
                
                // Keep updating usage
                val currentUsage = usageStats.getOrDefault(packageName, 0L) + 1000L
                usageStats[packageName] = currentUsage
                val usagePrefs = getSharedPreferences("UsageStatsPrefs", MODE_PRIVATE)
                usagePrefs.edit().putLong("usage_$packageName", currentUsage).apply()
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

    override fun onInterrupt() {
        // Leave empty or add non-dismissing logic
    }
    
    override fun onDestroy() {
        trackingJob?.cancel()
        removeOverlay()
        super.onDestroy()
    }
}