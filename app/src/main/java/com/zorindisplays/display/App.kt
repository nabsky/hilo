package com.zorindisplays.display

import androidx.compose.runtime.*
import com.zorindisplays.display.net.protocol.Role
import com.zorindisplays.display.net.protocol.Stage
import com.zorindisplays.display.ui.RegistrationGate
import com.zorindisplays.display.ui.nav.Router
import com.zorindisplays.display.ui.nav.Screen
import com.zorindisplays.display.ui.screens.BigScreen
import com.zorindisplays.display.ui.screens.GameScreen
import com.zorindisplays.display.ui.screens.IdleScreen
import com.zorindisplays.display.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun App(model: AppModel) {
    val router = remember { Router() }
    val role = model.settings.role

    // Автонавигация только для TABLE
    LaunchedEffect(role, model.tableState) {
        if (role != Role.TABLE) return@LaunchedEffect

        model.tableState?.collectLatest { st ->
            if (st?.stage == Stage.IDLE || st == null) router.go(Screen.Idle)
            else router.go(Screen.Game)
        }
    }

    RegistrationGate {
        if (role == Role.BIG) {
            BigScreen(port = model.settings.port)
            return@RegistrationGate
        }

        // TABLE mode
        when (router.screen) {
            Screen.Idle -> IdleScreen(
                onOpenSettings = { router.go(Screen.Settings) }
            )

            Screen.Game -> GameScreen(
                model = model,
                onOpenSettings = { router.go(Screen.Settings) },
                onBackToIdle = { router.go(Screen.Idle) }
            )

            Screen.Settings -> SettingsScreen(
                model = model,
                onBack = { router.backToGame() }
            )
        }
    }
}
