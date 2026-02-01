package com.zorindisplays.display.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.ui.theme.DefaultBackground
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor

@Composable
fun IdleScreen(
    onOpenSettings: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "idle")
    val angle by inf.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "a"
    )

    Box(Modifier.fillMaxSize().background(DefaultBackground), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(320.dp).rotate(angle).border(6.dp, PrimaryTextColor.copy(alpha = 0.85f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("HI-LO", style = DefaultTextStyle.copy(fontSize = 64.sp))
            Spacer(Modifier.height(12.dp))
            BasicText(
                "SETTINGS",
                modifier = Modifier
                    .border(2.dp, PrimaryTextColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { onOpenSettings() },
                style = DefaultTextStyle.copy(fontSize = 28.sp)
            )
        }
    }
}
