package com.example.mywallet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object StockPriceHelper {
    private val client = OkHttpClient()

    suspend fun getHargaLive(emiten: String): Double? {
        return try {
            val symbol = "${emiten}.JK"
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val result = json
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONObject("meta")
                .getDouble("regularMarketPrice")

            result
        } catch (e: Exception) {
            android.util.Log.e("StockPrice", "Error: ${e.message}")
            null
        }
    }
}