package com.example.mywallet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object StockPriceHelper {
    private val client = OkHttpClient()

    private suspend fun getMeta(symbol: String): JSONObject? {
        return try {
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            android.util.Log.d("HARGA_DEBUG", "getMeta [$symbol] HTTP code: ${response.code}")
            android.util.Log.d("HARGA_DEBUG", "getMeta [$symbol] body (200 char): ${body?.take(200)}")

            if (body == null) return null

            val json = JSONObject(body)
            json.getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONObject("meta")
        } catch (e: Exception) {
            android.util.Log.e("HARGA_DEBUG", "getMeta error [$symbol]: ${e.message}")
            null
        }
    }

    suspend fun getHargaLive(emiten: String): Double? {
        val symbol = when {
            emiten.startsWith("^") -> emiten
            emiten.endsWith(".JK") -> emiten
            emiten.endsWith("=F")  -> emiten
            emiten.endsWith("=X")  -> emiten
            else                   -> "$emiten.JK"
        }
        android.util.Log.d("HARGA_DEBUG", "getHargaLive: emiten=$emiten -> symbol=$symbol")
        val harga = getMeta(symbol)?.optDouble("regularMarketPrice")?.takeIf { !it.isNaN() }
        android.util.Log.d("HARGA_DEBUG", "getHargaLive: symbol=$symbol -> harga=$harga")
        return harga
    }

    suspend fun getPersentaseLive(symbol: String): String? {
        return try {
            val meta = getMeta(symbol) ?: return null

            val harga     = meta.optDouble("regularMarketPrice")
            val prevClose = meta.optDouble("chartPreviousClose")

            if (harga.isNaN() || prevClose.isNaN() || prevClose == 0.0) return null

            val persen = ((harga - prevClose) / prevClose) * 100
            val prefix = if (persen >= 0) "+" else ""
            "$prefix${"%.1f".format(persen)}%"
        } catch (e: Exception) {
            android.util.Log.e("StockPrice", "getPersentaseLive error [$symbol]: ${e.message}")
            null
        }
    }
}