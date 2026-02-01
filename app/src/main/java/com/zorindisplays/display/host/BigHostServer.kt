package com.zorindisplays.display.host

import com.zorindisplays.display.net.protocol.Cmd
import com.zorindisplays.display.net.protocol.Side
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
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sessions = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun start() {
        if (server != null) return

        serverScope.launch {
            while (isActive) {
                engine.tick(finishTimeoutMs = 10_000L)
                delay(500)
            }
        }

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

                // --- COMMANDS (для теста с браузера) ---
                get("/cmd/reset") {
                    engine.apply(Cmd.Reset)
                    call.respond(engine.state.value)
                }

                get("/cmd/arm") {
                    val tableId = call.request.queryParameters["table"]?.toIntOrNull() ?: 1
                    val boxId = call.request.queryParameters["box"]?.toIntOrNull() ?: 1
                    engine.apply(Cmd.Arm(tableId, boxId))
                    call.respond(engine.state.value)
                }

                get("/cmd/buyin") {
                    val amount = call.request.queryParameters["amount"]?.toIntOrNull() ?: 100
                    engine.apply(Cmd.BuyIn(amount))
                    call.respond(engine.state.value)
                }

                get("/cmd/choose") {
                    val side = call.request.queryParameters["side"]?.uppercase()
                    val s = if (side == "LO") Side.LO else if (side == "HI") Side.HI else Side.TIE
                    engine.apply(Cmd.Choose(s))
                    call.respond(engine.state.value)
                }

                get("/cmd/confirm") {
                    engine.apply(Cmd.Confirm)
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
        serverScope.cancel()
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
