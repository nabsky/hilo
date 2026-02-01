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


/**
 * DTO (то, что гоняем по сети).
 * Держим простым: всё необходимое для рендера/анимации.
 */
@Serializable
data class RoundStateDto(
    val roundId: String,
    val stage: Stage,
    val stageStartedAtMs: Long,

    val tableId: Int? = null,
    val boxId: Int? = null,

    val bank: Int = 0,
    val stepIndex: Int = 0,

    // для UI: текущая открытая карта, следующая (если уже вскрыта)
    val currentCard: String? = null,
    val revealedCard: String? = null,

    val choice: Side? = null,
    val resultText: String? = null
)

@Serializable
enum class Stage {
    IDLE,
    ARMED,        // "player prepare"
    BUY_IN,       // ввод суммы
    DEAL,         // разложили 5 карт (пока можно без деталей)
    CHOOSING,     // выбор Hi/Lo + коэффициенты (позже добавим в state)
    CONFIRMING,   // zoom/подтверждение
    REVEAL,       // вскрытие и результат
    FINISH        // конец раунда
}
