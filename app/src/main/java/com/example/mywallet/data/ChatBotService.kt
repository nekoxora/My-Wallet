package com.example.mywallet.data

import android.util.Log
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
import java.io.IOException
import java.util.concurrent.TimeUnit

object ChatBotService {
    private const val BASE_URL = "http://43.133.150.113/api_keuangan/"
    private const val CHATBOT_URL = "${BASE_URL}chatbot.php"
    private const val RSS_URL = "${BASE_URL}get_berita_rss.php"
    private const val API_KEY = "KODE_RAHASIA_ANDROID_123"
    private val indexMap = mapOf("IHSG" to "^JKSE", "LQ45" to "^JKLQ45")
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }).build()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val tickerRegex = Regex("\\b[A-Z]{4}\\b")
    private suspend fun triggerBackgroundFundamentalSync(
        candidateTickers: Set<String>,
        force: Boolean = false
    ) = withContext(Dispatchers.IO) {
        candidateTickers.map { emiten ->
            async {
                try {
                    if (force || !StockPriceHelper.checkDataExists(emiten)) StockPriceHelper.syncFundamentalToBackend(
                        emiten,
                        "auto_scrape_from_chat"
                    )
                } catch (e: Exception) {
                    Log.e("SCRAPING_DEBUG", "Error: ${e.message}")
                }
            }
        }.awaitAll()
    }

    private suspend fun fetchHargaLiveList(
        userInput: String,
        portofolioEmitenList: List<String>
    ): JSONArray = withContext(Dispatchers.IO) {
        val emitenUntukDicek = mutableSetOf<String>();
        val up = userInput.uppercase();
        val isPortfolioRequest = up.contains("ANALISA PORTOFOLIO SAYA")
        val tickersToSync = tickerRegex.findAll(up).map { it.value }.toMutableSet()
        if (isPortfolioRequest) {
            val portfolio =
                portofolioEmitenList.map { it.uppercase().trim() }.filter { it.isNotEmpty() }
                    .toSet(); tickersToSync.addAll(portfolio); if (tickersToSync.isNotEmpty()) triggerBackgroundFundamentalSync(
                tickersToSync,
                true
            )
        } else if (tickersToSync.isNotEmpty()) triggerBackgroundFundamentalSync(
            tickersToSync,
            false
        )
        emitenUntukDicek.addAll(portofolioEmitenList.map { it.uppercase() }); emitenUntukDicek.addAll(
        tickersToSync
    )
        indexMap.keys.forEach { keyword ->
            if (userInput.contains(
                    keyword,
                    ignoreCase = true
                )
            ) emitenUntukDicek.add(keyword)
        }
        val hargaLiveArray = JSONArray()
        for (kode in emitenUntukDicek) {
            val symbolLookup = indexMap[kode] ?: kode;
            val harga = StockPriceHelper.getHargaLive(symbolLookup)
            if (harga != null) hargaLiveArray.put(JSONObject().apply {
                put(
                    "emiten",
                    kode
                ); put("harga", harga)
            })
        }
        return@withContext hargaLiveArray
    }

    private suspend fun fetchBeritaList(): JSONArray = withContext(Dispatchers.IO) {
        val beritaArray = JSONArray()
        try {
            client.newCall(
                Request.Builder().url(RSS_URL).addHeader("X-API-Key", API_KEY).get().build()
            ).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    if (jsonResponse.optString("status") == "success") {
                        val dataArray = jsonResponse.optJSONArray("data") ?: JSONArray()
                        for (i in 0 until minOf(dataArray.length(), 5)) {
                            val item = dataArray.getJSONObject(i)
                            beritaArray.put(JSONObject().apply {
                                put(
                                    "emiten",
                                    item.optString("emiten", "")
                                ); put("judul", item.optString("judul", ""))
                            })
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CHATBOT", "Error: ${e.localizedMessage}")
        }
        return@withContext beritaArray
    }

    suspend fun generateResponse(
        userInput: String,
        deviceId: String,
        portofolioEmitenList: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        try {
            val hargaLiveArray = fetchHargaLiveList(userInput, portofolioEmitenList);
            val beritaArray = fetchBeritaList()
            val bodyJson = JSONObject().apply {
                put("user_input", userInput); put(
                "device_id",
                deviceId
            ); put("harga_live_list", hargaLiveArray); put("berita_list", beritaArray)
            }
            val request = Request.Builder().url(CHATBOT_URL).addHeader("X-API-Key", API_KEY)
                .post(bodyJson.toString().toRequestBody(JSON)).build()
            client.newCall(request).execute().use { response ->
                var bodyString = response.body?.string() ?: ""
                if (bodyString.contains("{")) bodyString =
                    bodyString.substring(bodyString.indexOf("{"))
                if (bodyString.contains("}")) bodyString =
                    bodyString.substring(0, bodyString.lastIndexOf("}") + 1)
                if (response.isSuccessful) JSONObject(bodyString).optString(
                    "response",
                    "Maaf, sistem sedang sibuk."
                ) else "Error Server: ${response.code}"
            }
        } catch (e: IOException) {
            "Error: Koneksi bermasalah."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    suspend fun scrapeAndSaveEmiten(emiten: String, deviceId: String = "manual_scrape"): Boolean {
        return StockPriceHelper.syncFundamentalToBackend(emiten.uppercase().trim(), deviceId)
    }

    suspend fun isDataExists(emiten: String): Boolean {
        return StockPriceHelper.checkDataExists(emiten)
    }
}
