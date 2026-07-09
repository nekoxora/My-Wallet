package com.example.mywallet

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PriceAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceIdHelper.getDeviceId(context)
            val histori = RetrofitClient.instance.getHistori(deviceId)
            if (histori.isEmpty()) return@withContext Result.success()

            val prefs = context.getSharedPreferences("price_alert_prefs", Context.MODE_PRIVATE)
            val todayDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

            val emitenStats = histori.groupBy { it.emiten.uppercase().trim() }
                .mapValues { (_, transactions) ->
                    val totalCost = transactions.sumOf { it.lot * 100.0 * it.harga }
                    val totalLot = transactions.sumOf { it.lot }
                    totalCost / (totalLot * 100.0)
                }

            emitenStats.forEach { (emiten, avgPrice) ->
                val symbol = when {
                    emiten.startsWith("^") || emiten.endsWith(".JK") || emiten.contains("=") -> emiten
                    else -> "$emiten.JK"
                }

                val lastNotifDate = prefs.getString("last_notif_$emiten", "")
                if (lastNotifDate == todayDate) return@forEach

                val meta = try {
                    StockPriceHelper.getHargaLiveWithMeta(symbol)
                } catch (_: Exception) {
                    null
                } ?: return@forEach

                val currentPrice = meta.optDouble("regularMarketPrice")
                val prevClose = meta.optDouble("chartPreviousClose")

                if (currentPrice.isNaN() || prevClose.isNaN() || prevClose == 0.0) return@forEach

                val dailyChangePercent = ((currentPrice - prevClose) / prevClose) * 100.0

                if (dailyChangePercent >= 15.0 || dailyChangePercent <= -10.0) {
                    val totalReturnPercent = ((currentPrice - avgPrice) / avgPrice) * 100.0

                    NotificationHelper.sendPriceAlertNotif(
                        context = context,
                        emiten = emiten,
                        currentPrice = currentPrice,
                        totalReturnPercent = totalReturnPercent
                    )

                    prefs.edit().putString("last_notif_$emiten", todayDate).apply()
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
