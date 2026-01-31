package com.zorindisplays.display.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor

@Composable
fun IdleScreen(
    onStart: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "idle_spin")
    val angle = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onStart() },
        contentAlignment = Alignment.Center
    ) {
        // “Кольцо”
        Box(
            modifier = Modifier
                .size(320.dp)
                .rotate(angle.value)
                .border(6.dp, PrimaryTextColor.copy(alpha = 0.85f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = "HI-LO",
                style = DefaultTextStyle.copy(fontSize = 64.sp, color = PrimaryTextColor)
            )
            Spacer(Modifier.height(14.dp))
            BasicText(
                text = "TAP TO START",
                style = DefaultTextStyle.copy(fontSize = 28.sp, color = Color(0xFFCCCCCC))
            )
        }
    }
}
