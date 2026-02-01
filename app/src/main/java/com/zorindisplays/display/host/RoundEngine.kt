package com.zorindisplays.display.host

import com.zorindisplays.display.net.protocol.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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
            Cmd.Reset -> _state.value = newIdle()

            is Cmd.Arm -> {
                _state.value = _state.value.copy(
                    stage = Stage.ARMED,
                    stageStartedAtMs = now(),
                    tableId = cmd.tableId,
                    boxId = cmd.boxId,
                    bank = 0,
                    stepIndex = 0,
                    cards = emptyList(),
                    camera = Camera.WIDE,
                    compareIndex = 0,
                    choice = null,
                    resultText = "GET READY"
                )
            }

            is Cmd.BuyIn -> {
                val cards = drawCards(5)
                _state.value = _state.value.copy(
                    stage = Stage.CHOOSING,
                    stageStartedAtMs = now(),
                    bank = cmd.amount,
                    stepIndex = 0,
                    cards = cards,
                    camera = Camera.WIDE,
                    compareIndex = 0,
                    choice = null,
                    resultText = null
                )
            }

            is Cmd.Choose -> {
                val s = _state.value
                if (s.stage != Stage.CHOOSING && s.stage != Stage.CONFIRMING) return

                _state.value = s.copy(
                    stage = Stage.CONFIRMING,
                    stageStartedAtMs = now(),
                    choice = cmd.side,
                    camera = Camera.COMPARE,
                    resultText = null
                )
            }

            Cmd.Confirm -> {
                val s = _state.value
                if (s.stage != Stage.CONFIRMING || s.choice == null) return
                if (s.cards.size < 2) return
                val i = s.compareIndex.coerceIn(0, 3)
                val a = s.cards[i]
                val b = s.cards[i + 1]

                val result = compare(a, b, s.choice)
                val newBank = when (result) {
                    "YOU WON!" -> s.bank * 2   // пока заглушка, потом под коэффициенты
                    "TIE" -> s.bank
                    else -> 0
                }

                if (newBank == 0) {
                    _state.value = newIdle()
                    return
                }

                val nextIndex = i + 1
                if (nextIndex >= 4) {
                    _state.value = s.copy(
                        stage = Stage.FINISH,
                        stageStartedAtMs = now(),
                        bank = newBank,
                        camera = Camera.WIDE,
                        resultText = "FINISH"
                    )
                } else {
                    _state.value = s.copy(
                        stage = Stage.CHOOSING,
                        stageStartedAtMs = now(),
                        bank = newBank,
                        compareIndex = nextIndex,
                        stepIndex = nextIndex,
                        camera = Camera.WIDE,
                        choice = null,
                        resultText = null
                    )
                }
            }
        }
    }

    private fun drawCards(n: Int): List<String> {
        val ranks = listOf("2","3","4","5","6","7","8","9","10","J","Q","K","A")
        val suits = listOf("♠","♥","♦","♣")
        return List(n) { ranks.random() + suits.random() }
    }

    private fun rank(card: String): Int {
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
