package com.zorindisplays.display.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Router(start: Screen = Screen.Idle) {
    var screen: Screen by mutableStateOf(start)
        private set

    fun go(to: Screen) {
        screen = to
    }

    fun backToGame() {
        screen = Screen.Game
    }

    fun backToIdle() {
        screen = Screen.Idle
    }
}
