package com.example.mywallet.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.Transaksi

@Composable
fun CustomDonutChart(
    listTransaksi: List<Transaksi>,
    hargaLiveMap: Map<String, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val chartColors = List(50) { index ->
        Color.hsl(
            hue = (index * 137.508f) % 360f,
            saturation = 0.55f + (index % 7) * 0.07f,
            lightness = 0.45f + (index % 5) * 0.08f,
            alpha = 1f
        )
    }

    val emitenValueMap = mutableMapOf<String, Long>()
    listTransaksi.forEach { transaksi ->
        val key = transaksi.emiten.uppercase()
        val currentValue = emitenValueMap[key] ?: 0L
        val hargaDigunakan = hargaLiveMap[key] ?: transaksi.harga
        val nilaiRupiah = (transaksi.lot.toDouble() * 100.0 * hargaDigunakan).toLong()
        emitenValueMap[key] = currentValue + nilaiRupiah
    }
    val totalSemuaAset = emitenValueMap.values.sum()

    var selectedEmiten by remember { mutableStateOf<String?>(null) }

    val centerText = if (selectedEmiten != null) {
        val nilaiSelected = emitenValueMap[selectedEmiten] ?: 0L
        "Rp " + String.format("%,d", nilaiSelected).replace(',', '.')
    } else {
        "Rp " + String.format("%,d", totalSemuaAset).replace(',', '.')
    }

    val labelText = if (selectedEmiten != null) selectedEmiten!! else "Total Aset"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(emitenValueMap) {
                    detectTapGestures { tapOffset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val minDim = minOf(canvasWidth, canvasHeight)
                        val radius = minDim / 2f
                        val strokeWidth = 75f
                        val innerRadius = radius - (strokeWidth / 2f)
                        val outerRadius = radius + (strokeWidth / 2f)
                        if (dist in innerRadius..outerRadius) {
                            var tapAngle =
                                Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
                                    .toFloat()
                            var normalizedTap = (tapAngle + 90f) % 360f
                            if (normalizedTap < 0) normalizedTap += 360f

                            var currentAngle = 0f
                            var hitEmiten: String? = null

                            for ((emiten, nilai) in emitenValueMap) {
                                val sweepAngle = (nilai.toFloat() / totalSemuaAset.toFloat()) * 360f
                                if (normalizedTap >= currentAngle && normalizedTap <= currentAngle + sweepAngle) {
                                    hitEmiten = emiten
                                    break
                                }
                                currentAngle += sweepAngle
                            }

                            selectedEmiten = if (selectedEmiten == hitEmiten) null else hitEmiten
                        } else {
                            selectedEmiten = null
                        }
                    }
                }
        ) {
            if (totalSemuaAset == 0L) {
                drawArc(
                    color = Color(0xFF3D3D7A), startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, style = Stroke(width = 75f, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f
                var index = 0

                emitenValueMap.forEach { (emiten, nilai) ->
                    val sweepAngle = (nilai.toFloat() / totalSemuaAset.toFloat()) * 360f
                    val color = chartColors[index % chartColors.size]

                    val isSelected = emiten == selectedEmiten
                    val strokeStyle = if (isSelected) {
                        Stroke(width = 85f, cap = StrokeCap.Butt)
                    } else {
                        Stroke(width = 75f, cap = StrokeCap.Butt)
                    }

                    drawArc(
                        color = if (selectedEmiten == null || isSelected) color else color.copy(
                            alpha = 0.3f
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = strokeStyle
                    )

                    startAngle += sweepAngle
                    index++
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = labelText,
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = centerText,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}