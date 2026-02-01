package com.zorindisplays.display.host

import com.zorindisplays.display.net.protocol.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.floor

class RoundEngine {

    private val _state = MutableStateFlow(newIdle())
    val state = _state.asStateFlow()

    private fun now() = System.currentTimeMillis()

    private fun newIdle(): RoundStateDto = RoundStateDto(
        roundId = UUID.randomUUID().toString(),
        stage = Stage.IDLE,
        stageStartedAtMs = now(),
        hiX = 0.0,
        loX = 0.0
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
            Cmd.Reset -> _state.value = newIdle()

            is Cmd.Arm -> {
                // ARM только из IDLE
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
                    resultText = "GET READY",
                    hiX = 0.0,
                    loX = 0.0
                )
            }

            is Cmd.BuyIn -> {
                // BUYIN только после ARM
                if (s.stage != Stage.ARMED) return
                if (s.tableId == null || s.boxId == null) return

                val cards = drawCards(5)

                val i = 0
                val cur = cards[i]
                val prior = countPriorSameRank(cards, i)
                val (hiX, loX) = multipliersFor(cur)
                val tieX = tieMultiplier(prior)

                _state.value = s.copy(
                    stage = Stage.CHOOSING,
                    stageStartedAtMs = now(),
                    bank = cmd.amount.coerceAtLeast(0),
                    stepIndex = 0,
                    cards = cards,
                    camera = Camera.WIDE,
                    compareIndex = 0,
                    choice = null,
                    resultText = null,
                    hiX = hiX,
                    loX = loX,
                    tieX = tieX
                )
            }

            is Cmd.Choose -> {
                if (s.stage != Stage.CHOOSING && s.stage != Stage.CONFIRMING) return
                if (s.cards.size < 5) return

                val i = s.compareIndex.coerceIn(0, 3)
                val cur = s.cards.getOrNull(i) ?: return

                val (hiX, loX) = multipliersFor(cur)
                val prior = countPriorSameRank(s.cards, i)
                val tieX = tieMultiplier(prior)

// запреты если коэффициент 0 (невозможно)
                if (cmd.side == Side.HI && hiX == 0.0) return
                if (cmd.side == Side.LO && loX == 0.0) return
                if (cmd.side == Side.TIE && tieX == 0.0) return

                _state.value = s.copy(
                    stage = Stage.CONFIRMING,
                    stageStartedAtMs = now(),
                    choice = cmd.side,
                    camera = Camera.COMPARE,
                    resultText = null,
                    hiX = hiX,
                    loX = loX,
                    tieX = tieX
                )
            }

            Cmd.Confirm -> {
                if (s.stage != Stage.CONFIRMING) return
                val choice = s.choice ?: return
                val i = s.compareIndex.coerceIn(0, 3)
                val current = s.cards[i]
                val next = s.cards[i + 1]

                val cmp = compareRanks(current, next)

                // helper: завершение шага/раунда
                fun goNextOrFinish(bankAfter: Int) {
                    val nextIndex = i + 1
                    if (bankAfter <= 0) {
                        _state.value = finish(0, "BETTER LUCK NEXT TIME!")
                        return
                    }
                    if (nextIndex >= 4) {
                        _state.value = finish(
                            bank = bankAfter,
                            text = "CONGRATULATIONS!\nYOU WON\n${bankAfter} USD"
                        )
                    } else {
                        val cur2 = s.cards[nextIndex]
                        val (hiN, loN) = multipliersFor(cur2)
                        val prior = countPriorSameRank(s.cards, nextIndex)
                        val tieN = tieMultiplier(prior)

                        _state.value = s.copy(
                            stage = Stage.CHOOSING,
                            stageStartedAtMs = now(),
                            bank = bankAfter,
                            compareIndex = nextIndex,
                            stepIndex = nextIndex,
                            camera = Camera.WIDE,
                            choice = null,
                            resultText = null,
                            hiX = hiN,
                            loX = loN,
                            tieX = tieN
                        )
                    }
                }

                when (choice) {
                    Side.HI, Side.LO -> {
                        // ничья = проигрыш (как раньше)
                        val won = when (cmp) {
                            1 -> (choice == Side.HI)
                            -1 -> (choice == Side.LO)
                            else -> false
                        }

                        if (!won) {
                            _state.value = finish(0, "BETTER LUCK NEXT TIME!")
                            return
                        }

                        val coef = if (choice == Side.HI) s.hiX else s.loX
                        val newBank = kotlin.math.floor(s.bank * coef).toInt()
                        goNextOrFinish(newBank)
                    }

                    Side.TIE -> {
                        val isTie = (cmp == 0)
                        val newBank = if (isTie) {
                            kotlin.math.floor(s.bank * s.tieX).toInt()
                        } else {
                            // проиграл TIE → банк /2, но игра продолжается
                            (s.bank / 2)
                        }
                        goNextOrFinish(newBank)
                    }
                }
            }
        }
    }

    fun tick(nowMs: Long = System.currentTimeMillis(), finishTimeoutMs: Long = 10_000L) {
        val s = _state.value
        if (s.stage == Stage.FINISH) {
            if (nowMs - s.stageStartedAtMs >= finishTimeoutMs) {
                _state.value = newIdle()
            }
        }
    }

    // ====== FIXED MULTIPLIERS (RTP≈95% for 4 steps, tie=loss) ======
    // ranks: 2..14 (A=14). Coefs are symmetric around 8.
    private fun multipliersFor(card: String): Pair<Double, Double> {
        return multipliersForRank(rank(card))
    }

    private fun multipliersForRank(r: Int): Pair<Double, Double> {
        return when (r) {
            2  -> 1.05 to 0.0
            3  -> 1.14 to 12.59
            4  -> 1.26 to 6.29
            5  -> 1.40 to 4.20
            6  -> 1.57 to 3.15
            7  -> 1.80 to 2.52
            8  -> 2.10 to 2.10
            9  -> 2.52 to 1.80
            10 -> 3.15 to 1.57
            11 -> 4.20 to 1.40 // J
            12 -> 6.29 to 1.26 // Q
            13 -> 12.59 to 1.14 // K
            14 -> 0.0 to 1.05   // A
            else -> 0.0 to 0.0
        }
    }

    // ====== cards utils ======
    private fun drawCards(n: Int): List<String> {
        val ranks = listOf("2","3","4","5","6","7","8","9","10","J","Q","K","A")
        val suits = listOf("♠","♥","♦","♣")

        val deck = buildList(52) {
            for (r in ranks) for (s in suits) add(r + s)
        }.shuffled()

        return deck.take(n)
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

    private fun countPriorSameRank(cards: List<String>, index: Int): Int {
        val r = rank(cards[index])
        var cnt = 0
        for (k in 0 until index) {
            if (rank(cards[k]) == r) cnt++
        }
        return cnt
    }

    private fun tieMultiplier(prior: Int): Double {
        return when {
            prior <= 0 -> 8.0
            prior == 1 -> 12.0
            prior >= 2 -> 24.0
            else -> 0.0
        }
    }
}
