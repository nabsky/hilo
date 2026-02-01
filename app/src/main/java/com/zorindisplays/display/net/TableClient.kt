package com.zorindisplays.display.net

import com.zorindisplays.display.net.protocol.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class TableClient(
    private val deviceId: String,
    private val tableId: Int,
    private val host: String,
    private val port: Int
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: WebSocketSession? = null

    private val _state = MutableStateFlow<RoundStateDto?>(null)
    val state = _state.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    fun connect() {
        scope.launch {
            try {
                client.webSocket(host = host, port = port, path = "/ws") {
                    session = this
                    _connected.value = true

                    // hello
                    outgoing.send(
                        Frame.Text(
                            json.encodeToString(
                                WsMsg.Hello(role = Role.TABLE, deviceId = deviceId, tableId = tableId)
                            )
                        )
                    )

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val msg = runCatching { json.decodeFromString<WsMsg>(frame.readText()) }.getOrNull()
                            if (msg is WsMsg.State) _state.value = msg.state
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                _connected.value = false
                session = null
            }
        }
    }

    fun disconnect() {
        scope.launch {
            runCatching { session?.close() }
            session = null
            _connected.value = false
        }
    }

    fun send(cmd: Cmd) {
        val s = session ?: return
        scope.launch {
            val text = json.encodeToString(WsMsg.Command(cmd = cmd))
            runCatching { s.send(Frame.Text(text)) }
        }
    }

    fun stop() {
        disconnect()
        scope.cancel()
        runCatching { client.close() }
    }
}
