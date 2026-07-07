package com.example.mywallet.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RincianScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToForm: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    var listTransaksi by remember { mutableStateOf<List<Transaksi>>(emptyList()) }
    var hargaLiveMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var historicalDataPoints by remember { mutableStateOf<List<Double>>(emptyList()) }
    var selectedChartType by remember { mutableStateOf(ChartType.MONTHLY) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val pullState = rememberPullToRefreshState()

    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(refreshTrigger, selectedChartType) {
        try {

            val deviceId = DeviceIdHelper.getDeviceId(context)
            listTransaksi = RetrofitClient.instance.getHistori(deviceId)

            val emitenUnik = listTransaksi.map { it.emiten.uppercase() }.distinct()
            val calendar = Calendar.getInstance()

            coroutineScope {
                val liveTask = emitenUnik.map { emiten ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        StockPriceHelper.getHargaLive(emiten) to emiten
                    }
                }

                val historicalResults = if (selectedChartType == ChartType.MONTHLY) {
                    val calStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val p1 = calStart.timeInMillis / 1000
                    val p2 = System.currentTimeMillis() / 1000

                    emitenUnik.map { emiten ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            emiten to StockPriceHelper.getHistoricalPrices(emiten, p1, p2)
                        }
                    }.awaitAll().toMap()
                } else {
                    emitenUnik.map { emiten ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            emiten to StockPriceHelper.getMonthlyHistoricalPrices(emiten)
                        }
                    }.awaitAll().toMap()
                }

                hargaLiveMap = liveTask.awaitAll()
                    .filter { it.first != null }
                    .associate { it.second to it.first!! }

                val aggregatedPoints = mutableListOf<Double>()
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)

                if (selectedChartType == ChartType.MONTHLY) {
                    val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                    val maxTransDay = listTransaksi.mapNotNull {
                        try {
                            dateFormat.parse(it.tgl)
                        } catch (e: Exception) {
                            null
                        }
                    }.filter {
                        val c = Calendar.getInstance().apply { time = it }
                        c.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                c.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                    }.map {
                        val c = Calendar.getInstance().apply { time = it }
                        c.get(Calendar.DAY_OF_MONTH)
                    }.maxOrNull() ?: 0

                    val lastDayToShow = maxOf(currentDayOfMonth, maxTransDay)

                    for (day in 1..lastDayToShow) {
                        var totalForDay = 0.0
                        val calDay = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        listTransaksi.forEach { transaksi ->
                            val tglTransaksi = try {
                                dateFormat.parse(transaksi.tgl)
                            } catch (e: Exception) {
                                null
                            }
                            val calBeli = Calendar.getInstance().apply {
                                if (tglTransaksi != null) {
                                    time = tglTransaksi
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            }

                            if (tglTransaksi != null && !calDay.before(calBeli)) {
                                val priceMap = historicalResults[transaksi.emiten] ?: emptyMap()
                                var lastPrice: Double? = null
                                for (d in day downTo 1) {
                                    if (priceMap.containsKey(d)) {
                                        lastPrice = priceMap[d]
                                        break
                                    }
                                }
                                val finalPrice =
                                    lastPrice ?: hargaLiveMap[transaksi.emiten.uppercase()]
                                    ?: transaksi.harga
                                totalForDay += transaksi.lot.toDouble() * 100.0 * finalPrice
                            }
                        }
                        aggregatedPoints.add(totalForDay)
                    }
                } else {
                    val currentMonth = calendar.get(Calendar.MONTH) + 1
                    for (m in 1..currentMonth) {
                        var totalForMonth = 0.0
                        listTransaksi.forEach { transaksi ->
                            val tglTransaksi = try {
                                dateFormat.parse(transaksi.tgl)
                            } catch (e: Exception) {
                                null
                            }
                            val calBeli = Calendar.getInstance().apply {
                                if (tglTransaksi != null) {
                                    time = tglTransaksi
                                }
                            }

                            if (tglTransaksi != null && (calendar.get(Calendar.YEAR) > calBeli.get(
                                    Calendar.YEAR
                                ) ||
                                        (calendar.get(Calendar.YEAR) == calBeli.get(Calendar.YEAR) && m >= calBeli.get(
                                            Calendar.MONTH
                                        ) + 1))
                            ) {

                                val priceMap = historicalResults[transaksi.emiten] ?: emptyMap()
                                val monthPrice =
                                    priceMap[m] ?: hargaLiveMap[transaksi.emiten.uppercase()]
                                    ?: transaksi.harga
                                totalForMonth += transaksi.lot.toDouble() * 100.0 * monthPrice
                            }
                        }
                        aggregatedPoints.add(totalForMonth)
                    }
                }
                historicalDataPoints = aggregatedPoints
            }

            if (isRefreshing) kotlinx.coroutines.delay(800)
            isRefreshing = false
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
        } finally {
            isRefreshing = false
        }
    }

    val totalModal = remember(listTransaksi) {
        listTransaksi.sumOf { it.lot.toDouble() * 100.0 * it.harga }
    }

    val totalMarketValue = remember(listTransaksi, hargaLiveMap) {
        listTransaksi.sumOf { transaksi ->
            val key = transaksi.emiten.uppercase()
            val hargaSekarang = hargaLiveMap[key] ?: transaksi.harga
            transaksi.lot.toDouble() * 100.0 * hargaSekarang
        }
    }

    val totalProfitLoss = totalMarketValue - totalModal
    val profitPercentage = if (totalModal > 0) (totalProfitLoss / totalModal) * 100.0 else 0.0

    val maxDaysInMonth = remember { Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp)
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                TotalProfitLossHeader(
                    totalProfitLoss = totalProfitLoss,
                    profitPercentage = profitPercentage,
                    historicalDataPoints = historicalDataPoints,
                    maxPoints = if (selectedChartType == ChartType.MONTHLY) maxDaysInMonth else 12,
                    chartType = selectedChartType,
                    onChartTypeChange = { selectedChartType = it }
                )

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
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            refreshTrigger++
                        },
                        state = pullState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        indicator = {
                            if (isRefreshing) {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = isRefreshing,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    ) {
                        val dragAmount =
                            if (isRefreshing || pullState.isAnimating) 0f else with(density) { pullState.distanceFraction * 80.dp.toPx() }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = dragAmount
                                },
                            contentPadding = PaddingValues(bottom = 150.dp)
                        ) {
                            items(listTransaksi, key = { it.id }) { transaksi ->
                                RincianCard(
                                    transaksi = transaksi,
                                    hargaSekarang = hargaLiveMap[transaksi.emiten.uppercase()]
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                DraggableBotIcon(onClick = onNavigateToChat)
            }
        }
    }
}

@Composable
fun TotalProfitLossHeader(
    totalProfitLoss: Double,
    profitPercentage: Double,
    historicalDataPoints: List<Double>,
    maxPoints: Int,
    chartType: ChartType,
    onChartTypeChange: (ChartType) -> Unit
) {
    val isProfit = totalProfitLoss >= 0
    val mainColor = if (isProfit) Color(0xFF4ADE80) else Color(0xFFEF4444)
    val sign = if (isProfit) "+" else ""

    val calendar = Calendar.getInstance()
    val localeId = Locale("id", "ID")
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, localeId) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Total Portfolio Profit / Loss",
            color = TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sign + "Rp " + String.format(Locale.US, "%,.0f", totalProfitLoss)
                .replace(',', '.'),
            color = mainColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(mainColor.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = sign + String.format(Locale.US, "%.2f", profitPercentage) + "%",
                color = mainColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (historicalDataPoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(CardDark)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(
                    ChartType.MONTHLY to "Monthly",
                    ChartType.YEARLY to "Yearly"
                ).forEach { (type, label) ->
                    val isSelected = chartType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RingColor else Color.Transparent)
                            .clickable { onChartTypeChange(type) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextGray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (chartType == ChartType.MONTHLY) "Performance in $monthName" else "Performance in ${
                    calendar.get(
                        Calendar.YEAR
                    )
                }",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            GrowthLineChart(
                dataPoints = historicalDataPoints,
                chartType = chartType,
                maxPoints = maxPoints,
                monthName = monthName,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun RincianCard(transaksi: Transaksi, hargaSekarang: Double? = null) {
    var hargaLive by remember { mutableStateOf<Double?>(hargaSekarang) }
    var isLoading by remember { mutableStateOf(hargaSekarang == null) }
    val hargaAvgFormatted =
        "Rp " + String.format(Locale.US, "%,.2f", transaksi.harga).replace(',', '.')

    LaunchedEffect(transaksi.emiten, hargaSekarang) {
        if (hargaSekarang != null) {
            hargaLive = hargaSekarang
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
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
                            "Rp " + String.format(Locale.US, "%,.2f", hargaLive!!).replace(',', '.')

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
