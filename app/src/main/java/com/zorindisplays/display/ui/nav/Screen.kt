package com.zorindisplays.display.ui.nav

sealed class Screen {
    data object Idle : Screen()
    data object Game : Screen()
    data object Settings : Screen()
}