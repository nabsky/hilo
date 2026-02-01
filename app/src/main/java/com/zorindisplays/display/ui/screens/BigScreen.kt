package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zorindisplays.display.AppModel
import com.zorindisplays.display.ui.theme.DefaultBackground
import com.zorindisplays.display.ui.theme.DefaultTextStyle

@Composable
fun BigScreen(model: AppModel) {
    // в режиме BIG состояние локально внутри host'а; пока для простоты покажем “SERVER RUNNING”
    Column(Modifier.fillMaxSize().background(DefaultBackground).padding(24.dp)) {
        BasicText("BIG MODE", style = DefaultTextStyle)
        Spacer(Modifier.height(12.dp))
        BasicText("Host: 0.0.0.0:${model.settings.port}", style = DefaultTextStyle)
        BasicText("Open /status in browser for JSON", style = DefaultTextStyle)
    }
}
