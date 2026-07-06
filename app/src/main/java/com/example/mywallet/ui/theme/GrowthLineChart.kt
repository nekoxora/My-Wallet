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

    val density = LocalDensity.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val indonesianMonths = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")

    val textPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#A0A0B5")
        textSize = with(density) { 8.sp.toPx() }
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val tooltipPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = with(density) { 12.sp.toPx() }
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 16.dp)
            .pointerInput(dataPoints, maxPoints) {
                val paddingLeft = 60.dp.toPx()
                val chartWidth = size.width - paddingLeft

                fun updateIndex(x: Float) {
                    val relativeX = x - paddingLeft
                    if (relativeX in 0f..chartWidth) {
                        val index = (relativeX / chartWidth * (maxPoints - 1)).toInt()
                        selectedIndex = index.coerceIn(0, dataPoints.size - 1)
                    } else {
                        selectedIndex = null
                    }
                }

                detectTapGestures(
                    onPress = { 
                        updateIndex(it.x)
                        tryAwaitRelease()
                        selectedIndex = null
                    },
                    onTap = { selectedIndex = null }
                )
            }
            .pointerInput(dataPoints, maxPoints) {
                val paddingLeft = 60.dp.toPx()
                val chartWidth = size.width - paddingLeft

                detectDragGestures(
                    onDragStart = { offset ->
                        val relativeX = offset.x - paddingLeft
                        if (relativeX in 0f..chartWidth) {
                            val index = (relativeX / chartWidth * (maxPoints - 1)).toInt()
                            selectedIndex = index.coerceIn(0, dataPoints.size - 1)
                        }
                    },
                    onDrag = { change, _ ->
                        val relativeX = change.position.x - paddingLeft
                        if (relativeX in 0f..chartWidth) {
                            val index = (relativeX / chartWidth * (maxPoints - 1)).toInt()
                            selectedIndex = index.coerceIn(0, dataPoints.size - 1)
                        } else {
                            selectedIndex = null
                        }
                    },
                    onDragEnd = { selectedIndex = null },
                    onDragCancel = { selectedIndex = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingLeft = 60.dp.toPx()
            val paddingBottom = 30.dp.toPx()
            val paddingTop = 10.dp.toPx()
            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom - paddingTop

            val minVal = 0.0 
            val maxVal = (dataPoints.maxOrNull() ?: 0.0) * 1.1
            val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

            val gridCount = 5
            for (i in 0..gridCount) {
                val y = paddingTop + chartHeight - (i * chartHeight / gridCount)
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.2f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                val labelVal = minVal + (i * range / gridCount)
                val formattedLabel = formatPriceLabel(labelVal)
                drawContext.canvas.nativeCanvas.drawText(
                    formattedLabel,
                    5.dp.toPx(),
                    y + 3.dp.toPx(),
                    textPaint.apply { textAlign = Paint.Align.LEFT }
                )
            }

            if (chartType == ChartType.MONTHLY) {
                for (day in 1..maxPoints) {
                    val x = paddingLeft + ((day - 1) * chartWidth / (maxPoints - 1).coerceAtLeast(1))
                    if (day == 1 || day == 8 || day == 15 || day == 22 || day == 29 || day == maxPoints) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "$day",
                            x,
                            height - 5.dp.toPx(),
                            textPaint.apply { textAlign = Paint.Align.CENTER }
                        )
                    }
                }
            } else {
                for (i in 0 until 12) {
                    val x = paddingLeft + (i * chartWidth / 11f)
                    drawContext.canvas.nativeCanvas.drawText(
                        indonesianMonths[i],
                        x,
                        height - 5.dp.toPx(),
                        textPaint.apply { textAlign = Paint.Align.CENTER }
                    )
                }
            }

            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                val x = paddingLeft + (index * chartWidth / (maxPoints - 1).coerceAtLeast(1))
                val y = paddingTop + chartHeight - ((value - minVal) / range * chartHeight).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Color(0xFF06B6D4),
                style = Stroke(width = 2.5.dp.toPx())
            )

            selectedIndex?.let { index ->
                if (index < dataPoints.size) {
                    val x = paddingLeft + (index * chartWidth / (maxPoints - 1).coerceAtLeast(1))
                    val value = dataPoints[index]
                    val y = paddingTop + chartHeight - ((value - minVal) / range * chartHeight).toFloat()

                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(x, paddingTop),
                        end = Offset(x, height - paddingBottom),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawCircle(color = Color(0xFF06B6D4), radius = 5.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))

                    val localeId = Locale.forLanguageTag("id-ID")
                    val labelPrefix = if (chartType == ChartType.MONTHLY) "${index + 1} $monthName" else indonesianMonths[index]
                    val tooltipText = "$labelPrefix: Rp${String.format(localeId, "%,d", value.toLong()).replace(',', '.')}"
                    
                    val tooltipWidth = tooltipPaint.measureText(tooltipText) + 20.dp.toPx()
                    val tooltipHeight = 35.dp.toPx()
                    var tooltipX = (x - (tooltipWidth / 2)).coerceIn(paddingLeft, width - tooltipWidth)
                    val tooltipY = (y - tooltipHeight - 10.dp.toPx()).coerceAtLeast(5.dp.toPx())

                    drawRoundRect(
                        color = Color(0xFF1F1F40),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        alpha = 0.95f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        tooltipText,
                        tooltipX + (tooltipWidth / 2),
                        tooltipY + (tooltipHeight / 2) + 5.dp.toPx(),
                        tooltipPaint
                    )
                }
            }
        }
    }
}

private fun formatPriceLabel(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "Rp${(value / 1_000_000_000).toInt()}B"
        value >= 1_000_000 -> "Rp${(value / 1_000_000).toInt()}M"
        value >= 1_000 -> "Rp${(value / 1_000).toInt()}K"
        else -> "Rp${value.toInt()}"
    }
}
