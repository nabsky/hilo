package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zorindisplays.display.AppModel
import com.zorindisplays.display.net.protocol.Cmd
import com.zorindisplays.display.net.protocol.Side
import com.zorindisplays.display.ui.theme.DefaultBackground
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TableGameScreen(model: AppModel) {
    val stateFlow = model.tableState
    val connectedFlow = model.tableConnected

    var connected by remember { mutableStateOf(false) }
    var stageText by remember { mutableStateOf("NO STATE") }

    LaunchedEffect(connectedFlow) {
        connectedFlow?.collectLatest { connected = it }
    }
    LaunchedEffect(stateFlow) {
        stateFlow?.collectLatest { st ->
            stageText = st?.let {
                val i = it.compareIndex.coerceIn(0, it.cards.size - 2)
                val current = it.cards.getOrNull(i)
                val next = it.cards.getOrNull(i + 1)

                "${it.stage} bank=${it.bank} cur=$current next=$next result=${it.resultText}"
            } ?: "NO STATE"
        }
    }

    Column(Modifier.fillMaxSize().background(DefaultBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText("TABLE MODE", style = DefaultTextStyle)
        BasicText("Connected: $connected", style = DefaultTextStyle)
        BasicText(stageText, style = DefaultTextStyle)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("ARM (T2 B3)") { model.send(Cmd.Arm(tableId = model.settings.tableId, boxId = 3)) }
            Btn("BUYIN 100") { model.send(Cmd.BuyIn(100)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("HI") { model.send(Cmd.Choose(Side.HI)) }
            Btn("LO") { model.send(Cmd.Choose(Side.LO)) }
            Btn("CONFIRM") { model.send(Cmd.Confirm) }
        }

        Btn("RESET") { model.send(Cmd.Reset) }
    }
}

@Composable
private fun Btn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(2.dp, PrimaryTextColor)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(onClick = onClick)
    ) {
        BasicText(text, style = DefaultTextStyle)
    }
}
