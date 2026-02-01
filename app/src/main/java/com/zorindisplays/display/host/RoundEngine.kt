package com.zorindisplays.display.host

import com.zorindisplays.display.net.protocol.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class RoundEngine {

    private val _state = MutableStateFlow(newIdle())
    val state = _state.asStateFlow()

    private fun now() = System.currentTimeMillis()

    private fun newIdle(): RoundStateDto = RoundStateDto(
        roundId = UUID.randomUUID().toString(),
        stage = Stage.IDLE,
        stageStartedAtMs = now()
    )

    fun apply(cmd: Cmd) {
        when (cmd) {
            is Cmd.Reset -> {
                _state.value = newIdle()
            }

            is Cmd.Arm -> {
                _state.value = _state.value.copy(
                    stage = Stage.ARMED,
                    stageStartedAtMs = now(),
                    tableId = cmd.tableId,
                    boxId = cmd.boxId,
                    resultText = "PLAYER PREPARE"
                )
            }

            is Cmd.BuyIn -> {
                _state.value = _state.value.copy(
                    stage = Stage.DEAL,
                    stageStartedAtMs = now(),
                    bank = cmd.amount,
                    stepIndex = 0,
                    currentCard = drawCard(),
                    revealedCard = null,
                    choice = null,
                    resultText = null
                )
                // после DEAL сразу даём выбирать
                _state.value = _state.value.copy(
                    stage = Stage.CHOOSING,
                    stageStartedAtMs = now()
                )
            }

            is Cmd.Choose -> {
                val s = _state.value
                if (s.stage != Stage.CHOOSING && s.stage != Stage.CONFIRMING) return

                _state.value = s.copy(
                    stage = Stage.CONFIRMING,
                    stageStartedAtMs = now(),
                    choice = cmd.side,
                    resultText = null
                )
            }

            is Cmd.Confirm -> {
                val s = _state.value
                if (s.stage != Stage.CONFIRMING || s.choice == null || s.currentCard == null) return

                val next = drawCard()
                val result = compare(s.currentCard, next, s.choice)

                val newBank = when (result) {
                    "YOU WON!" -> s.bank * 2 // пока заглушка; коэффициенты подключим позже
                    "TIE" -> s.bank
                    else -> 0
                }

                _state.value = s.copy(
                    stage = Stage.REVEAL,
                    stageStartedAtMs = now(),
                    revealedCard = next,
                    bank = newBank,
                    resultText = result
                )

                // дальше: или следующий шаг, или конец (пока просто сброс при лузе/нуле)
                if (newBank == 0) {
                    _state.value = newIdle()
                } else {
                    val nextStep = s.stepIndex + 1
                    if (nextStep >= 4) {
                        _state.value = _state.value.copy(
                            stage = Stage.FINISH,
                            stageStartedAtMs = now(),
                            resultText = "FINISH"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            stage = Stage.CHOOSING,
                            stageStartedAtMs = now(),
                            stepIndex = nextStep,
                            currentCard = next,
                            revealedCard = null,
                            choice = null,
                            resultText = null
                        )
                    }
                }
            }
        }
    }

    private fun drawCard(): String {
        // пока супер просто: "Q♦", "K♣" и т.п. (позже заменим на нормальную колоду)
        val ranks = listOf("2","3","4","5","6","7","8","9","10","J","Q","K","A")
        val suits = listOf("♠","♥","♦","♣")
        return ranks.random() + suits.random()
    }

    private fun rank(card: String): Int {
        // rank для compare: достаём начало строки до масти
        val r = card.dropLast(1)
        return when (r) {
            "2" -> 2; "3" -> 3; "4" -> 4; "5" -> 5; "6" -> 6; "7" -> 7
            "8" -> 8; "9" -> 9; "10" -> 10; "J" -> 11; "Q" -> 12; "K" -> 13; "A" -> 14
            else -> 0
        }
    }

    private fun compare(current: String, next: String, choice: Side): String {
        val a = rank(current)
        val b = rank(next)
        if (a == b) return "TIE"
        val isHigher = b > a
        val won = when (choice) {
            Side.HI -> isHigher
            Side.LO -> !isHigher
        }
        return if (won) "YOU WON!" else "YOU LOST!"
    }
}
