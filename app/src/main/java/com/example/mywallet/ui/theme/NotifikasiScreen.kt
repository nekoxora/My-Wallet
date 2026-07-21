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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.BeritaFilterHelper
import com.example.mywallet.DeviceIdHelper
import com.example.mywallet.StockPriceHelper
import com.example.mywallet.data.BeritaSaham
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@Composable
fun NotifikasiScreen(onBack: () -> Unit) {
    var bTampil by remember { mutableStateOf<List<BeritaSaham>>(emptyList()) }
    var search by remember { mutableStateOf("") };
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) };
    var trigger by remember { mutableIntStateOf(0) }
    val pullState = rememberPullToRefreshState();
    val ctx = LocalContext.current;
    val dens = LocalDensity.current;
    val prefs = ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val clrIds = prefs.getStringSet("cleared_berita_ids", emptySet()) ?: emptySet()
    LaunchedEffect(trigger) {
        try {
            if (trigger > 0) isRefreshing = true else isLoading = true
            val id = DeviceIdHelper.getDeviceId(ctx);
            val uE =
                RetrofitClient.instance.getHistori(id).map { it.emiten.uppercase().trim() }.toSet()
            val res = RetrofitClient.instance.getBerita()
            if (res.status == "success") {
                val filt = res.data.filter {
                    it.id !in clrIds && BeritaFilterHelper.isBeritaRelevant(
                        it,
                        uE
                    )
                }
                val cachedBiRate = StockPriceHelper.getBiRateValue()?.let { "$it%" } ?: "-"
                bTampil = coroutineScope {
                    filt.map { b ->
                        async(Dispatchers.IO) {
                            try {
                                val e = b.emiten.uppercase().trim()
                                if (e == "BI RATE") return@async b.copy(
                                    hargaStr = cachedBiRate,
                                    persentase = ""
                                )
                                val isG = BeritaFilterHelper.isGlobal(b.emiten);
                                val sym = StockPriceHelper.buildSymbol(b.emiten, isG)
                                var h = StockPriceHelper.getHargaLiveWithMeta(sym)
                                    ?.optDouble("regularMarketPrice");
                                val p = StockPriceHelper.getPersentaseLive(sym)
                                if (h != null && h < 1.0 && !isG) h = 1.0 / h
                                b.copy(harga = h?.toInt() ?: 0, persentase = p ?: "-")
                            } catch (e: Exception) {
                                b
                            }
                        }
                    }.awaitAll()
                }
            } else bTampil = emptyList()
            if (trigger > 0) delay(800)
            isRefreshing = false
        } catch (e: Exception) {
            bTampil = emptyList()
        } finally {
            isLoading = false; isRefreshing = false
        }
    }
    val filtered = remember(
        bTampil,
        search
    ) {
        if (search.isEmpty()) bTampil else bTampil.filter {
            it.judul.contains(
                search,
                true
            ) || it.isi.contains(search, true) || it.emiten.contains(search, true)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search for news", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor,
                unfocusedBorderColor = RingColor.copy(alpha = 0.5f),
                cursorColor = Color.White
            ),
            singleLine = true
        )
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
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { trigger++ },
                state = pullState,
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
                indicator = {
                    if (isRefreshing) {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }) {
                val drag =
                    if (isRefreshing || pullState.isAnimating) 0f else with(dens) { pullState.distanceFraction * 80.dp.toPx() }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = drag }) {
                    items(filtered) { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (b.url.isNotEmpty()) try {
                                        ctx.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(b.url)
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Link tidak valid", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = b.judul,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ); Spacer(modifier = Modifier.height(6.dp))
                                val isi = b.isi.replace(Regex("<[^>]*>"), "").split("\n")
                                    .filter { it.isNotBlank() }.take(2).joinToString("\n\n").let {
                                    if (it.length > 100) it.take(100).trim() + "..." else it
                                }
                                Text(
                                    text = isi,
                                    color = TextGray,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                ); Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = b.emiten,
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
                                        val displayPrice = if (b.emiten.uppercase()
                                                .trim() == "BI RATE"
                                        ) b.hargaStr else if (b.harga > 0) "${b.harga}" else null
                                        if (displayPrice != null) {
                                            Text(
                                                text = "Price : $displayPrice",
                                                color = TextGray,
                                                fontSize = 14.sp
                                            ); Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        if (b.persentase != "-" && b.persentase.isNotEmpty()) {
                                            val isP = b.persentase.startsWith("+"); Box(
                                                modifier = Modifier
                                                    .width(
                                                        65.dp
                                                    )
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isP) Color(0xFF4ADE80) else Color(
                                                            0xFFEF4444
                                                        )
                                                    )
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = b.persentase,
                                                    color = if (isP) Color(0xFF064E3B) else Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(text = b.tgl, color = TextGray, fontSize = 14.sp)
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
                ) { Text(text = "Back", fontSize = 16.sp) }
                OutlinedButton(
                    onClick = {
                        val cur = bTampil.map { it.id }.toSet(); prefs.edit()
                        .putStringSet("cleared_berita_ids", clrIds + cur).apply(); bTampil =
                        emptyList()
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
                ) { Text(text = "Clear All", fontSize = 16.sp) }
            }
        }
    }
}
