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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
        val density = LocalDensity.current

        val scale = minOf(maxWidth.value / DESIGN_W, maxHeight.value / DESIGN_H)
        val offsetXDp = (maxWidth.value - DESIGN_W * scale) / 2f
        val offsetYDp = (maxHeight.value - DESIGN_H * scale) / 2f

        val offsetXPx = with(density) { offsetXDp.dp.toPx() }
        val offsetYPx = with(density) { offsetYDp.dp.toPx() }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetXPx
                    translationY = offsetYPx
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
    val rowY = 360f
    val cardW = 230f
    val cardH = 340f
    val gap = 26f

    val rowWidth = cardW * 5f + gap * 4f
    val startX = (DESIGN_W - rowWidth) / 2f

     // compare
    val i = s.compareIndex.coerceIn(0, 3)
    val leftX = startX + (cardW + gap) * i
    val rightX = startX + (cardW + gap) * (i + 1)

    val targetScale = if (s.camera == Camera.COMPARE) 1.35f else 1f
    val animScale by animateFloatAsState(targetScale, label = "camScale")

    // “камера”: просто масштабируем всю сцену карт из центра пары
    val pairCenterX = (leftX + rightX + cardW) / 2f
    val pairCenterY = rowY + cardH / 2f

    val pivotX = pairCenterX / DESIGN_W
    val pivotY = pairCenterY / DESIGN_H

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val centerX = DESIGN_W / 2f
        val centerY = DESIGN_H / 2f

        val followPair = (s.stage != Stage.FINISH)

        val dx = if (followPair) centerX - (pairCenterX * animScale) else 0f
        val dy = if (followPair) centerY - (pairCenterY * animScale) else 0f

        Box(
            modifier = Modifier
                .offset(x = px(dx), y = px(dy))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
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
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    text: String,
    dim: Boolean
) {
    Box(
        modifier = Modifier
            .offset(x = px(x), y = px(y))
            .size(px(w), px(h))
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
private fun ChoiceHints(choice: Side?) {
    val y = 900f // ниже, чтобы не пересекалось с зумом

    val hiSelected = choice == Side.HI
    val loSelected = choice == Side.LO

    // левая и правая кнопки
    HintBox(
        x = 420f,
        y = y,
        text = "HI  x 6.06",
        selected = hiSelected
    )
    HintBox(
        x = 1120f,
        y = y,
        text = "LO  x 1.22",
        selected = loSelected
    )

    // символ между ними по центру
    val symbol = when (choice) {
        null -> "?"
        Side.HI -> "<"
        Side.LO -> ">"
    }

    BasicText(
        text = symbol,
        modifier = Modifier
            .offset(x = px(DESIGN_W / 2f - 18f), y = px(y + 4f)),
        style = DefaultTextStyle.copy(fontSize = 72.sp)
    )
}

@Composable
private fun HintBox(x: Float, y: Float, text: String, selected: Boolean) {
    val bg = if (selected) PrimaryTextColor else DefaultBackground
    val fg = if (selected) DefaultBackground else PrimaryTextColor

    Box(
        modifier = Modifier
            .offset(x = px(x), y = px(y))
            .size(px(380f), px(92f))
            .border(3.dp, PrimaryTextColor.copy(alpha = 0.9f))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = DefaultTextStyle.copy(
                fontSize = 40.sp,
                color = fg
            )
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

@Composable
private fun px(v: Float) = with(androidx.compose.ui.platform.LocalDensity.current) { v.toDp() }
