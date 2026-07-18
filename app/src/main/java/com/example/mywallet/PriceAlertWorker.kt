package com.example.mywallet
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class PriceAlertWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val id = DeviceIdHelper.getDeviceId(applicationContext)
            val h = RetrofitClient.instance.getHistori(id)
            if (h.isEmpty()) return@withContext Result.success()
            val prefs = applicationContext.getSharedPreferences("price_alert_prefs", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val stats = h.groupBy { it.emiten.uppercase().trim() }.mapValues { (_, tx) ->
                val cost = tx.sumOf { it.lot * 100.0 * it.harga }
                cost / (tx.sumOf { it.lot } * 100.0)
            }
            stats.forEach { (e, avg) ->
                val lastSent = prefs.getLong("last_ts_$e", 0L)
                if (now - lastSent < 24 * 60 * 60 * 1000L) return@forEach
                val meta = StockPriceHelper.getHargaLiveWithMeta(StockPriceHelper.buildSymbol(e)) ?: return@forEach
                val cur = meta.optDouble("regularMarketPrice", Double.NaN)
                val prev = meta.optDouble("chartPreviousClose", Double.NaN)
                if (cur.isNaN() || prev.isNaN() || prev == 0.0) return@forEach
                val daily = ((cur - prev) / prev) * 100.0
                if (daily >= 15.0 || daily <= -10.0) {
                    NotificationHelper.sendPriceAlertNotif(applicationContext, e, cur, ((cur - avg) / avg) * 100.0)
                    prefs.edit().putLong("last_ts_$e", now).apply()
                }
            }
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
}
