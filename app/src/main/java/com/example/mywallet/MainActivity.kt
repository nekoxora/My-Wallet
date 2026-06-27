package com.example.mywallet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.ui.theme.BgDark
import com.example.mywallet.ui.theme.CardDark
import com.example.mywallet.ui.theme.MyWalletTheme
import com.example.mywallet.ui.theme.RingColor
import com.example.mywallet.ui.theme.TextGray
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.example.mywallet.ui.theme.nvBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import retrofit2.http.Query
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import com.example.mywallet.ui.theme.Purple40
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class InvestasiData(val kode_emiten: String, val jumlah_lot: Int, val harga_beli: Double)
data class DeleteData(val emiten: String)
data class ApiResponse(val status: String, val message: String)
data class Transaksi(
    val id: Int,
    val emiten: String,
    val tgl: String,
    val lot: Int,
    val harga: Double
)

data class HargaLiveResponse(
    val status: String,
    val emiten: String?,
    val harga_live: Double?,
    val message: String?
)

data class BeritaSaham(
    val id: Int,
    val judul: String,
    val isi: String,
    val emiten: String,
    val tgl: String
)

interface ApiService {
    @POST("api_keuangan/insert_investasi.php")
    suspend fun simpanInvestasi(@Body data: InvestasiData): ApiResponse

    @POST("api_keuangan/delete_investasi.php")
    suspend fun hapusInvestasi(@Body data: DeleteData): ApiResponse

    @GET("api_keuangan/get_histori.php")
    suspend fun getHistori(): List<Transaksi>

    @GET("api_keuangan/get_harga_live.php")
    suspend fun getHargaLive(@Query("emiten") emiten: String): HargaLiveResponse
}

object RetrofitClient {
    private const val BASE_URL = "http://43.133.150.113/"
    val instance: ApiService by lazy {
        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark,
                    contentColor = Color.White
                ) {
                    MainApp()
                }
            }
        }
    }
}

enum class Layar { DASHBOARD, FORM, RINCIAN, NOTIFIKASI }

@Composable
fun MainApp() {
    var layarAktif by remember { mutableStateOf(Layar.DASHBOARD) }
    var layarSebelumnya by remember { mutableStateOf(Layar.DASHBOARD) }

    when (layarAktif) {
        Layar.DASHBOARD -> DashboardScreen(
            onNavigateToForm = {
                layarSebelumnya = Layar.DASHBOARD
                layarAktif = Layar.FORM
            },
            onNavigateToRincian = { layarAktif = Layar.RINCIAN },
            onNavigateToNotifikasi = { layarAktif = Layar.NOTIFIKASI }
        )

        Layar.FORM -> FormInvestasi(
            onBack = { layarAktif = layarSebelumnya }
        )

        Layar.RINCIAN -> RincianScreen(
            onNavigateToHome = { layarAktif = Layar.DASHBOARD },
            onNavigateToForm = {
                layarSebelumnya = Layar.RINCIAN
                layarAktif = Layar.FORM
            }
        )

        Layar.NOTIFIKASI -> NotifikasiScreen(onBack = { layarAktif = Layar.DASHBOARD })
    }
}

@Composable
fun NotifikasiScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
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

fun saveImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "profile_photo.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun IconNotification(
    hasNotification: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.TopEnd
    ) {
        Icon(
            painter = painterResource(id = R.drawable.bell),
            contentDescription = "Notifications",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
        )

        if (hasNotification) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .border(2.dp, BgDark, CircleShape)
            )
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
    var adaNotif by remember { mutableStateOf(false) }

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
            listTransaksi = RetrofitClient.instance.getHistori()
            val emitenUnik = listTransaksi.map { it.emiten.uppercase() }.distinct()

            val mapBaru = coroutineScope {
                emitenUnik.map { emiten ->
                    async {
                        val harga = StockPriceHelper.getHargaLive(emiten)
                        harga to emiten
                    }
                }.awaitAll()
                    .filter { it.first != null }
                    .associate { it.second to it.first!! }
            }

            hargaLiveMap = mapBaru
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
                    hasNotification = adaNotif,
                    onClick = {
                        adaNotif = false
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
                                    val dataHapus = DeleteData(transaksi.emiten)

                                    val response = RetrofitClient.instance.hapusInvestasi(dataHapus)

                                    if (response.status == "success") {
                                        listTransaksi = RetrofitClient.instance.getHistori()
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

@Composable
fun RincianScreen(onNavigateToHome: () -> Unit, onNavigateToForm: () -> Unit) {
    var listTransaksi by remember { mutableStateOf<List<Transaksi>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            listTransaksi = RetrofitClient.instance.getHistori()
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
                .padding(top = padding.calculateTopPadding())
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
            .padding(vertical = 8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormInvestasi(onBack: () -> Unit) {
    var kodeEmiten by remember { mutableStateOf("") }
    var jumlahLot by remember { mutableStateOf("") }
    var hargaBeli by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tambah Portofolio",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = kodeEmiten,
            onValueChange = { kodeEmiten = it },
            label = { Text("Kode Emiten", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor,
                unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = jumlahLot,
            onValueChange = { input ->
                if (input.all { char -> char.isDigit() }) jumlahLot = input
            },
            label = { Text("Jumlah Lot", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = NumberDotTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor,
                unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hargaBeli,
            onValueChange = { input ->
                if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    hargaBeli = input
                }
            },
            label = { Text("Harga per Lembar", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor, unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor, unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (kodeEmiten.isNotEmpty() && jumlahLot.isNotEmpty() && hargaBeli.isNotEmpty()) {
                    val dataKirim = InvestasiData(
                        kode_emiten = kodeEmiten.uppercase(),
                        jumlah_lot = jumlahLot.toIntOrNull() ?: 0,
                        harga_beli = hargaBeli.replace(',', '.').toDoubleOrNull() ?: 0.0
                    )
                    coroutineScope.launch {
                        try {
                            val response =
                                RetrofitClient.instance.simpanInvestasi(dataKirim)
                            Toast.makeText(
                                context,
                                response.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (response.status == "success") {
                                onBack()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Gagal: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Isi semua data!", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RingColor)
        ) {
            Text("Simpan Investasi", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

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

    val emitenLotMap = mutableMapOf<String, Int>()
    listTransaksi.forEach { transaksi ->
        val currentLot = emitenLotMap[transaksi.emiten] ?: 0
        emitenLotMap[transaksi.emiten] = currentLot + transaksi.lot
    }

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

@Composable
fun InvestmentCard(kodeEmiten: String, tanggal: String, jumlahLot: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = kodeEmiten,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = tanggal, color = TextGray, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .border(1.dp, RingColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$jumlahLot LOT",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableInvestmentCard(transaksi: Transaksi, onDelete: () -> Unit) {
    var sudahHapus by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart && !sudahHapus) {
                sudahHapus = true
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "Hapus",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        content = {
            InvestmentCard(
                kodeEmiten = transaksi.emiten,
                tanggal = transaksi.tgl,
                jumlahLot = transaksi.lot
            )
        }
    )
}

class NumberDotTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val reversed = originalText.reversed()
        var formattedReversed = ""
        for (i in reversed.indices) {
            formattedReversed += reversed[i]
            if ((i + 1) % 3 == 0 && i != reversed.lastIndex) {
                formattedReversed += "."
            }
        }
        val formattedText = formattedReversed.reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= originalText.length) return formattedText.length
                val totalDots = (originalText.length - 1) / 3
                val dotsToRight = (originalText.length - offset - 1) / 3
                return offset + (totalDots - dotsToRight)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formattedText.length) return originalText.length
                var originalOffset = 0
                var transformedCount = 0
                while (transformedCount < offset && originalOffset < originalText.length) {
                    if (formattedText[transformedCount] != '.') originalOffset++
                    transformedCount++
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}