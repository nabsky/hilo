package com.zorindisplays.display.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zorindisplays.display.net.protocol.*
import com.zorindisplays.display.ui.theme.DefaultBackground
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import com.zorindisplays.display.ui.theme.PrimaryTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

private const val DESIGN_W = 1920f
private const val DESIGN_H = 1080f

@Composable
fun BigScreen(port: Int) {
    var state by remember { mutableStateOf<RoundStateDto?>(null) }

    val json = remember {
        Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    // poll /status локально (сервер внутри этого же эмулятора)
    LaunchedEffect(port) {
        while (true) {
            runCatching {
                val text = withContext(Dispatchers.IO) { httpGet("http://127.0.0.1:$port/status") }
                state = json.decodeFromString(RoundStateDto.serializer(), text)
            }
            delay(200)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DefaultBackground)
    ) {
        val scale = minOf(maxWidth.value / DESIGN_W, maxHeight.value / DESIGN_H)
        val offsetX = (maxWidth.value - DESIGN_W * scale) / 2f
        val offsetY = (maxHeight.value - DESIGN_H * scale) / 2f

        // “виртуальный холст 1920×1080”
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
                .size(DESIGN_W.dp, DESIGN_H.dp)
        ) {
            RenderBigState(state)
        }
    }
}

@Composable
private fun BoxScope.RenderBigState(s: RoundStateDto?) {
    if (s == null) return

    // верх: bank по центру, table/box справа
    TopBar(s)

    when (s.stage) {
        Stage.IDLE -> {
            CenterText("IDLE")
        }

        Stage.ARMED -> {
            CenterText("TABLE ${s.tableId}  BOX ${s.boxId}\nGET READY")
        }

        Stage.CHOOSING,
        Stage.CONFIRMING,
        Stage.REVEAL,
        Stage.FINISH -> {
            CardsRowScene(s)
        }
    }
}

@Composable
private fun BoxScope.TopBar(s: RoundStateDto) {
    // bank — сверху по центру
    BasicText(
        text = if (s.bank > 0) "${s.bank} USD" else "",
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 28.dp),
        style = DefaultTextStyle.copy(fontSize = 48.sp)
    )

    // table/box — справа сверху
    BasicText(
        text = buildString {
            if (s.tableId != null && s.boxId != null) append("T${s.tableId}  B${s.boxId}")
        },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 28.dp, end = 28.dp),
        style = DefaultTextStyle.copy(fontSize = 28.sp)
    )
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = DefaultTextStyle.copy(fontSize = 72.sp),
        )
    }
}

@Composable
private fun CardsRowScene(s: RoundStateDto) {
    val cards = s.cards
    if (cards.size < 5) {
        CenterText("WAITING CARDS…")
        return
    }

    // layout под 1920x1080:
    // ряд карт по центру, 5 штук
    val rowY = 360.dp
    val cardW = 230.dp
    val cardH = 340.dp
    val gap = 26.dp

    val rowWidth = cardW * 5 + gap * 4
    val startX = (DESIGN_W.dp - rowWidth) / 2

    // compare
    val i = s.compareIndex.coerceIn(0, 3)
    val leftX = startX + (cardW + gap) * i
    val rightX = startX + (cardW + gap) * (i + 1)

    val targetScale = if (s.camera == Camera.COMPARE) 1.35f else 1f
    val animScale by animateFloatAsState(targetScale, label = "camScale")

    // “камера”: просто масштабируем всю сцену карт из центра пары
    val pairCenterX = (leftX + rightX + cardW) / 2
    val pairCenterY = rowY + cardH / 2

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val cx = pairCenterX.toPx()
                    val cy = pairCenterY.toPx()
                    // scale around pair center
                    translationX = cx - cx * animScale
                    translationY = cy - cy * animScale
                    scaleX = animScale
                    scaleY = animScale
                }
        ) {
            // рисуем 5 карт
            for (idx in 0 until 5) {
                val x = startX + (cardW + gap) * idx
                val faceUp = idx <= i // открыты все до текущей (включая)
                val isNext = idx == i + 1
                val revealNext = (s.stage == Stage.REVEAL || s.stage == Stage.FINISH) && isNext

                CardView(
                    x = x,
                    y = rowY,
                    w = cardW,
                    h = cardH,
                    text = when {
                        idx == i + 1 && !revealNext -> "🂠" // закрытая
                        faceUp || revealNext -> cards[idx]
                        else -> "🂠"
                    },
                    dim = idx > i + 1
                )
            }

            // символ между парой при COMPARE/REVEAL
            if (s.camera == Camera.COMPARE || s.stage == Stage.REVEAL) {
                CompareOverlay(
                    leftX = leftX,
                    rightX = rightX,
                    rowY = rowY,
                    cardW = cardW,
                    cardH = cardH,
                    stage = s.stage,
                    resultText = s.resultText
                )
            }
        }

        // подсказки снизу (пока заглушки, коэффициенты добавим позже)
        if (s.stage == Stage.CHOOSING || s.stage == Stage.CONFIRMING) {
            ChoiceHints(choice = s.choice)
        }

        // финальный текст
        if (s.stage == Stage.REVEAL || s.stage == Stage.FINISH) {
            val t = s.resultText ?: ""
            if (t.isNotBlank()) {
                BasicText(
                    text = t,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    style = DefaultTextStyle.copy(fontSize = 64.sp)
                )
            }
        }
    }
}

@Composable
private fun CardView(
    x: Dp,
    y: Dp,
    w: Dp,
    h: Dp,
    text: String,
    dim: Boolean
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(w, h)
            .border(4.dp, PrimaryTextColor.copy(alpha = if (dim) 0.25f else 0.9f))
            .background(DefaultBackground)
            .padding(16.dp)
    ) {
        BasicText(
            text = text,
            style = DefaultTextStyle.copy(
                fontSize = 56.sp,
                color = PrimaryTextColor.copy(alpha = if (dim) 0.25f else 1f)
            )
        )
    }
}

@Composable
private fun CompareOverlay(
    leftX: Dp,
    rightX: Dp,
    rowY: Dp,
    cardW: Dp,
    cardH: Dp,
    stage: Stage,
    resultText: String?
) {
    val centerX = (leftX + rightX + cardW) / 2
    val symbol = when {
        stage != Stage.REVEAL -> "?"
        resultText == "TIE" -> "="
        resultText == "YOU WON!" -> "<"  // next higher if HI chosen; пока условно
        else -> ">"
    }

    BasicText(
        text = symbol,
        modifier = Modifier
            .offset(
                x = centerX - 16.dp,
                y = rowY + cardH / 2 - 60.dp
            ),
        style = DefaultTextStyle.copy(fontSize = 120.sp)
    )
}

@Composable
private fun ChoiceHints(choice: Side?) {
    val y = 820.dp

    val hiSelected = choice == Side.HI
    val loSelected = choice == Side.LO

    // слева
    HintBox(
        x = 420.dp,
        y = y,
        text = "HI  x 6.06",
        filled = hiSelected
    )
    // справа
    HintBox(
        x = 1120.dp,
        y = y,
        text = "LO  x 1.22",
        filled = loSelected
    )
}

@Composable
private fun HintBox(x: Dp, y: Dp, text: String, filled: Boolean) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(380.dp, 92.dp)
            .border(3.dp, PrimaryTextColor.copy(alpha = 0.9f))
            .background(if (filled) PrimaryTextColor.copy(alpha = 0.12f) else DefaultBackground),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = DefaultTextStyle.copy(fontSize = 40.sp)
        )
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
