package com.example.empire.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.empire.ui.start.StartScreen
import com.example.empire.ui.load.SelectSaveScreen
import com.example.empire.ui.settings.SettingsScreen
import com.example.empire.ui.gameplay.GameplayScreen  // our gameplay wrapper

@Composable
fun AppNav(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Start.route) {
        composable(Screen.Start.route)      { StartScreen(navController) }
        composable(Screen.SelectSave.route) { SelectSaveScreen(navController) }
        composable(Screen.Settings.route)   { SettingsScreen(navController) }
        composable(Screen.Gameplay.route)   { GameplayScreen(navController) } // gameplay wrapper
    }
}
