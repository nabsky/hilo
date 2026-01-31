package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var tableName by remember { mutableStateOf(TextFieldValue("TABLE 1")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = "SETTINGS",
                style = DefaultTextStyle.copy(fontSize = 48.sp)
            )
            Box(
                modifier = Modifier
                    .border(2.dp, PrimaryTextColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                BasicText("BACK", style = DefaultTextStyle.copy(fontSize = 28.sp))
            }
        }

        BasicText(
            text = "TABLE NAME:",
            style = DefaultTextStyle.copy(fontSize = 28.sp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .border(2.dp, PrimaryTextColor.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = tableName,
                onValueChange = { incoming ->
                    tableName = incoming.copy(
                        // пример: всё в upper, ограничим длину
                        text = incoming.text.uppercase().take(16),
                        selection = TextRange(
                            incoming.selection.start.coerceIn(0, incoming.text.length.coerceAtMost(16)),
                            incoming.selection.end.coerceIn(0, incoming.text.length.coerceAtMost(16))
                        )
                    )
                },
                singleLine = true,
                textStyle = DefaultTextStyle.copy(fontSize = 40.sp),
                cursorBrush = SolidColor(PrimaryTextColor),
                modifier = Modifier.fillMaxSize()
            )
        }

        BasicText(
            text = "MORE SETTINGS HERE...",
            style = DefaultTextStyle.copy(fontSize = 24.sp)
        )
    }
}
