package com.example.empire.ui

sealed class Screen(val route: String) {
    object Start     : Screen("start")
    object SelectSave: Screen("select_save")
    object Gameplay  : Screen("gameplay")
    object Settings  : Screen("settings")
}
