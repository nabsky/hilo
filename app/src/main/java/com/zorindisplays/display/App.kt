package com.zorindisplays.display

import androidx.compose.runtime.*
import com.zorindisplays.display.net.protocol.Role
import com.zorindisplays.display.net.protocol.Stage
import com.zorindisplays.display.ui.RegistrationGate
import com.zorindisplays.display.ui.nav.Router
import com.zorindisplays.display.ui.nav.Screen
import com.zorindisplays.display.ui.screens.GameScreen
import com.zorindisplays.display.ui.screens.IdleScreen
import com.zorindisplays.display.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun App(model: AppModel) {
    val router = remember { Router() }

    // Автонавигация для TABLE: если пришло не-IDLE состояние — идём в Game
    LaunchedEffect(model.tableState) {
        model.tableState?.collectLatest { st ->
            if (model.settings.role == Role.TABLE) {
                if (st?.stage == Stage.IDLE || st == null) router.go(Screen.Idle)
                else router.go(Screen.Game)
            }
        }
    }

    RegistrationGate {
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
