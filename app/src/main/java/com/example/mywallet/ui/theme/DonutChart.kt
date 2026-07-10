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
    val colors = List(50) { i ->
        Color.hsl(
            (i * 137.508f) % 360f,
            0.55f + (i % 7) * 0.07f,
            0.45f + (i % 5) * 0.08f
        )
    }
    val map = mutableMapOf<String, Long>(); listTransaksi.forEach { tx ->
        val k = tx.emiten.uppercase(); map[k] =
        (map[k] ?: 0L) + (tx.lot.toDouble() * 100.0 * (hargaLiveMap[k] ?: tx.harga)).toLong()
    }
    val total = map.values.sum();
    var sel by remember { mutableStateOf<String?>(null) }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(220.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(map) {
                    detectTapGestures { tap ->
                        val ctr = Offset(size.width / 2f, size.height / 2f);
                        val d =
                            kotlin.math.sqrt(((tap.x - ctr.x) * (tap.x - ctr.x) + (tap.y - ctr.y) * (tap.y - ctr.y)).toDouble())
                                .toFloat();
                        val r =
                            minOf(
                                size.width,
                                size.height
                            ) / 2f; if (d in (r - 37.5f)..(r + 37.5f)) {
                        var a = Math.toDegrees(
                            kotlin.math.atan2(
                                (tap.y - ctr.y).toDouble(),
                                (tap.x - ctr.x).toDouble()
                            )
                        ).toFloat();
                        var n = (a + 90f) % 360f; if (n < 0) n += 360f;
                        var cur = 0f;
                        var hit: String? = null; if (total > 0) {
                            for ((e, v) in map) {
                                val sw =
                                    (v.toFloat() / total.toFloat()) * 360f; if (n >= cur && n <= cur + sw) {
                                    hit = e; break
                                }; cur += sw
                            }
                        }; sel = if (sel == hit) null else hit
                    } else sel = null
                    }
                }) {
            if (total == 0L) drawArc(
                Color(0xFF3D3D7A),
                -90f,
                360f,
                false,
                style = Stroke(75f, cap = StrokeCap.Round)
            )
            else {
                var st = -90f; map.forEach { (e, v) ->
                    val sw = (v.toFloat() / total.toFloat()) * 360f;
                    val isS = e == sel; drawArc(
                    if (sel == null || isS) colors[map.keys.indexOf(e) % colors.size] else colors[map.keys.indexOf(
                        e
                    ) % colors.size].copy(0.3f),
                    st,
                    sw,
                    false,
                    style = Stroke(if (isS) 85f else 75f, cap = StrokeCap.Butt)
                ); st += sw
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                sel ?: "Total Aset",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ); Spacer(Modifier.height(4.dp)); Text(
            "Rp " + String.format(
                "%,d",
                if (sel != null) map[sel] ?: 0L else total
            ).replace(',', '.'), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        }
    }
}
