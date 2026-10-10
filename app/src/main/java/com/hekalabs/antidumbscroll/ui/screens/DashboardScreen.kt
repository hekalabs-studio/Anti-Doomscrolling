package com.hekalabs.antidumbscroll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import android.content.Intent
import android.provider.Settings
import com.hekalabs.antidumbscroll.R
import com.hekalabs.antidumbscroll.util.isAccessibilityServiceEnabled

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    focusModeViewModel: FocusModeViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    var isAccessibilityEnabled by remember { 
        mutableStateOf(isAccessibilityServiceEnabled(context)) 
    }
    
    val currentPhase by focusModeViewModel.currentPhase.collectAsState()
    val remainingTimeMs by focusModeViewModel.remainingTimeMs.collectAsState()
    val isPaused by focusModeViewModel.isPaused.collectAsState()
    var showEndSessionDialog by remember { mutableStateOf(false) }
    var showNoAppsDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showEndSessionDialog) {
        AlertDialog(
            onDismissRequest = { showEndSessionDialog = false },
            title = { Text("Akhiri Sesi?") },
            text = { Text("Sesi fokus akan dihentikan dan progres saat ini akan hilang.") },
            confirmButton = {
                TextButton(onClick = {
                    focusModeViewModel.endSession()
                    showEndSessionDialog = false
                }) {
                    Text("Ya, Akhiri")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showNoAppsDialog) {
        AlertDialog(
            onDismissRequest = { showNoAppsDialog = false },
            title = { Text(stringResource(R.string.nav_focus)) },
            text = { Text(stringResource(R.string.no_monitored_apps_focus)) },
            confirmButton = {
                TextButton(onClick = {
                    showNoAppsDialog = false
                    focusModeViewModel.startSession()
                }) {
                    Text(stringResource(R.string.continue_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNoAppsDialog = false
                    onNavigateToSettings()
                }) {
                    Text(stringResource(R.string.add_apps))
                }
            }
        )
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text(stringResource(R.string.nav_focus)) },
            text = { Text(stringResource(R.string.accessibility_required)) },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityDialog = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text(stringResource(R.string.enable_service))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (!isAccessibilityEnabled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.monitoring_inactive_warning),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.enable_btn), color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.nav_dashboard),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentPhase == PomodoroPhase.IDLE) {
                            Text(
                                text = stringResource(R.string.start_focus),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.focus_summary, uiState.focusDuration, uiState.shortBreakDuration),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                if (!isAccessibilityEnabled) {
                                    showAccessibilityDialog = true
                                } else if (uiState.isMonitoredAppsEmpty) {
                                    showNoAppsDialog = true
                                } else {
                                    focusModeViewModel.startSession()
                                }
                            }) {
                                Text(stringResource(R.string.start_focus))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onNavigateToSettings) {
                                Text(
                                    text = stringResource(R.string.change_in_settings),
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        } else {
                            val phaseText = when (currentPhase) {
                                PomodoroPhase.FOKUS -> stringResource(R.string.phase_fokus)
                                PomodoroPhase.ISTIRAHAT_PENDEK -> stringResource(R.string.phase_istirahat_pendek)
                                PomodoroPhase.ISTIRAHAT_PANJANG -> stringResource(R.string.phase_istirahat_panjang)
                                else -> ""
                            }
                            Text(
                                text = phaseText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalSeconds = (remainingTimeMs / 1000).coerceAtLeast(0)
                            val m = totalSeconds / 60
                            val s = totalSeconds % 60
                            Text(
                                text = String.format(locale, "%02d:%02d", m, s),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isPaused) {
                                    Button(onClick = { focusModeViewModel.startSession() }) {
                                        Text(stringResource(R.string.continue_btn))
                                    }
                                } else {
                                    Button(onClick = { focusModeViewModel.pauseSession() }) {
                                        Text(stringResource(R.string.pause_btn))
                                    }
                                }
                                Button(
                                    onClick = { showEndSessionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(stringResource(R.string.end_btn))
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                WeeklyStatsCard(
                    weeklyStats = uiState.weeklyStats,
                    dayLabels = uiState.weeklyDayLabels
                )
            }

            item {
                TotalUsageCard(totalTimeMs = uiState.totalScreenTimeMs)
            }

            if (uiState.isMonitoredAppsEmpty) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.empty_monitored_apps_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = onNavigateToSettings) {
                            Text(stringResource(R.string.add_app_btn))
                        }
                    }
                }
            } else if (uiState.appUsages.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.app_usage_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(uiState.appUsages) { appUsage ->
                    AppUsageItem(appUsage)
                }
            }

            item {
                Text(
                    text = stringResource(R.string.achievements_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            items(uiState.achievements) { achievement ->
                AchievementItem(achievement)
            }
        }
    }
}

@Composable
fun TotalUsageCard(totalTimeMs: Long) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    val seconds = (totalTimeMs / 1000) % 60
    val minutes = (totalTimeMs / (1000 * 60)) % 60
    val hours = (totalTimeMs / (1000 * 60 * 60))
    val timeString = if (hours > 0) {
        String.format(locale, "%dh %02dm %02ds", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format(locale, "%dm %02ds", minutes, seconds)
    } else {
        String.format(locale, "%ds", seconds)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.tracked_screen_time),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppUsageItem(appUsage: AppUsage) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    val limitMinutes = appUsage.limitMs / 60000
    val usageMinutes = appUsage.usageTimeMs / 60000
    val remainingMinutes = (limitMinutes - usageMinutes).coerceAtLeast(0)

    val seconds = (appUsage.usageTimeMs / 1000) % 60
    val minutes = (appUsage.usageTimeMs / (1000 * 60)) % 60
    val hours = (appUsage.usageTimeMs / (1000 * 60 * 60))
    val timeString = if (hours > 0) {
        String.format(locale, "%dh %02dm %02ds", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format(locale, "%dm %02ds", minutes, seconds)
    } else {
        String.format(locale, "%ds", seconds)
    }

    val context = LocalContext.current
    val appName = remember(appUsage.packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(appUsage.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            appUsage.packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stringResource(R.string.usage_remaining_text, remainingMinutes, limitMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = if (remainingMinutes == 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { (appUsage.usageTimeMs.toFloat() / appUsage.limitMs.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (remainingMinutes == 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val icon: ImageVector = when (achievement.iconResName) {
        "Star" -> Icons.Filled.Star
        "Warning" -> Icons.Filled.Warning
        "Analytics" -> Icons.Filled.Analytics
        else -> Icons.Filled.Star
    }

    val backgroundColor = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun WeeklyStatsCard(weeklyStats: List<Float>, dayLabels: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.weekly_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = weeklyStats.maxOrNull()?.coerceAtLeast(30f) ?: 30f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val count = maxOf(weeklyStats.size, dayLabels.size, 7)
                for (index in 0 until count) {
                    val value = weeklyStats.getOrElse(index) { 0f }
                    val label = dayLabels.getOrElse(index) { "" }

                    val durationText = if (value > 0f) {
                        val mins = value.toInt()
                        val h = mins / 60
                        val m = mins % 60
                        if (h > 0) {
                            if (m > 0) "${h}h ${m}m" else "${h}h"
                        } else {
                            "${m}m"
                        }
                    } else {
                        ""
                    }

                    val isToday = (index == count - 1)
                    val activeBarColor = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Teks Durasi Atas
                        Box(
                            modifier = Modifier.height(20.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (durationText.isNotEmpty()) {
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tampilan Batang (Chart Bar)
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(androidx.compose.ui.graphics.Color(0x20000000)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barHeightFraction = (value / maxVal).coerceIn(0f, 1f)
                            if (barHeightFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(barHeightFraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(activeBarColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Label Nama Hari Bawah
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}