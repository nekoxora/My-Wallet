package com.example.mywallet.data

import com.example.mywallet.StockPriceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {

    private const val PROXY_URL = "http://43.133.150.113/api_keuangan/gemini_proxy.php"

    private val indexMap = mapOf(
        "IHSG" to "^JKSE",
        "LQ45" to "^JKLQ45"
    )

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private suspend fun fetchHargaLiveList(
        userInput: String,
        portofolioEmitenList: List<String>
    ): JSONArray {
        val emitenUntukDicek = mutableSetOf<String>()
        emitenUntukDicek.addAll(portofolioEmitenList.map { it.uppercase() })

        val regex = Regex("\\b[A-Z]{4}\\b")
        regex.find(userInput.uppercase())?.let { emitenUntukDicek.add(it.value) }

        indexMap.keys.forEach { keyword ->
            if (userInput.contains(keyword, ignoreCase = true)) emitenUntukDicek.add(keyword)
        }

        val hargaLiveArray = JSONArray()
        for (kode in emitenUntukDicek) {
            val symbolLookup = indexMap[kode] ?: kode
            val harga = StockPriceHelper.getHargaLive(symbolLookup)
            if (harga != null) {
                hargaLiveArray.put(
                    JSONObject().apply {
                        put("emiten", kode)
                        put("harga", harga)
                    }
                )
            } else {
                android.util.Log.d("GEMINI_DEBUG", "Gagal ambil harga live untuk: $kode")
            }
        }
        return hargaLiveArray
    }

    suspend fun generateResponse(
        userInput: String,
        deviceId: String,
        portofolioEmitenList: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        try {
            val hargaLiveArray = fetchHargaLiveList(userInput, portofolioEmitenList)

            android.util.Log.d("GEMINI_DEBUG", "Harga live dikirim: $hargaLiveArray")

            val bodyJson = JSONObject().apply {
                put("user_input", userInput)
                put("device_id", deviceId)
                put("harga_live_list", hargaLiveArray)
            }

            val requestBody = bodyJson.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url(PROXY_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                var responseBodyString = response.body?.string() ?: ""

                if (responseBodyString.contains("{")) {
                    responseBodyString =
                        responseBodyString.substring(responseBodyString.indexOf("{"))
                }
                if (responseBodyString.contains("}")) {
                    responseBodyString =
                        responseBodyString.substring(0, responseBodyString.lastIndexOf("}") + 1)
                }

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBodyString)
                    return@use jsonResponse.optString(
                        "response",
                        "Maaf, format balasan tidak sesuai."
                    )
                } else {
                    val errorMessage = try {
                        JSONObject(responseBodyString).optString(
                            "error",
                            "Error Code: ${response.code}"
                        )
                    } catch (e: Exception) {
                        "Terjadi kesalahan server (Code: ${response.code})"
                    }
                    return@use "Error: $errorMessage"
                }
            }
        } catch (e: IOException) {
            "Error: Gagal terhubung ke server. Cek koneksi internet Anda."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Terjadi kesalahan tidak terduga."}"
        }
    }
}