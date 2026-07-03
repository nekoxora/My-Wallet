package com.example.mywallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {

    // Ganti dengan domain/VPS kamu yang sudah dipakai untuk endpoint MyWallet lainnya
    private const val PROXY_URL = "http://43.133.150.113/api_keuangan/gemini_proxy.php"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Mengirim pertanyaan user ke backend proxy, yang akan meneruskannya ke Gemini AI.
     * API key TIDAK pernah ada di sisi Android.
     */
    suspend fun generateResponse(
        userInput: String,
        portfolioContext: String = "",
        deviceId: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("user_input", userInput)
                .add("portfolio_context", portfolioContext)
                .add("device_id", deviceId)
                .build()

            val request = Request.Builder()
                .url(PROXY_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    val errMsg = try {
                        responseBody?.let { JSONObject(it).optString("error") }
                    } catch (e: Exception) {
                        null
                    }
                    return@withContext errMsg?.takeIf { it.isNotBlank() }
                        ?: "Error: Gagal terhubung ke server (kode ${response.code})."
                }

                val json = JSONObject(responseBody)

                if (json.has("error")) {
                    return@withContext "Error: ${json.getString("error")}"
                }

                json.optString("response", "Maaf, saya tidak bisa memberikan jawaban saat ini.")
            }
        } catch (e: IOException) {
            "Error: Gagal terhubung ke server. Cek koneksi internet Anda."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Terjadi kesalahan tidak terduga."}"
        }
    }
}