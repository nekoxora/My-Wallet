package com.example.mywallet.ui.theme

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.StockPriceHelper
import com.example.mywallet.data.BeritaSaham
import com.example.mywallet.data.RetrofitClient

fun toYahooSymbol(emiten: String): String {
    return when (emiten.uppercase().trim()) {
        "IHSG" -> "^JKSE"
        "GOLD" -> ""
        "WTI OIL" -> "CL=F"
        "COAL" -> ""
        "USD/IDR" -> ""
        "DJIA" -> "^DJI"
        "NASDAQ" -> "^IXIC"
        "S&P500" -> "^GSPC"
        "BI RATE" -> ""
        "BIG CAPS" -> ""
        else -> "${emiten.uppercase().trim()}.JK"
    }
}

@Composable
fun NotifikasiScreen(onBack: () -> Unit) {
    var beritaTampil by remember { mutableStateOf<List<BeritaSaham>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    val isCleared = prefs.getBoolean("berita_cleared", false)
    android.util.Log.d("NOTIF_DEBUG", "isCleared = $isCleared")

    LaunchedEffect(Unit) {
        android.util.Log.d("NOTIF_DEBUG", "LaunchedEffect jalan, isCleared = $isCleared")
        if (isCleared) {
            android.util.Log.d("NOTIF_DEBUG", "SKIP fetch karena cleared")
            beritaTampil = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
            val response = RetrofitClient.instance.getBerita()

            if (response.status == "success") {
                val listDenganHarga = response.data.map { berita ->
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val symbol = toYahooSymbol(berita.emiten)

                            if (symbol.isEmpty()) {
                                return@withContext berita
                            }

                            var hargaLive = StockPriceHelper.getHargaLive(symbol)
                            val persentaseLive = StockPriceHelper.getPersentaseLive(symbol)

                            if (hargaLive != null && hargaLive < 1.0) {
                                hargaLive = 1.0 / hargaLive
                            }

                            berita.copy(
                                harga = hargaLive?.toInt() ?: 0,
                                persentase = persentaseLive ?: "-"
                            )
                        } catch (e: Exception) {
                            berita
                        }
                    }
                }
                beritaTampil = listDenganHarga
            } else {
                beritaTampil = emptyList()
            }
        } catch (e: Exception) {
            beritaTampil = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp)
    ) {

        if (isLoading) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(beritaTampil) { berita ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (berita.url.isNotEmpty()) {
                                    try {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(berita.url)
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Link tidak valid",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Text(
                                text = berita.judul,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val isiDipotong = berita.isi.let { teks ->
                                val teksBersih = teks.replace(Regex("<[^>]*>"), "")
                                val paragraf = teksBersih.split("\n").filter { it.isNotBlank() }
                                val duaParagraf = paragraf.take(2).joinToString("\n\n")
                                when {
                                    duaParagraf.length > 100 -> duaParagraf.take(100).trim() + "..."
                                    paragraf.size > 2 -> "$duaParagraf..."
                                    else -> duaParagraf
                                }
                            }
                            Text(
                                text = isiDipotong,
                                color = TextGray,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(15.dp))

                            Text(
                                text = berita.emiten,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (berita.harga > 0) {
                                        Text(
                                            text = "Price : ${berita.harga}",
                                            color = TextGray,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    if (berita.persentase != "-" && berita.persentase.isNotEmpty()) {
                                        val isPositif = berita.persentase.startsWith("+")
                                        Box(
                                            modifier = Modifier
                                                .width(65.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isPositif) Color(0xFF4ADE80)
                                                    else Color(0xFFEF4444)
                                                )
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = berita.persentase,
                                                color = if (isPositif) Color(0xFF064E3B)
                                                else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(text = berita.tgl, color = TextGray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = RingColor
                ),
                border = BorderStroke(1.dp, RingColor)
            ) {
                Text("Back", fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = {
                    android.util.Log.d("NOTIF_DEBUG", "Clear All DIKLIK")
                    beritaTampil = emptyList()
                    val berhasil = prefs.edit().putBoolean("berita_cleared", true).commit()
                    android.util.Log.d("NOTIF_DEBUG", "Simpan ke prefs berhasil = $berhasil")
                    android.util.Log.d(
                        "NOTIF_DEBUG",
                        "Cek ulang prefs = ${prefs.getBoolean("berita_cleared", false)}"
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFEF4444)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Text("Clear All", fontSize = 16.sp)
            }
        }
    }
}