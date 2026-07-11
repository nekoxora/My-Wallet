package com.example.mywallet.data

import com.example.mywallet.StockPriceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChatBotService {
    private const val BASE_URL = "http://43.133.150.113/api_keuangan/"
    private const val CHATBOT_URL = "${BASE_URL}chatbot.php"
    private const val RSS_URL = "${BASE_URL}get_berita_rss.php"
    private const val API_KEY = "KODE_RAHASIA_ANDROID_123"
    private val indexMap = mapOf("IHSG" to "^JKSE", "LQ45" to "^JKLQ45")
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }).build()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val tickerRegex = Regex("\\b[A-Z]{4}\\b")
    private suspend fun triggerSync(tickers: Set<String>, wait: Boolean) =
        withContext(Dispatchers.IO) {
            val jobs = tickers.map { e ->
                async {
                    try {
                        if (wait || !StockPriceHelper.checkDataExists(e)) StockPriceHelper.syncFundamentalToBackend(
                            e,
                            "chat"
                        )
                    } catch (ex: Exception) {
                    }
                }
            }
            if (wait) jobs.awaitAll()
        }

    private suspend fun fetchHargaLiveList(
        userInput: String,
        portofolioEmitenList: List<String>
    ): JSONArray = withContext(Dispatchers.IO) {
        val emitenUntukDicek = mutableSetOf<String>();
        val up = userInput.uppercase();
        val isPortfolio = up.contains("ANALISA PORTOFOLIO SAYA")
        val tickersFromInput = tickerRegex.findAll(up).map { it.value }.toMutableSet()
        if (isPortfolio) {
            val portfolio =
                portofolioEmitenList.map { it.uppercase().trim() }.filter { it.isNotEmpty() }
                    .toSet()
            val allToSync = (tickersFromInput + portfolio).toSet()
            if (allToSync.isNotEmpty()) triggerSync(allToSync, true)
        } else if (tickersFromInput.isNotEmpty()) {
            triggerSync(tickersFromInput, false)
        }
        emitenUntukDicek.addAll(portofolioEmitenList.map { it.uppercase() }); emitenUntukDicek.addAll(
        tickersFromInput
    )
        indexMap.keys.forEach { k -> if (userInput.contains(k, true)) emitenUntukDicek.add(k) }
        val arr = JSONArray()
        for (kode in emitenUntukDicek) {
            val s = indexMap[kode] ?: kode;
            val h = StockPriceHelper.getHargaLive(s)
            if (h != null) arr.put(JSONObject().apply { put("emiten", kode); put("harga", h) })
        }
        return@withContext arr
    }

    private suspend fun fetchBeritaList(): JSONArray = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        try {
            client.newCall(
                Request.Builder().url(RSS_URL).addHeader("X-API-Key", API_KEY).get().build()
            ).execute().use { r ->
                if (r.isSuccessful) {
                    val data =
                        JSONObject(r.body?.string() ?: "").optJSONArray("data") ?: JSONArray()
                    for (i in 0 until minOf(data.length(), 5)) {
                        val item = data.getJSONObject(i); arr.put(JSONObject().apply {
                            put(
                                "emiten",
                                item.optString("emiten", "")
                            ); put("judul", item.optString("judul", ""))
                        })
                    }
                }
            }
        } catch (e: Exception) {
        }
        return@withContext arr
    }

    suspend fun generateResponse(
        userInput: String,
        deviceId: String,
        portofolioEmitenList: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        try {
            val hargaLive = fetchHargaLiveList(userInput, portofolioEmitenList);
            val berita = fetchBeritaList()
            val body = JSONObject().apply {
                put("user_input", userInput); put(
                "device_id",
                deviceId
            ); put("harga_live_list", hargaLive); put("berita_list", berita)
            }.toString().toRequestBody(JSON)
            client.newCall(
                Request.Builder().url(CHATBOT_URL).addHeader("X-API-Key", API_KEY).post(body)
                    .build()
            ).execute().use { r ->
                var s = r.body?.string() ?: ""
                if (s.contains("{")) s = s.substring(s.indexOf("{"), s.lastIndexOf("}") + 1)
                if (r.isSuccessful) JSONObject(s).optString(
                    "response",
                    "Maaf, sistem sibuk."
                ) else "Error: ${r.code}"
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    suspend fun scrapeAndSaveEmiten(emiten: String, deviceId: String = "manual"): Boolean {
        return StockPriceHelper.syncFundamentalToBackend(emiten.uppercase().trim(), deviceId)
    }

    suspend fun isDataExists(emiten: String): Boolean {
        return StockPriceHelper.checkDataExists(emiten)
    }
}
