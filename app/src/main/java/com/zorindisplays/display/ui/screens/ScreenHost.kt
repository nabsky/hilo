package com.zorindisplays.display.ui.screens

import androidx.compose.runtime.Composable
import com.zorindisplays.display.ui.nav.Router
import com.zorindisplays.display.ui.nav.Screen

@Composable
fun ScreenHost(router: Router) {
    when (router.screen) {
        Screen.Idle -> IdleScreen(onStart = { router.go(Screen.Game) })
        Screen.Game -> GameScreen(
            onOpenSettings = { router.go(Screen.Settings) },
            onBackToIdle = { router.go(Screen.Idle) }
        )
        Screen.Settings -> SettingsScreen(
            onBack = { router.backToGame() }
        )
    }
}
