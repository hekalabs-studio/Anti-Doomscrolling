package com.example.dumbscrolling.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dumbscrolling.R

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                        text = "App Usage",
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
                    text = "Achievements",
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
    val seconds = (totalTimeMs / 1000) % 60
    val minutes = (totalTimeMs / (1000 * 60)) % 60
    val hours = (totalTimeMs / (1000 * 60 * 60))
    val timeString = if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format(java.util.Locale.getDefault(), "%dm %02ds", minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%ds", seconds)
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
                text = "Tracked Screen Time",
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
    val limitMinutes = appUsage.limitMs / 60000
    val usageMinutes = appUsage.usageTimeMs / 60000
    val remainingMinutes = (limitMinutes - usageMinutes).coerceAtLeast(0)

    val seconds = (appUsage.usageTimeMs / 1000) % 60
    val minutes = (appUsage.usageTimeMs / (1000 * 60)) % 60
    val hours = (appUsage.usageTimeMs / (1000 * 60 * 60))
    val timeString = if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format(java.util.Locale.getDefault(), "%dm %02ds", minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%ds", seconds)
    }

    val appName = appUsage.packageName.split(".").last().replaceFirstChar { it.uppercase() }

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
            text = "$remainingMinutes dari $limitMinutes menit tersisa",
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
