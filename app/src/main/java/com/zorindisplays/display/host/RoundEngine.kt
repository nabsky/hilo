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

    private fun finish(bank: Int, text: String): RoundStateDto {
        val s = _state.value
        return s.copy(
            stage = Stage.FINISH,
            stageStartedAtMs = now(),
            bank = bank,
            camera = Camera.WIDE,
            choice = null,
            resultText = text
        )
    }

    fun apply(cmd: Cmd) {
        val s = _state.value

        when (cmd) {
            Cmd.Reset -> {
                _state.value = newIdle()
            }

            is Cmd.Arm -> {
                // 1) ARM разрешаем только из IDLE (не посреди игры)
                if (s.stage != Stage.IDLE) return

                _state.value = RoundStateDto(
                    roundId = UUID.randomUUID().toString(),
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
                // 2) BUYIN только после ARM (чтобы всегда был table/box)
                if (s.stage != Stage.ARMED) return
                if (s.tableId == null || s.boxId == null) return

                val cards = drawCards(5)
                _state.value = s.copy(
                    stage = Stage.CHOOSING,
                    stageStartedAtMs = now(),
                    bank = cmd.amount.coerceAtLeast(0),
                    stepIndex = 0,
                    cards = cards,
                    camera = Camera.WIDE,
                    compareIndex = 0,
                    choice = null,
                    resultText = null
                )
            }

            is Cmd.Choose -> {
                // Выбор только во время выбора/подтверждения
                if (s.stage != Stage.CHOOSING && s.stage != Stage.CONFIRMING) return
                if (s.cards.size < 5) return

                _state.value = s.copy(
                    stage = Stage.CONFIRMING,
                    stageStartedAtMs = now(),
                    choice = cmd.side,
                    camera = Camera.COMPARE,
                    resultText = null
                )
            }

            Cmd.Confirm -> {
                // Подтверждение только после выбора
                if (s.stage != Stage.CONFIRMING) return
                val choice = s.choice ?: return
                if (s.cards.size < 2) return

                val i = s.compareIndex.coerceIn(0, 3)
                val current = s.cards[i]
                val next = s.cards[i + 1]

                val cmp = compareRanks(current, next)
                val won = when (cmp) {
                    0 -> true // tie — проходит дальше, банк не меняем
                    1 -> (choice == Side.HI)
                    -1 -> (choice == Side.LO)
                    else -> false
                }

                if (!won) {
                    // 3) проигрыш -> FINISH, банк=0, утешительный текст
                    _state.value = finish(
                        bank = 0,
                        text = "BETTER LUCK NEXT TIME!"
                    )
                    return
                }

                // выигрыш или tie
                val newBank = if (cmp == 0) s.bank else (s.bank * 2) // пока x2 (коэфы добавим позже)

                val nextIndex = i + 1
                if (nextIndex >= 4) {
                    // 2) финиш с поздравлением
                    _state.value = finish(
                        bank = newBank,
                        text = "CONGRATULATIONS!"
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

    // 4) тик для авто-возврата в IDLE (FINISH -> IDLE через N мс)
    fun tick(nowMs: Long = System.currentTimeMillis(), finishTimeoutMs: Long = 10_000L) {
        val s = _state.value
        if (s.stage == Stage.FINISH) {
            if (nowMs - s.stageStartedAtMs >= finishTimeoutMs) {
                _state.value = newIdle()
            }
        }
    }

    // cards utils
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

    // returns: 1 if next > current, -1 if next < current, 0 if equal
    private fun compareRanks(current: String, next: String): Int {
        val a = rank(current)
        val b = rank(next)
        return when {
            b > a -> 1
            b < a -> -1
            else -> 0
        }
    }
}
