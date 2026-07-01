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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.mywallet.BeritaFilterHelper
import com.example.mywallet.DeviceIdHelper
import com.example.mywallet.NotificationHelper
import com.example.mywallet.R
import com.example.mywallet.StockPriceHelper
import com.example.mywallet.data.DeleteData
import com.example.mywallet.data.RetrofitClient
import com.example.mywallet.data.Transaksi
import com.example.mywallet.data.saveImageToInternalStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun IconNotification(
    jumlahNotif: Int,
    onClick: () -> Unit
) {
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
            val badgeText = if (jumlahNotif > 99) "99+" else jumlahNotif.toString()
            val badgeWidth = if (jumlahNotif > 99) 30.dp else if (jumlahNotif > 9) 20.dp else 16.dp

            Box(
                modifier = Modifier
                    .size(width = badgeWidth, height = 16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red)
                    .border(1.dp, BgDark, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
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
    onNavigateToNotifikasi: () -> Unit
) {
    var listTransaksi by remember { mutableStateOf<List<Transaksi>>(emptyList()) }
    var hargaLiveMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val groupedTransactions by remember(listTransaksi) {
        derivedStateOf {
            listTransaksi.groupBy { it.emiten.uppercase() }
                .map { (emiten, transactions) ->
                    val totalLot = transactions.sumOf { it.lot }
                    transactions.first().copy(
                        emiten = emiten,
                        lot = totalLot
                    )
                }
        }
    }

    var jumlahNotif by remember { mutableStateOf(0) }
    var currentRelevantNotifIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var profileImagePath by remember {
        val saved = prefs.getString("profile_path", null)
        mutableStateOf(saved)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val path = saveImageToInternalStorage(context, it)
            if (path != null) {
                profileImagePath = path
                prefs.edit().putString("profile_path", path).apply()
            }
        }
    }

    var namaUser by remember {
        mutableStateOf(prefs.getString("nama_user", "User") ?: "User")
    }
    var showEditNama by remember { mutableStateOf(false) }
    var inputNama by remember { mutableStateOf("") }

    if (showEditNama) {
        AlertDialog(
            onDismissRequest = { showEditNama = false },
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
                    value = inputNama,
                    onValueChange = { inputNama = it },
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
                    shape = RoundedCornerShape(15.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputNama.isNotBlank()) {
                        namaUser = inputNama
                        prefs.edit().putString("nama_user", inputNama).apply()
                    }
                    showEditNama = false
                }) {
                    Text("Simpan", color = RingColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNama = false }) {
                    Text("Batal", color = TextGray)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        try {
            val deviceId = DeviceIdHelper.getDeviceId(context)
            listTransaksi = RetrofitClient.instance.getHistori(deviceId)
            val emitenUnik = listTransaksi.map { it.emiten.uppercase() }.distinct()

            val mapBaru = coroutineScope {
                emitenUnik.map { emiten ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        StockPriceHelper.getHargaLive(emiten) to emiten
                    }
                }.awaitAll()
                    .filter { it.first != null }
                    .associate { it.second to it.first!! }
            }

            hargaLiveMap = mapBaru
            android.util.Log.d("HARGA_DEBUG", "hargaLiveMap = $hargaLiveMap")
            android.util.Log.d("HARGA_DEBUG", "emitenUnik = $emitenUnik")

            try {
                val beritaResponse = RetrofitClient.instance.getBerita()
                if (beritaResponse.status == "success") {
                    val idTerkirimSebelumnya =
                        prefs.getStringSet("notif_id_terkirim", emptySet()) ?: emptySet()
                    val idCleared =
                        prefs.getStringSet("cleared_berita_ids", emptySet()) ?: emptySet()
                    val idTerbaca =
                        prefs.getStringSet("notif_id_terbaca", emptySet()) ?: emptySet()
                    val deviceId = DeviceIdHelper.getDeviceId(context)
                    val userEmitens = listTransaksi.map { it.emiten.uppercase().trim() }.toSet()

                    val beritaBelumTerkirim =
                        beritaResponse.data.filter { it.id !in idTerkirimSebelumnya }
                    val beritaRelevan = beritaResponse.data.filter {
                        it.id !in idCleared && BeritaFilterHelper.isBeritaRelevant(it, userEmitens)
                    }

                    currentRelevantNotifIds = beritaRelevan.map { it.id }.toSet()
                    val beritaBelumTerbaca = beritaRelevan.filter { it.id !in idTerbaca }

                    jumlahNotif = beritaBelumTerbaca.size

                    if (beritaBelumTerkirim.isNotEmpty()) {
                        val beritaBaru = beritaBelumTerkirim.filter {
                            it.id !in idCleared && BeritaFilterHelper.isBeritaRelevant(
                                it,
                                userEmitens
                            )
                        }

                        beritaBaru.forEach { berita ->
                            NotificationHelper.sendBeritaNotif(
                                context = context,
                                notifId = berita.id.hashCode(),
                                emiten = berita.emiten,
                                judul = berita.judul
                            )
                        }

                        val semuaIdSekarang = beritaResponse.data.map { it.id }.toSet()
                        prefs.edit().putStringSet("notif_id_terkirim", semuaIdSekarang).apply()
                    }
                }
            } catch (_: Exception) {
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
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
            ) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
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
                    }
                    IconButton(onClick = onNavigateToRincian) {
                        Icon(
                            painter = painterResource(id = R.drawable.chartsvg),
                            contentDescription = "Rincian",
                            tint = Color.White,
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
                        painter = if (profileImagePath != null)
                            rememberAsyncImagePainter(java.io.File(profileImagePath!!))
                        else
                            painterResource(id = R.drawable.profile),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, color = Purple40, CircleShape)
                            .clickable { galleryLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.clickable {
                            inputNama = namaUser
                            showEditNama = true
                        }
                    ) {
                        Text(text = "Welcome back!", color = TextGray, fontSize = 13.sp)
                        Text(
                            text = namaUser,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconNotification(
                    jumlahNotif = jumlahNotif,
                    onClick = {
                        val updatedTerbaca = (prefs.getStringSet("notif_id_terbaca", emptySet())
                            ?: emptySet()) + currentRelevantNotifIds
                        prefs.edit().putStringSet("notif_id_terbaca", updatedTerbaca).apply()

                        jumlahNotif = 0
                        onNavigateToNotifikasi()
                    }
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CustomDonutChart(
                    listTransaksi = listTransaksi,
                    hargaLiveMap = hargaLiveMap
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "My Investment",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(groupedTransactions, key = { it.emiten }) { transaksi ->
                    SwipeableInvestmentCard(
                        transaksi = transaksi,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    val deviceId = DeviceIdHelper.getDeviceId(context)
                                    val dataHapus =
                                        DeleteData(device_id = deviceId, emiten = transaksi.emiten)
                                    val response = RetrofitClient.instance.hapusInvestasi(dataHapus)
                                    if (response.status == "success") {
                                        listTransaksi = RetrofitClient.instance.getHistori(deviceId)
                                        Toast.makeText(
                                            context,
                                            "Investasi ${transaksi.emiten} berhasil dihapus",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Server Error: ${response.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}