package com.zorindisplays.display.net.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsMsg {

    @Serializable
    @SerialName("hello")
    data class Hello(
        val role: Role,
        val deviceId: String,
        val tableId: Int? = null
    ) : WsMsg()

    @Serializable
    @SerialName("cmd")
    data class Command(
        val roundId: String? = null,
        val cmd: Cmd
    ) : WsMsg()

    @Serializable
    @SerialName("state")
    data class State(
        val state: RoundStateDto
    ) : WsMsg()
}

@Serializable
enum class Role { BIG, TABLE }

@Serializable
sealed class Cmd {
    @Serializable @SerialName("arm")
    data class Arm(val tableId: Int, val boxId: Int) : Cmd()

    @Serializable @SerialName("buyin")
    data class BuyIn(val amount: Int) : Cmd()

    @Serializable @SerialName("choose")
    data class Choose(val side: Side) : Cmd()

    @Serializable @SerialName("confirm")
    data object Confirm : Cmd()

    @Serializable @SerialName("reset")
    data object Reset : Cmd()
}

@Serializable
enum class Side { HI, LO }

@Serializable
enum class Stage {
    IDLE,
    ARMED,
    CHOOSING,
    CONFIRMING,
    REVEAL,
    FINISH
}

@Serializable
enum class Camera { WIDE, COMPARE }

@Serializable
data class RoundStateDto(
    val roundId: String,
    val stage: Stage,
    val stageStartedAtMs: Long,

    val tableId: Int? = null,
    val boxId: Int? = null,

    val bank: Int = 0,
    val stepIndex: Int = 0,

    // 5 карт раунда (строки пока)
    val cards: List<String> = emptyList(),

    // какая “камера” на биг-экране
    val camera: Camera = Camera.WIDE,

    // индекс пары (0..3): сравниваем cards[i] и cards[i+1]
    val compareIndex: Int = 0,

    // выбранная сторона и результат
    val choice: Side? = null,
    val resultText: String? = null,
    val hiX: Double = 0.0,
    val loX: Double = 0.0
)
