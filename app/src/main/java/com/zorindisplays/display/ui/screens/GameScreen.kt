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
fun GameScreen(
    model: AppModel,
    onOpenSettings: () -> Unit,
    onBackToIdle: () -> Unit
) {
    var connected by remember { mutableStateOf(false) }
    var line by remember { mutableStateOf("NO STATE") }

    LaunchedEffect(model.tableConnected) {
        model.tableConnected?.collectLatest { connected = it }
    }
    LaunchedEffect(model.tableState) {
        model.tableState?.collectLatest { st ->
            line = st?.let {
                val i = it.compareIndex.coerceIn(0, 3)
                val cur = it.cards.getOrNull(i)
                val next = it.cards.getOrNull(i + 1)

                "${it.stage} bank=${it.bank} i=$i cur=$cur next=$next choice=${it.choice} result=${it.resultText}"
            } ?: "NO STATE"
        }
    }

    Column(Modifier.fillMaxSize().background(DefaultBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText("Connected: $connected", style = DefaultTextStyle)
        BasicText(line, style = DefaultTextStyle)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("IDLE") { onBackToIdle() }
            Btn("SETTINGS") { onOpenSettings() }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("ARM B3") { model.send(Cmd.Arm(tableId = model.settings.tableId, boxId = 3)) }
            Btn("BUYIN 100") { model.send(Cmd.BuyIn(100)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("HI") { model.send(Cmd.Choose(Side.HI)) }
            Btn("LO") { model.send(Cmd.Choose(Side.LO)) }
            Btn("CONFIRM") { model.send(Cmd.Confirm) }
            Btn("RESET") { model.send(Cmd.Reset) }
        }
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
