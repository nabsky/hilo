package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor
import kotlin.random.Random

@Composable
fun GameScreen(
    onOpenSettings: () -> Unit,
    onBackToIdle: () -> Unit
) {
    var current by remember { mutableStateOf(Random.nextInt(1, 14)) } // 1..13
    var next by remember { mutableStateOf<Int?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    fun drawNext() {
        val n = Random.nextInt(1, 14)
        next = n
        lastResult = when {
            n > current -> "HIGH"
            n < current -> "LOW"
            else -> "SAME"
        }
        current = n
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallButton("IDLE", onBackToIdle)
            SmallButton("SETTINGS", onOpenSettings)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = "CURRENT: $current",
                style = DefaultTextStyle.copy(fontSize = 64.sp)
            )
            Spacer(Modifier.height(12.dp))

            if (lastResult != null) {
                BasicText(
                    text = "RESULT: $lastResult",
                    style = DefaultTextStyle.copy(fontSize = 36.sp)
                )
                Spacer(Modifier.height(12.dp))
            }

            BasicText(
                text = "TAP: DRAW NEXT",
                style = DefaultTextStyle.copy(fontSize = 28.sp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(3.dp, PrimaryTextColor)
                .clickable { drawNext() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "DRAW",
                style = DefaultTextStyle.copy(fontSize = 72.sp)
            )
        }
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(2.dp, PrimaryTextColor.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = DefaultTextStyle.copy(fontSize = 28.sp)
        )
    }
}
