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

class PriceAlertWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceIdHelper.getDeviceId(context)
            val histori = RetrofitClient.instance.getHistori(deviceId)
            if (histori.isEmpty()) return@withContext Result.success()
            val prefs = context.getSharedPreferences("price_alert_prefs", Context.MODE_PRIVATE)
            val today =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val stats = histori.groupBy { it.emiten.uppercase().trim() }.mapValues { (_, tx) ->
                val cost = tx.sumOf { it.lot * 100.0 * it.harga }
                val totalLot = tx.sumOf { it.lot }
                cost / (totalLot * 100.0)
            }
            stats.forEach { (emiten, avgPrice) ->
                if (prefs.getString("last_notif_$emiten", "") == today) return@forEach
                val meta =
                    StockPriceHelper.getHargaLiveWithMeta(StockPriceHelper.buildSymbol(emiten))
                        ?: return@forEach
                val cur = meta.optDouble("regularMarketPrice", Double.NaN)
                val prev = meta.optDouble("chartPreviousClose", Double.NaN)
                if (cur.isNaN() || prev.isNaN() || prev == 0.0) return@forEach
                val daily = ((cur - prev) / prev) * 100.0
                if (daily >= 15.0 || daily <= -10.0) {
                    NotificationHelper.sendPriceAlertNotif(
                        context,
                        emiten,
                        cur,
                        ((cur - avgPrice) / avgPrice) * 100.0
                    )
                    prefs.edit().putString("last_notif_$emiten", today).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
