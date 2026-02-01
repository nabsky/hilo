package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zorindisplays.display.AppModel
import com.zorindisplays.display.net.protocol.Role
import com.zorindisplays.display.settings.SettingsState
import com.zorindisplays.display.ui.theme.DefaultBackground
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor

@Composable
fun SettingsScreen(
    model: AppModel,
    onBack: () -> Unit
) {
    var role by remember { mutableStateOf(model.settings.role) }
    var host by remember { mutableStateOf(TextFieldValue(model.settings.host)) }
    var port by remember { mutableStateOf(TextFieldValue(model.settings.port.toString())) }
    var tableId by remember { mutableStateOf(TextFieldValue(model.settings.tableId.toString())) }

    Column(Modifier.fillMaxSize().background(DefaultBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn(if (role == Role.BIG) "[BIG]" else "BIG") { role = Role.BIG }
            Btn(if (role == Role.TABLE) "[TABLE]" else "TABLE") { role = Role.TABLE }
            Btn("BACK") { onBack() }
        }

        BasicText("HOST:", style = DefaultTextStyle)
        Input(host) { host = it }

        BasicText("PORT:", style = DefaultTextStyle)
        Input(port) { port = it.onlyDigits(maxLen = 5) }

        BasicText("TABLE ID:", style = DefaultTextStyle)
        Input(tableId) { tableId = it.onlyDigits(maxLen = 2) }

        Btn("APPLY") {
            val newS = SettingsState(
                role = role,
                host = host.text.ifBlank { "10.0.2.2" },
                port = port.text.toIntOrNull() ?: 8080,
                tableId = tableId.text.toIntOrNull() ?: 1
            )
            model.applySettings(newS)
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
    ) { BasicText(text, style = DefaultTextStyle) }
}

@Composable
private fun Input(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(2.dp, PrimaryTextColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = DefaultTextStyle,
            cursorBrush = SolidColor(PrimaryTextColor),
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun TextFieldValue.onlyDigits(maxLen: Int): TextFieldValue {
    val cleaned = text.filter { it.isDigit() }.take(maxLen)
    val sel = selection.end.coerceIn(0, cleaned.length)
    return copy(text = cleaned, selection = TextRange(sel))
}
