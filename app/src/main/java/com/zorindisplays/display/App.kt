package com.zorindisplays.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zorindisplays.display.ui.RegistrationGate
import com.zorindisplays.display.ui.nav.Router
import com.zorindisplays.display.ui.screens.ScreenHost
import com.zorindisplays.display.ui.theme.DefaultBackground

@Composable
fun App() {
    val router = remember { Router() }

    RegistrationGate {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DefaultBackground)
        ) {
            ScreenHost(router)
        }
    }
}
