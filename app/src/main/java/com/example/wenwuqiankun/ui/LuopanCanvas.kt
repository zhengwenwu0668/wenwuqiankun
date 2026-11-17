package com.example.wenwuqiankun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.wenwuqiankun.domain.FeixingResult
import com.example.wenwuqiankun.domain.YearMonthResult
import com.example.wenwuqiankun.domain.Constants as C
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 0..359° 的罗盘方向查表：算式与逐次调用完全一致（先 toRadians 再收窄成 Float），只多算一次。
private val COMPASS_SIN = FloatArray(360) { sin(Math.toRadians(it.toDouble()).toFloat()) }
private val COMPASS_COS = FloatArray(360) { cos(Math.toRadians(it.toDouble()).toFloat()) }

// 同一 (文字, 字号, 颜色) 的 TextLayoutResult 复用：一次排盘后勾选流年/流月等重绘不再重新测量。
// 缓存随 density/fontScale 变化整体丢弃，条目上限就是度盘 + 山名 + 九宫字符数。
private class LabelCache {
    private val layouts = HashMap<String, TextLayoutResult>()

    fun getOrMeasure(measurer: TextMeasurer, text: String, fontSp: Float, color: Color): TextLayoutResult {
        val key = "$text#$fontSp#$color"
        val cached = layouts[key]
        if (cached != null) return cached
        val measured = measurer.measure(text, TextStyle(fontSize = fontSp.sp, color = color))
        layouts[key] = measured
        return measured
    }
}

// 九宫格中心运星字号（sp），供控制区沿用，保证参数区版面与九宫格字号同源。
internal fun yunFontSp(canvasWidthPx: Float, density: Float): Float {
    val r = 0.40f * canvasWidthPx
    val a = 0.825f * r / sqrt(2f)
    val step = 2f * a / 3f
    return step * 0.35f / density
}

@Composable
fun LuopanCanvas(
    result: FeixingResult?,
    yearMonthResult: YearMonthResult?,
    showYear: Boolean,
    showMonth: Boolean,
    zuoIdx: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelCache = remember(density.density, density.fontScale) { LabelCache() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val S = size.minDimension
        if (S <= 0f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        // R 为环带基准半径，取箭头图标外沿仍能容下的最大值（画布半边 0.5S）；
        // R0 只用于定住箭头尺寸，使箭头不随环带放大。
        val R0 = 0.40f * S
        val R = 0.465f * S
        val lw = 0.6f * density.density

        drawRect(Color.White)

        drawCircle(Color.Black, radius = R, center = Offset(cx, cy), style = Stroke(lw))
        drawCircle(Color.Black, radius = 0.925f * R, center = Offset(cx, cy), style = Stroke(lw))
        drawCircle(Color.Black, radius = 0.825f * R, center = Offset(cx, cy), style = Stroke(lw))

        for (d in 0 until 360) {
            // 10° 刻度压到 0.015R：腾出的径向空间给放大后的度盘数字（数字外沿正好贴到刻度内端）。
            val tickLen = when {
                d % 10 == 0 -> 0.01500f * R
                d % 5 == 0 -> 0.05000f * R
                else -> 0.03125f * R
            }
            val p1 = compassToCanvas(cx, cy, R, d)
            val p2 = compassToCanvas(cx, cy, R - tickLen, d)
            drawLine(Color.Black, p1, p2, strokeWidth = lw)
        }

        for (d in 0 until 360 step 10) {
            val dotCenter = compassToCanvas(cx, cy, 0.984375f * R, d)
            val dotR = 0.00625f * R
            val dotPath = Path().apply {
                for (i in 0 until 12) {
                    val a = (i * 30.0 * Math.PI / 180.0).toFloat()
                    val px = dotCenter.x + dotR * cos(a)
                    val py = dotCenter.y + dotR * sin(a)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(dotPath, Color.Black, style = Fill)

            // 角度数字：以 9.5° 在 0.946875R 处的弧长作为可用宽度，横向 0.95 系数触发等比压缩字号
            val availArc = 9.5f * (Math.PI / 180.0).toFloat() * 0.946875f * R
            drawRotatedText(
                textMeasurer, labelCache, d.toFloat(), cx, cy,
                0.946875f * R, "$d",
                0.0677f * R / density.density, Color.Black,
                arcLimit = availArc
            )
        }

        for (k in 0 until 24) {
            val boundaryAngle = k * 15f + 7.5f
            val p1 = compassToCanvas(cx, cy, 0.825f * R, boundaryAngle)
            val p2 = compassToCanvas(cx, cy, 0.925f * R, boundaryAngle)
            drawLine(Color.Black, p1, p2, strokeWidth = lw)
        }

        for (j in 0 until 24) {
            val angle = j * 15f
            drawRotatedText(
                textMeasurer, labelCache, angle, cx, cy,
                0.875f * R, C.MOUNTAINS[j],
                S * 0.0389f / density.density, Color.Black
            )
        }

        val a = 0.825f * R / sqrt(2f)
        val step = 2f * a / 3f
        val gridLeft = cx - a
        val gridTop = cy - a
        val gridRight = cx + a
        val gridBottom = cy + a

        drawLine(Color.Black, Offset(gridLeft, gridTop), Offset(gridRight, gridTop), lw)
        drawLine(Color.Black, Offset(gridRight, gridTop), Offset(gridRight, gridBottom), lw)
        drawLine(Color.Black, Offset(gridRight, gridBottom), Offset(gridLeft, gridBottom), lw)
        drawLine(Color.Black, Offset(gridLeft, gridBottom), Offset(gridLeft, gridTop), lw)

        for (i in 1..2) {
            val y = gridTop + i * step
            drawLine(Color.Black, Offset(gridLeft, y), Offset(gridRight, y), lw)
            val x = gridLeft + i * step
            drawLine(Color.Black, Offset(x, gridTop), Offset(x, gridBottom), lw)
        }

        if (result != null) {
            for (idx in 0 until 9) {
                val row = idx / 3
                val col = idx % 3
                val cellCx = cx + (col - 1) * step
                val cellCy = cy + (row - 1) * step

                val yunChar = C.YUN_LABELS[result.yunPan[idx] - 1]
                drawCenteredText(textMeasurer, labelCache, cellCx, cellCy, yunChar, step * 0.35f / density.density, Color.Black)

                // 规格「绘图参数」的角星坐标按 matplotlib Y 轴向上写；Compose Canvas 为 Y 轴向下，
                // 故角星 Y 偏移需翻转：山/向星在上方两角，年/月星在下方两角（验收 #3/#4）。
                drawCenteredText(textMeasurer, labelCache, cellCx - step * 0.32f, cellCy - step * 0.32f, "${result.shanPan[idx]}", step * 0.25f / density.density, Color.Black)
                drawCenteredText(textMeasurer, labelCache, cellCx + step * 0.32f, cellCy - step * 0.32f, "${result.xiangPan[idx]}", step * 0.25f / density.density, Color.Black)

                if (showYear && yearMonthResult != null) {
                    drawCenteredText(textMeasurer, labelCache, cellCx - step * 0.32f, cellCy + step * 0.32f, "${yearMonthResult.yearPan[idx]}", step * 0.25f / density.density, Color.Red)
                }
                if (showMonth && yearMonthResult != null) {
                    drawCenteredText(textMeasurer, labelCache, cellCx + step * 0.32f, cellCy + step * 0.32f, "${yearMonthResult.monthPan[idx]}", step * 0.25f / density.density, Color(0xFF4CAF50))
                }
            }
        }

        // 坐山/向首箭头跟随排盘实际度数：result 非空时用精确度数，未排盘时回退到下拉选中的中心度。
        // 注意原点差异：度盘数字走 drawRotatedText（底边原点），而指针走 rotate（顶边原点），两者相差 180°，
        // 故「坐山度数」刻度一侧为坐山/箭尾、对侧为向首/箭头。
        val zuoDeg = if (result != null) result.zuoDeg else zuoIdx * 15f
        val xiangDeg = (zuoDeg + 180f) % 360f
        drawZuoArrow(R, R0, lw, xiangDeg)
        drawXiangArrow(R, R0, lw, zuoDeg)
    }
}

private fun DrawScope.compassToCanvas(cx: Float, cy: Float, r: Float, compassDeg: Float): Offset {
    val rad = Math.toRadians(compassDeg.toDouble()).toFloat()
    return Offset(cx + r * sin(rad), cy - r * cos(rad))
}

// 整数度走查表，与上面逐次计算取值相同。
private fun DrawScope.compassToCanvas(cx: Float, cy: Float, r: Float, compassDeg: Int): Offset =
    Offset(cx + r * COMPASS_SIN[compassDeg], cy - r * COMPASS_COS[compassDeg])

private fun DrawScope.drawRotatedText(
    textMeasurer: TextMeasurer,
    cache: LabelCache,
    compassAngle: Float,
    cx: Float, cy: Float,
    radius: Float,
    text: String,
    fontSp: Float,
    color: Color,
    arcLimit: Float = Float.MAX_VALUE
) {
    var layout = cache.getOrMeasure(textMeasurer, text, fontSp, color)
    // 0.95 视为横向缩放阈值：文字宽度超过 arcLimit * 0.95 时按同比例缩小字号
    val threshold = arcLimit * 0.95f
    if (layout.size.width > threshold) {
        layout = cache.getOrMeasure(textMeasurer, text, fontSp * (threshold / layout.size.width), color)
    }
    rotate(compassAngle, pivot = Offset(cx, cy)) {
        drawText(
            layout,
            topLeft = Offset(cx - layout.size.width / 2f, cy + radius - layout.size.height / 2f)
        )
    }
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    cache: LabelCache,
    x: Float, y: Float,
    text: String,
    fontSp: Float,
    color: Color
) {
    val layout = cache.getOrMeasure(textMeasurer, text, fontSp, color)
    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
}

// 箭尾（坐山侧）：偏移量按 R0 定尺，贴在半径 R 的环边外侧，环带放大时箭头本身不变大。
private fun DrawScope.drawZuoArrow(R: Float, R0: Float, lw: Float, compassDeg: Float) {
    val deltaDeg = 1.24f
    rotate(compassDeg, pivot = Offset(center.x, center.y)) {
        val shaftTop = center.y - (R + 0.019f * R0)
        val shaftBottom = center.y - R
        drawLine(Color.Black, Offset(center.x, shaftBottom), Offset(center.x, shaftTop), lw)

        val wingY = center.y - (R + 0.0625f * R0)
        val deltaRad = Math.toRadians(deltaDeg.toDouble()).toFloat()
        val wingDx = 1.0625f * R0 * sin(deltaRad)
        val notchY = center.y - (R + 0.044f * R0)

        val path = Path().apply {
            moveTo(center.x, shaftTop)
            lineTo(center.x - wingDx, wingY)
            lineTo(center.x, notchY)
            close()
        }
        drawPath(path, Color.Black, style = Fill)

        val path2 = Path().apply {
            moveTo(center.x, shaftTop)
            lineTo(center.x + wingDx, wingY)
            lineTo(center.x, notchY)
            close()
        }
        drawPath(path2, Color.Black, style = Fill)
    }
}

// 箭头（向首侧）：同箭尾，偏移量按 R0 定尺。
private fun DrawScope.drawXiangArrow(R: Float, R0: Float, lw: Float, compassDeg: Float) {
    val deltaDeg = 1.05f
    rotate(compassDeg, pivot = Offset(center.x, center.y)) {
        val tipY = center.y - (R + 0.0625f * R0)
        val baseY = center.y - (R + 0.025f * R0)
        drawLine(Color.Black, Offset(center.x, center.y - R), Offset(center.x, tipY), lw)

        val deltaRad = Math.toRadians(deltaDeg.toDouble()).toFloat()
        val baseDx = 1.025f * R0 * sin(deltaRad)

        val path = Path().apply {
            moveTo(center.x, tipY)
            lineTo(center.x - baseDx, baseY)
            lineTo(center.x + baseDx, baseY)
            close()
        }
        drawPath(path, Color.Black, style = Fill)
    }
}
