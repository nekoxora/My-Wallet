package com.example.mywallet.ui.theme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class ChartType { MONTHLY, YEARLY }

@Composable
fun GrowthLineChart(
    dataPoints: List<Double>,
    chartType: ChartType = ChartType.MONTHLY,
    maxPoints: Int = 30,
    monthName: String = "",
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return
    val dens = LocalDensity.current;
    var selIdx by remember { mutableStateOf<Int?>(null) }
    val idMonths =
        listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
    val txtP = Paint().apply {
        color = android.graphics.Color.parseColor("#A0A0B5"); textSize =
        with(dens) { 8.sp.toPx() }; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    val tipP = Paint().apply {
        color = android.graphics.Color.WHITE; textSize = with(dens) { 12.sp.toPx() }; typeface =
        Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 16.dp)
            .pointerInput(dataPoints, maxPoints) {
                val padL = 60.dp.toPx();
                val cW = size.width - padL; fun upd(x: Float) {
                val relX = x - padL; if (relX in 0f..cW) {
                    val idx = (relX / cW * (maxPoints - 1)).toInt(); selIdx =
                        idx.coerceIn(0, dataPoints.size - 1)
                } else {
                    selIdx = null
                }
            }; detectTapGestures(
                onPress = { upd(it.x); tryAwaitRelease(); selIdx = null },
                onTap = { selIdx = null })
            }
            .pointerInput(dataPoints, maxPoints) {
                val padL = 60.dp.toPx();
                val cW = size.width - padL; detectDragGestures(onDragStart = {
                val relX = it.x - padL; if (relX in 0f..cW) {
                val idx = (relX / cW * (maxPoints - 1)).toInt(); selIdx =
                    idx.coerceIn(0, dataPoints.size - 1)
            }
            }, onDrag = { change, _ ->
                val relX = change.position.x - padL; if (relX in 0f..cW) {
                val idx = (relX / cW * (maxPoints - 1)).toInt(); selIdx =
                    idx.coerceIn(0, dataPoints.size - 1)
            } else {
                selIdx = null
            }
            }, onDragEnd = { selIdx = null }, onDragCancel = { selIdx = null })
            }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width;
            val h = size.height;
            val pL = 60.dp.toPx();
            val pB = 30.dp.toPx();
            val pT = 10.dp.toPx();
            val cW = w - pL;
            val cH = h - pB - pT
            val maxV = (dataPoints.maxOrNull() ?: 0.0) * 1.1;
            val rng = if (maxV == 0.0) 1.0 else maxV
            for (i in 0..5) {
                val y = pT + cH - (i * cH / 5); drawLine(
                    Color.DarkGray.copy(0.2f),
                    Offset(pL, y),
                    Offset(w, y),
                    1.dp.toPx()
                ); drawContext.canvas.nativeCanvas.drawText(
                    formatPrice(i * rng / 5),
                    5.dp.toPx(),
                    y + 3.dp.toPx(),
                    txtP.apply { textAlign = Paint.Align.LEFT })
            }
            if (chartType == ChartType.MONTHLY) {
                for (d in 1..maxPoints) {
                    val x = pL + ((d - 1) * cW / (maxPoints - 1).coerceAtLeast(1)); if (d in listOf(
                            1,
                            8,
                            15,
                            22,
                            29,
                            maxPoints
                        )
                    ) drawContext.canvas.nativeCanvas.drawText(
                        "$d",
                        x,
                        h - 5.dp.toPx(),
                        txtP.apply { textAlign = Paint.Align.CENTER })
                }
            } else {
                for (i in 0 until 12) {
                    val x = pL + (i * cW / 11f); drawContext.canvas.nativeCanvas.drawText(
                        idMonths[i],
                        x,
                        h - 5.dp.toPx(),
                        txtP.apply { textAlign = Paint.Align.CENTER })
                }
            }
            val path = Path(); dataPoints.forEachIndexed { i, v ->
            val x = pL + (i * cW / (maxPoints - 1).coerceAtLeast(1));
            val y = pT + cH - (v / rng * cH).toFloat(); if (i == 0) path.moveTo(
            x,
            y
        ) else path.lineTo(x, y)
        }
            drawPath(path, Color(0xFF06B6D4), style = Stroke(2.5.dp.toPx()))
            if (dataPoints.size > 1) {
                val fP = Path().apply {
                    addPath(path); lineTo(
                    pL + ((dataPoints.size - 1) * cW / (maxPoints - 1).coerceAtLeast(1)), pT + cH
                ); lineTo(pL, pT + cH); close()
                }; drawPath(
                    fP,
                    Brush.verticalGradient(
                        listOf(Color(0xFF06B6D4).copy(0.3f), Color.Transparent),
                        pT,
                        pT + cH
                    )
                )
            }
            selIdx?.let { i ->
                if (i < dataPoints.size) {
                    val x = pL + (i * cW / (maxPoints - 1).coerceAtLeast(1));
                    val v = dataPoints[i];
                    val y = pT + cH - (v / rng * cH).toFloat()
                    drawLine(
                        Color.White.copy(0.4f),
                        Offset(x, pT),
                        Offset(x, h - pB),
                        1.dp.toPx()
                    ); drawCircle(
                        Color(0xFF06B6D4),
                        5.dp.toPx(),
                        Offset(x, y)
                    ); drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                    val pfx =
                        if (chartType == ChartType.MONTHLY) "${i + 1} $monthName" else idMonths[i];
                    val txt =
                        "$pfx: Rp${String.format(Locale.US, "%,d", v.toLong()).replace(',', '.')}"
                    val tW = tipP.measureText(txt) + 20.dp.toPx();
                    val tH = 35.dp.toPx();
                    val tX = (x - (tW / 2)).coerceIn(pL, w - tW);
                    val tY = (y - tH - 10.dp.toPx()).coerceAtLeast(5.dp.toPx())
                    drawRoundRect(
                        Color(0xFF1F1F40),
                        Offset(tX, tY),
                        Size(tW, tH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        alpha = 0.95f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        txt,
                        tX + (tW / 2),
                        tY + (tH / 2) + 5.dp.toPx(),
                        tipP
                    )
                }
            }
        }
    }
}

private fun formatPrice(v: Double): String = when {
    v >= 1e9 -> "Rp${(v / 1e9).toInt()}B"; v >= 1e6 -> "Rp${(v / 1e6).toInt()}M"; v >= 1e3 -> "Rp${(v / 1e3).toInt()}K"; else -> "Rp${v.toInt()}"
}
