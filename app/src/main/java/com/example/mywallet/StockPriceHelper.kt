package com.example.mywallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

object StockPriceHelper {
    private val client = OkHttpClient()

    private suspend fun getMeta(symbol: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "application/json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                .getJSONObject("meta")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getHargaLive(emiten: String): Double? {
        val symbol = buildSymbol(emiten)
        return getMeta(symbol)?.optDouble("regularMarketPrice")?.takeIf { !it.isNaN() }
    }

    suspend fun getHargaLiveWithMeta(symbol: String): JSONObject? {
        return getMeta(symbol)
    }

    suspend fun getPersentaseLive(symbol: String): String? {
        return try {
            val meta = getMeta(symbol) ?: return null
            val harga = meta.optDouble("regularMarketPrice")
            val prevClose = meta.optDouble("chartPreviousClose")
            if (harga.isNaN() || prevClose.isNaN() || prevClose == 0.0) return null
            val persen = ((harga - prevClose) / prevClose) * 100
            val prefix = if (persen >= 0) "+" else ""
            "$prefix${"%.1f".format(persen)}%"
        } catch (_: Exception) {
            null
        }
    }

    fun buildSymbol(emiten: String): String {
        val clean = emiten.uppercase().trim()
        return when (clean) {
            "IHSG" -> "^JKSE"
            "DJIA" -> "^DJI"
            "NASDAQ" -> "^IXIC"
            "S&P500" -> "^GSPC"
            "WTI OIL" -> "CL=F"
            else -> {
                when {
                    clean.startsWith("^") || clean.endsWith(".JK") || clean.contains("=") -> clean
                    else -> "$clean.JK"
                }
            }
        }
    }

    suspend fun getHistoricalPrices(
        emiten: String,
        period1: Long,
        period2: Long
    ): Map<Int, Double> = withContext(Dispatchers.IO) {
        try {
            val symbol = buildSymbol(emiten)
            val url =
                "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?period1=$period1&period2=$period2&interval=1d"
            val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyMap()

            val json = JSONObject(body)
            val resObj = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val timestamps = resObj.optJSONArray("timestamp") ?: return@withContext emptyMap()
            val closeArray =
                resObj.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                    .getJSONArray("close")

            val priceMap = mutableMapOf<Int, Double>()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))

            for (i in 0 until timestamps.length()) {
                if (!closeArray.isNull(i)) {
                    cal.timeInMillis = timestamps.getLong(i) * 1000
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    priceMap[day] = closeArray.getDouble(i)
                }
            }
            priceMap
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun getMonthlyHistoricalPrices(emiten: String): Map<Int, Double> = withContext(Dispatchers.IO) {
        try {
            val symbol = buildSymbol(emiten)
            val url =
                "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1y&interval=1mo"
            val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyMap()

            val json = JSONObject(body)
            val resObj = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val timestamps = resObj.optJSONArray("timestamp") ?: return@withContext emptyMap()
            val closeArray =
                resObj.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                    .getJSONArray("close")

            val priceMap = mutableMapOf<Int, Double>()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))

            for (i in 0 until timestamps.length()) {
                if (!closeArray.isNull(i)) {
                    cal.timeInMillis = timestamps.getLong(i) * 1000
                    val month = cal.get(Calendar.MONTH) + 1
                    priceMap[month] = closeArray.getDouble(i)
                }
            }
            priceMap
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
