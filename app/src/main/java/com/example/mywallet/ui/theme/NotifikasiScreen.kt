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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.mywallet.data.BeritaSaham
import com.example.mywallet.data.RetrofitClient

@Composable
fun NotifikasiScreen(onBack: () -> Unit) {
    var beritaTampil by remember { mutableStateOf<List<BeritaSaham>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val sumberBerita = listOf(
        BeritaSaham(
            1,
            "Lorem ipsum",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            "OASA",
            "2026-06-27",
            999,
            "+50%",
            "https://finance.yahoo.com/quote/OASA.JK/"
        ),
        BeritaSaham(
            2,
            "Volume Produksi Meningkat",
            "Laporan terbaru menunjukkan peningkatan kapasitas operasional yang berdampak positif pada pergerakan saham hari ini.",
            "BRMS",
            "2026-06-26",
            185,
            "+15%",
            "https://finance.yahoo.com/quote/BRMS.JK/"
        ),
        BeritaSaham(
            3,
            "Harga Komoditas Dukung Margin",
            "Sektor energi mendapat sentimen positif dari pergerakan harga komoditas global.",
            "BUMI",
            "2026-06-26",
            120,
            "-5%",
            "https://finance.yahoo.com/quote/BUMI.JK/"
        )
    )

    LaunchedEffect(Unit) {
        try {
            val histori = RetrofitClient.instance.getHistori()
            val emitenUnik = histori.map { it.emiten.uppercase() }.distinct()
            beritaTampil = sumberBerita.filter { emitenUnik.contains(it.emiten.uppercase()) }
        } catch (e: Exception) {
            beritaTampil = sumberBerita
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
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RingColor)
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
                                val paragraf = teks.split("\n").filter { it.isNotBlank() }
                                val duaParagraf = paragraf.take(2).joinToString("\n\n")
                                if (duaParagraf.length > 100) duaParagraf.take(100)
                                    .trim() + "..." else if (paragraf.size > 2) duaParagraf + "..." else duaParagraf
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
                                    Text(
                                        text = "Price : ${berita.harga}",
                                        color = TextGray,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    val isPositif = berita.persentase.startsWith("+")
                                    Box(
                                        modifier = Modifier
                                            .width(65.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isPositif) Color(0xFF4ADE80) else Color(
                                                    0xFFEF4444
                                                )
                                            )
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = berita.persentase,
                                            color = if (isPositif) Color(0xFF064E3B) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
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
    }
}