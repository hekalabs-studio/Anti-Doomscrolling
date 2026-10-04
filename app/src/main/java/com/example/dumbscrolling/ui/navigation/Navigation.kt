package com.example.dumbscrolling.ui.navigation

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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.dumbscrolling.ui.screens.DashboardScreen
import com.example.dumbscrolling.ui.screens.FocusModeScreen
import com.example.dumbscrolling.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object FocusModeRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(DashboardRoute)
    val currentRoute = backStack.lastOrNull() ?: DashboardRoute

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
                    DashboardScreen()
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


