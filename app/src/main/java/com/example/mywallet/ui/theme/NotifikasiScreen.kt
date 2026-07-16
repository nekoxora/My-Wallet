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
    var beritaTampil by remember { mutableStateOf<List<BeritaSaham>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val pullState = rememberPullToRefreshState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val clearedBeritaIds = prefs.getStringSet("cleared_berita_ids", emptySet()) ?: emptySet()
    LaunchedEffect(refreshTrigger) {
        try {
            if (refreshTrigger > 0) isRefreshing = true else isLoading = true
            val deviceId = DeviceIdHelper.getDeviceId(context)
            val historiResponse = RetrofitClient.instance.getHistori(deviceId)
            val userEmitens = historiResponse.map { it.emiten.uppercase().trim() }.toSet()
            val response = RetrofitClient.instance.getBerita()
            if (response.status == "success") {
                val listFiltered = response.data.filter {
                    it.id !in clearedBeritaIds && BeritaFilterHelper.isBeritaRelevant(
                        it,
                        userEmitens
                    )
                }
                beritaTampil = coroutineScope {
                    listFiltered.map { berita ->
                        async(Dispatchers.IO) {
                            try {
                                val symbol = StockPriceHelper.buildSymbol(berita.emiten);
                                var h = StockPriceHelper.getHargaLive(symbol);
                                val p =
                                    StockPriceHelper.getPersentaseLive(symbol); if (h != null && h < 1.0) h =
                                    1.0 / h; berita.copy(
                                    harga = h?.toInt() ?: 0,
                                    persentase = p ?: "-"
                                )
                            } catch (e: Exception) {
                                berita
                            }
                        }
                    }.awaitAll()
                }
            } else {
                beritaTampil = emptyList()
            }
            if (refreshTrigger > 0) delay(800)
            isRefreshing = false
        } catch (e: Exception) {
            beritaTampil = emptyList()
        } finally {
            isLoading = false; isRefreshing = false
        }
    }
    val filteredBerita = remember(
        beritaTampil,
        searchQuery
    ) {
        if (searchQuery.isEmpty()) beritaTampil else beritaTampil.filter {
            it.judul.contains(
                searchQuery,
                ignoreCase = true
            ) || it.isi.contains(searchQuery, ignoreCase = true) || it.emiten.contains(
                searchQuery,
                ignoreCase = true
            )
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
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search for news", color = TextGray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextGray
                )
            },
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
                onRefresh = { refreshTrigger++ },
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
                val dragAmount =
                    if (isRefreshing || pullState.isAnimating) 0f else with(density) { pullState.distanceFraction * 80.dp.toPx() }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = dragAmount }) {
                    items(filteredBerita) { berita ->
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
                                }) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = berita.judul,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val isi = berita.isi.replace(Regex("<[^>]*>"), "").split("\n")
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
                                            ); Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        if (berita.persentase != "-" && berita.persentase.isNotEmpty()) {
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
                ) { Text("Back", fontSize = 16.sp) }
                OutlinedButton(
                    onClick = {
                        val currentIds = beritaTampil.map { it.id }.toSet();
                        val newClearedIds = (prefs.getStringSet("cleared_berita_ids", emptySet())
                            ?: emptySet()) + currentIds; prefs.edit()
                        .putStringSet("cleared_berita_ids", newClearedIds).apply(); beritaTampil =
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
                ) { Text("Clear All", fontSize = 16.sp) }
            }
        }
    }
}
