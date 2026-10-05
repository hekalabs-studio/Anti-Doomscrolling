package com.example.dumbscrolling.ui.navigation

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.dumbscrolling.ui.screens.DashboardScreen
import com.example.dumbscrolling.ui.screens.FocusModeScreen
import com.example.dumbscrolling.ui.screens.OnboardingScreen
import com.example.dumbscrolling.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object FocusModeRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object OnboardingRoute : NavKey

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val isFirstTime = prefs.getBoolean("is_first_time", true)
    
    val backStack = rememberNavBackStack(if (isFirstTime) OnboardingRoute else DashboardRoute)
    val currentRoute = backStack.lastOrNull() ?: DashboardRoute

    if (currentRoute == OnboardingRoute) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(onFinish = {
                        prefs.edit().putBoolean("is_first_time", false).apply()
                        backStack.clear()
                        backStack.add(DashboardRoute)
                    })
                }
            }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = currentRoute == DashboardRoute,
                    onClick = {
                        if (currentRoute != DashboardRoute) {
                            backStack.clear()
                            backStack.add(DashboardRoute)
                        }
                    },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                item(
                    selected = currentRoute == FocusModeRoute,
                    onClick = {
                        if (currentRoute != FocusModeRoute) {
                            backStack.clear()
                            backStack.add(DashboardRoute)
                            backStack.add(FocusModeRoute)
                        }
                    },
                    icon = { Icon(Icons.Filled.SelfImprovement, contentDescription = "Focus Mode") },
                    label = { Text("Focus") }
                )
                item(
                    selected = currentRoute == SettingsRoute,
                    onClick = {
                        if (currentRoute != SettingsRoute) {
                            backStack.clear()
                            backStack.add(DashboardRoute)
                            backStack.add(SettingsRoute)
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<DashboardRoute> {
                        DashboardScreen(
                            onNavigateToSettings = {
                                if (currentRoute != SettingsRoute) {
                                    backStack.clear()
                                    backStack.add(DashboardRoute)
                                    backStack.add(SettingsRoute)
                                }
                            }
                        )
                    }
                    entry<FocusModeRoute> {
                        FocusModeScreen()
                    }
                    entry<SettingsRoute> {
                        SettingsScreen()
                    }
                }
            )
        }
    }
}


