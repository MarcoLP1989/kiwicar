package com.kiwicar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kiwicar.ui.charts.ChartsScreen
import com.kiwicar.ui.components.BottomNav
import com.kiwicar.ui.entry.AddEntryScreen
import com.kiwicar.ui.home.HomeScreen

@Composable
fun KiwiApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            BottomNav(currentRoute = currentRoute, onSelect = { route ->
                if (currentRoute == "add") navController.popBackStack()
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
            composable("charts") { ChartsScreen(onEditEntry = { /* edit route added in Task 10 */ }) }
            composable("history") { Placeholder("Storico") }
            composable("settings") { Placeholder("Impostazioni") }
            composable("add") { AddEntryScreen(onSaved = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(title) }
}
