package com.example.mywallet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object StockPriceHelper {
    private val client = OkHttpClient()

    // -------------------------------------------------------
    // Ambil meta JSON dari Yahoo Finance
    // symbol sudah dalam bentuk lengkap: "GOTO.JK" atau "^JKSE"
    // -------------------------------------------------------
    private suspend fun getMeta(symbol: String): JSONObject? {
        return try {
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            json.getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONObject("meta")
        } catch (e: Exception) {
            android.util.Log.e("StockPrice", "getMeta error [$symbol]: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------
    // Dipanggil dari RincianScreen (emiten = "GOTO", "BBCA", dll)
    // Method ini tetap auto-append .JK agar RincianScreen tidak perlu diubah
    // -------------------------------------------------------
    suspend fun getHargaLive(emiten: String): Double? {
        val symbol = if (emiten.startsWith("^") || emiten.endsWith(".JK")) {
            emiten // sudah lengkap, pakai apa adanya
        } else {
            "$emiten.JK" // dari RincianScreen, tambahkan .JK
        }
        return getMeta(symbol)?.optDouble("regularMarketPrice")?.takeIf { !it.isNaN() }
    }

    // -------------------------------------------------------
    // Ambil persentase perubahan harga hari ini
    // symbol sudah lengkap: "GOTO.JK" atau "^JKSE"
    // Return contoh: "+1.5%" atau "-0.8%"
    // -------------------------------------------------------
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