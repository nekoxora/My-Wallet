package com.example.mywallet.ui.theme

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.mywallet.BeritaFilterHelper
import com.example.mywallet.DeviceIdHelper
import com.example.mywallet.R
import com.example.mywallet.StockPriceHelper
import com.example.mywallet.data.DeleteData
import com.example.mywallet.data.RetrofitClient
import com.example.mywallet.data.Transaksi
import com.example.mywallet.data.saveImageToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IconNotification(jumlahNotif: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.bell),
            contentDescription = "Notifications",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        if (jumlahNotif > 0) {
            val txt = if (jumlahNotif > 99) "99+" else jumlahNotif.toString()
            val w = if (jumlahNotif > 99) 30.dp else if (jumlahNotif > 9) 20.dp else 16.dp
            Box(
                modifier = Modifier
                    .size(width = w, height = 16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red)
                    .border(1.dp, BgDark, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = txt,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 8.sp
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToRincian: () -> Unit,
    onNavigateToNotifikasi: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    var listTransaksi by remember { mutableStateOf<List<Transaksi>>(emptyList()) }
    var hargaLiveMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val pullState = rememberPullToRefreshState();
    val scp = rememberCoroutineScope();
    val ctx = LocalContext.current;
    val dens = LocalDensity.current;
    val prefs = ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val grouped by remember(listTransaksi) {
        derivedStateOf {
            listTransaksi.groupBy { it.emiten.uppercase() }
                .map { (e, txs) -> txs.first().copy(emiten = e, lot = txs.sumOf { it.lot }) }
        }
    }
    var jumlahNotif by remember { mutableIntStateOf(0) };
    var relevantNotifIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var profilePath by remember { mutableStateOf(prefs.getString("profile_path", null)) }
    val gallery =
        rememberLauncherForActivityResult(contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            uri?.let {
                val p = saveImageToInternalStorage(ctx, it); if (p != null) {
                profilePath = p; prefs.edit().putString("profile_path", p).apply()
            }
            }
        }
    var name by remember { mutableStateOf(prefs.getString("nama_user", "User") ?: "User") };
    var showEdit by remember { mutableStateOf(false) };
    var input by remember { mutableStateOf("") }
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            containerColor = CardDark,
            title = {
                Text(
                    "Ubah Nama",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Nama baru", color = TextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RingColor,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = RingColor,
                        unfocusedLabelColor = TextGray,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(15.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (input.isNotBlank()) {
                        name = input; prefs.edit().putString("nama_user", input).apply()
                    }; showEdit = false
                }) { Text("Simpan", color = RingColor) }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) {
                    Text(
                        "Batal",
                        color = TextGray
                    )
                }
            })
    }
    LaunchedEffect(refreshTrigger) {
        try {
            if (refreshTrigger > 0) isRefreshing = true
            val id = DeviceIdHelper.getDeviceId(ctx); listTransaksi =
                RetrofitClient.instance.getHistori(id);
            val uni = listTransaksi.map { it.emiten.uppercase() }.distinct()
            hargaLiveMap = coroutineScope {
                uni.map { e ->
                    async(Dispatchers.IO) {
                        StockPriceHelper.getHargaLive(e) to e
                    }
                }.awaitAll().filter { it.first != null }.associate { it.second to it.first!! }
            }
            try {
                val res = RetrofitClient.instance.getBerita()
                if (res.status == "success") {
                    val clr = prefs.getStringSet("cleared_berita_ids", emptySet()) ?: emptySet();
                    val rd = prefs.getStringSet("notif_id_terbaca", emptySet()) ?: emptySet();
                    val userE = listTransaksi.map { it.emiten.uppercase().trim() }.toSet()
                    val rel = res.data.filter {
                        it.id !in clr && BeritaFilterHelper.isBeritaRelevant(
                            it,
                            userE
                        )
                    }
                    relevantNotifIds = rel.map { it.id }.toSet(); jumlahNotif =
                        rel.count { it.id !in rd }
                    val all = res.data.map { it.id }.toSet();
                    val upd =
                        (prefs.getStringSet("notif_id_terkirim", emptySet()) ?: emptySet()) + all
                    prefs.edit().putStringSet("notif_id_terkirim", upd).apply()
                }
            } catch (_: Exception) {
            }
            if (refreshTrigger > 0) delay(800)
            isRefreshing = false
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal", Toast.LENGTH_SHORT).show()
        } finally {
            isRefreshing = false
        }
    }
    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToForm,
                containerColor = RingColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.offset(y = 55.dp)
            ) { Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        },
        floatingActionButtonPosition = FabPosition.Center,
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
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(id = R.drawable.home),
                            contentDescription = "Home",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(25.dp)
                        )
                    }; IconButton(onClick = onNavigateToRincian) {
                    Icon(
                        painter = painterResource(id = R.drawable.chartsvg),
                        contentDescription = "Rincian",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                }
            }
        }) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = if (profilePath != null) rememberAsyncImagePainter(
                                java.io.File(
                                    profilePath!!
                                )
                            ) else painterResource(id = R.drawable.profile),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, Purple40, CircleShape)
                                .clickable { gallery.launch("image/*") })
                        Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.clickable {
                        input = name; showEdit = true
                    }) {
                        Text("Welcome back!", color = TextGray, fontSize = 13.sp); Text(
                        name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    }
                    }
                    IconNotification(
                        jumlahNotif = jumlahNotif,
                        onClick = {
                            val upd = (prefs.getStringSet("notif_id_terbaca", emptySet())
                                ?: emptySet()) + relevantNotifIds; prefs.edit()
                            .putStringSet("notif_id_terbaca", upd).apply(); jumlahNotif =
                            0; onNavigateToNotifikasi()
                        })
                }
                Spacer(modifier = Modifier.height(50.dp)); Box(
                modifier = Modifier.fillMaxWidth(),
                Alignment.Center
            ) { CustomDonutChart(listTransaksi, hargaLiveMap) }
                Spacer(modifier = Modifier.height(40.dp)); Text(
                "My Investment",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ); Spacer(modifier = Modifier.height(16.dp))
                PullToRefreshBox(
                    isRefreshing,
                    { refreshTrigger++ },
                    state = pullState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    indicator = {
                        if (isRefreshing) PullToRefreshDefaults.Indicator(
                            pullState,
                            isRefreshing,
                            Modifier.align(Alignment.TopCenter)
                        )
                    }) {
                    val drag =
                        if (isRefreshing || pullState.isAnimating) 0f else with(dens) { pullState.distanceFraction * 80.dp.toPx() }; LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = drag },
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(grouped, key = { it.emiten }) { tx ->
                        SwipeableInvestmentCard(
                            tx,
                            onDelete = {
                                scp.launch {
                                    try {
                                        val id =
                                            DeviceIdHelper.getDeviceId(ctx); if (RetrofitClient.instance.hapusInvestasi(
                                                DeleteData(id, tx.emiten)
                                            ).status == "success"
                                        ) {
                                            listTransaksi =
                                                RetrofitClient.instance.getHistori(id); Toast.makeText(
                                                ctx,
                                                "Dihapus",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                    }
                }
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) { DraggableBotIcon { onNavigateToChat() } }
        }
    }
}
