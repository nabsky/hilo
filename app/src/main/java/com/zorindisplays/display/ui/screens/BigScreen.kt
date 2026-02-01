package com.zorindisplays.display.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import com.zorindisplays.display.net.protocol.RoundStateDto
import com.zorindisplays.display.ui.theme.DefaultBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun BigScreen(
    port: Int
) {
    var state by remember { mutableStateOf<RoundStateDto?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    val json = remember {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    LaunchedEffect(port) {
        while (true) {
            try {
                val text = withContext(Dispatchers.IO) {
                    httpGet("http://127.0.0.1:$port/status")
                }
                state = json.decodeFromString(RoundStateDto.serializer(), text)
                err = null
            } catch (t: Throwable) {
                err = t.javaClass.simpleName + ": " + (t.message ?: "")
            }
            delay(400)
        }
    }


    Column(Modifier.fillMaxSize().background(DefaultBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText("BIG MODE", style = DefaultTextStyle.copy(fontSize = 42.sp))

        if (err != null) {
            BasicText("ERROR: $err", style = DefaultTextStyle.copy(fontSize = 20.sp))
        }

        val s = state
        if (s == null) {
            BasicText("NO STATE", style = DefaultTextStyle.copy(fontSize = 28.sp))
        } else {
            BasicText("stage: ${s.stage}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("table: ${s.tableId}  box: ${s.boxId}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("bank: ${s.bank}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("step: ${s.stepIndex}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("card: ${s.currentCard}  next: ${s.revealedCard}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("choice: ${s.choice}", style = DefaultTextStyle.copy(fontSize = 28.sp))
            BasicText("result: ${s.resultText}", style = DefaultTextStyle.copy(fontSize = 28.sp))
        }

        Spacer(Modifier.height(12.dp))
        BasicText("Control from Mac:", style = DefaultTextStyle.copy(fontSize = 20.sp))
        BasicText("adb forward tcp:18080 tcp:$port", style = DefaultTextStyle.copy(fontSize = 20.sp))
        BasicText("curl http://localhost:18080/cmd/arm?table=2&box=3", style = DefaultTextStyle.copy(fontSize = 20.sp))
    }
}

private fun httpGet(url: String): String {
    val c = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 1500
        readTimeout = 1500
    }
    c.inputStream.bufferedReader().use { return it.readText() }
}