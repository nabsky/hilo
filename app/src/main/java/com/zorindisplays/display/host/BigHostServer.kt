package com.zorindisplays.display.host

import com.zorindisplays.display.net.protocol.WsMsg
import com.zorindisplays.display.net.protocol.WsMsg.State
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Duration
import java.util.concurrent.CopyOnWriteArraySet

class BigHostServer(
    private val port: Int = 8080
) {
    private val engine = RoundEngine()
    private var server: ApplicationEngine? = null

    private val sessions = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun start() {
        if (server != null) return

        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                // HTTP — для статуса/статистики (пока просто текущий state)
                get("/status") {
                    call.respond(engine.state.value)
                }

                // WebSocket — realtime
                webSocket("/ws") {
                    sessions.add(this)
                    try {
                        // при подключении сразу отправим текущее состояние
                        sendText(State(engine.state.value).toJson())

                        // подписка: рассылаем state при каждом изменении
                        val job = CoroutineScope(Dispatchers.Default).launch {
                            engine.state.collect { st ->
                                broadcast(State(st))
                            }
                        }

                        // читаем команды
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val msg = runCatching { json.decodeFromString<WsMsg>(frame.readText()) }.getOrNull()
                                if (msg is WsMsg.Command) {
                                    engine.apply(msg.cmd)
                                }
                            }
                        }

                        job.cancel()
                    } finally {
                        sessions.remove(this)
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private suspend fun DefaultWebSocketServerSession.sendText(text: String) {
        send(Frame.Text(text))
    }

    private fun WsMsg.toJson(): String = json.encodeToString(this)

    private suspend fun broadcast(msg: WsMsg) {
        val text = msg.toJson()
        sessions.forEach { s ->
            runCatching { s.send(Frame.Text(text)) }
        }
    }
}
