package com.example.mywallet.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.DeviceIdHelper
import com.example.mywallet.R
import com.example.mywallet.StockPriceHelper
import com.example.mywallet.data.RetrofitClient
import com.example.mywallet.data.Transaksi
import kotlinx.coroutines.withContext

@Composable
fun RincianScreen(onNavigateToHome: () -> Unit, onNavigateToForm: () -> Unit) {
    var listTransaksi by remember { mutableStateOf<List<Transaksi>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val deviceId = DeviceIdHelper.getDeviceId(context)
            listTransaksi = RetrofitClient.instance.getHistori(deviceId)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = BgDark,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToForm,
                containerColor = RingColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.offset(y = 55.dp)
            ) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .fillMaxWidth()
                    .height(65.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(nvBar)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            painter = painterResource(id = R.drawable.home),
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(id = R.drawable.chartsvg),
                            contentDescription = "Rincian",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            if (listTransaksi.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada investasi\nKlik '+' untuk mulai",
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    items(listTransaksi, key = { it.id }) { transaksi ->
                        RincianCard(transaksi = transaksi)
                    }
                }
            }

        }
    }
}

@Composable
fun RincianCard(transaksi: Transaksi) {
    var hargaLive by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val hargaAvgFormatted = "Rp " + String.format("%,.2f", transaksi.harga).replace(',', '.')

    LaunchedEffect(transaksi.emiten) {
        try {
            val harga = withContext(kotlinx.coroutines.Dispatchers.IO) {
                StockPriceHelper.getHargaLive(transaksi.emiten)
            }
            hargaLive = harga
        } catch (e: Exception) {
            android.util.Log.e("CEK_HARGA", "Gagal: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .padding(horizontal = 24.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaksi.emiten,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = transaksi.tgl, color = TextGray, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .border(1.dp, RingColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${transaksi.lot} LOT",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.DarkGray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Average Price", color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hargaAvgFormatted,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Current Price", color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))

                if (isLoading) {
                    Text(
                        text = "Loading...",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    if (hargaLive == null) {
                        Text(
                            text = "-",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val liveFormatted =
                            "Rp " + String.format("%,.2f", hargaLive!!).replace(',', '.')

                        val priceColor = when {
                            hargaLive!! > transaksi.harga -> Color(0xFF4ADE80)
                            hargaLive!! < transaksi.harga -> Color(0xFFEF4444)
                            else -> Color.White
                        }

                        Text(
                            text = liveFormatted,
                            color = priceColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}