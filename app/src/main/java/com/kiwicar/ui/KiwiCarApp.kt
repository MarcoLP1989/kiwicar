package com.kiwicar.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kiwicar.ui.charts.ChartsScreen
import com.kiwicar.ui.components.BottomNav
import com.kiwicar.ui.entry.AddEntryScreen
import com.kiwicar.ui.history.HistoryScreen
import com.kiwicar.ui.home.HomeScreen
import com.kiwicar.ui.settings.SettingsScreen

@Composable
fun KiwiCarApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            BottomNav(currentRoute = currentRoute, onSelect = { route ->
                if (currentRoute == "add" || currentRoute == "edit/{id}") navController.popBackStack()
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        },
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(onAddClick = { navController.navigate("add") }) }
            composable("charts") { ChartsScreen(onEditEntry = { navController.navigate("edit/$it") }) }
            composable("history") { HistoryScreen(onEditEntry = { navController.navigate("edit/$it") }) }
            composable("settings") { SettingsScreen() }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                AddEntryScreen(
                    onSaved = { navController.popBackStack() },
                    editingId = entry.arguments?.getLong("id"),
                )
            }
            composable("add") { AddEntryScreen(onSaved = { navController.popBackStack() }) }
        }
    }
}

