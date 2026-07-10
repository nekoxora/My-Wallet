package com.example.mywallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object StockPriceHelper {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
    private var yahooCookie: String? = null
    private var yahooCrumb: String? = null
    private suspend fun refreshCookieAndCrumb(): Boolean = withContext(Dispatchers.IO) {
        try {
            val res1 = client.newCall(
                Request.Builder().url("https://fc.yahoo.com").addHeader("User-Agent", "Mozilla/5.0")
                    .get().build()
            ).execute()
            val cookies = res1.headers("Set-Cookie")
            if (cookies.isNotEmpty()) yahooCookie = cookies.joinToString("; ") { it.split(";")[0] }
            res1.close()
            val res2 = client.newCall(
                Request.Builder().url("https://query2.finance.yahoo.com/v1/test/getcrumb")
                    .addHeader("Cookie", yahooCookie ?: "").addHeader("User-Agent", "Mozilla/5.0")
                    .get().build()
            ).execute()
            if (res2.isSuccessful) {
                yahooCrumb = res2.body?.string()
                res2.close()
                return@withContext !yahooCrumb.isNullOrEmpty()
            }
            res2.close()
            return@withContext false
        } catch (e: Exception) {
            false
        }
    }

    private const val PHP_API_URL = "http://43.133.150.113/api_keuangan/update_fundamental.php"
    private const val PHP_GET_URL = "http://43.133.150.113/api_keuangan/get_fundamental.php"
    private const val SECRET_API_KEY = "KODE_RAHASIA_ANDROID_123"
    suspend fun getHargaLiveWithMeta(symbol: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d"
            client.newCall(
                Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
            ).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                JSONObject(response.body?.string() ?: "").optJSONObject("chart")
                    ?.optJSONArray("result")?.getJSONObject(0)?.optJSONObject("meta")
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHargaLive(emiten: String): Double? =
        getHargaLiveWithMeta(buildSymbol(emiten))?.optDouble("regularMarketPrice")
            ?.takeIf { !it.isNaN() }

    suspend fun getPersentaseLive(symbol: String): String? = try {
        val meta = getHargaLiveWithMeta(symbol)
        val cur = meta?.optDouble("regularMarketPrice") ?: Double.NaN
        val pre = meta?.optDouble("chartPreviousClose") ?: Double.NaN
        if (cur.isNaN() || pre.isNaN() || pre == 0.0) null
        else "${if (cur >= pre) "+" else ""}${"%.1f".format(((cur - pre) / pre) * 100)}%"
    } catch (e: Exception) {
        null
    }

    suspend fun getHistoricalPrices(emiten: String, p1: Long, p2: Long): Map<Int, Double> =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://query1.finance.yahoo.com/v8/finance/chart/${buildSymbol(emiten)}?period1=$p1&period2=$p2&interval=1d"
                client.newCall(
                    Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyMap()
                    val res = JSONObject(response.body?.string() ?: "").optJSONObject("chart")
                        ?.optJSONArray("result")?.getJSONObject(0) ?: return@withContext emptyMap()
                    val ts = res.optJSONArray("timestamp") ?: return@withContext emptyMap()
                    val cl =
                        res.optJSONObject("indicators")?.optJSONArray("quote")?.getJSONObject(0)
                            ?.optJSONArray("close") ?: return@withContext emptyMap()
                    val map = mutableMapOf<Int, Double>()
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                    for (i in 0 until ts.length()) {
                        if (!cl.isNull(i)) {
                            cal.timeInMillis =
                                ts.getLong(i) * 1000; map[cal.get(Calendar.DAY_OF_MONTH)] =
                                cl.getDouble(i)
                        }
                    }
                    map
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun getMonthlyHistoricalPrices(emiten: String): Map<Int, Double> =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://query1.finance.yahoo.com/v8/finance/chart/${buildSymbol(emiten)}?range=1y&interval=1mo"
                client.newCall(
                    Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyMap()
                    val res = JSONObject(response.body?.string() ?: "").optJSONObject("chart")
                        ?.optJSONArray("result")?.getJSONObject(0) ?: return@withContext emptyMap()
                    val ts = res.optJSONArray("timestamp") ?: return@withContext emptyMap()
                    val cl =
                        res.optJSONObject("indicators")?.optJSONArray("quote")?.getJSONObject(0)
                            ?.optJSONArray("close") ?: return@withContext emptyMap()
                    val map = mutableMapOf<Int, Double>()
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                    for (i in 0 until ts.length()) {
                        if (!cl.isNull(i)) {
                            cal.timeInMillis =
                                ts.getLong(i) * 1000; map[cal.get(Calendar.MONTH) + 1] =
                                cl.getDouble(i)
                        }
                    }
                    map
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun scrapeFundamentalFromYahoo(emiten: String): FundamentalData? =
        withContext(Dispatchers.IO) {
            try {
                if (yahooCookie == null || yahooCrumb == null) refreshCookieAndCrumb()
                val url =
                    "https://query2.finance.yahoo.com/v10/finance/quoteSummary/${buildSymbol(emiten)}?modules=defaultKeyStatistics,financialData,summaryDetail&crumb=$yahooCrumb"
                client.newCall(
                    Request.Builder().url(url).addHeader("Cookie", yahooCookie ?: "")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .addHeader("Origin", "https://finance.yahoo.com").build()
                ).execute().use { response ->
                    if (response.code == 401 && refreshCookieAndCrumb()) return@withContext scrapeFundamentalFromYahoo(
                        emiten
                    )
                    if (!response.isSuccessful) return@withContext scrapeFundamentalFallback(emiten)
                    val json = JSONObject(response.body?.string() ?: "")
                    val data = (json.optJSONObject("quoteResponse")
                        ?: json.optJSONObject("quoteSummary"))?.optJSONArray("result")
                        ?.getJSONObject(0) ?: return@withContext scrapeFundamentalFallback(emiten)

                    fun raw(obj: JSONObject?, f: String): Double? =
                        obj?.optJSONObject(f)?.optDouble("raw")?.takeIf { !it.isNaN() }

                    val stats = data.optJSONObject("defaultKeyStatistics");
                    val summ = data.optJSONObject("summaryDetail");
                    val fin = data.optJSONObject("financialData")
                    FundamentalData(
                        emiten,
                        raw(fin, "currentPrice"),
                        raw(stats, "trailingEps"),
                        raw(stats, "bookValue"),
                        raw(summ, "trailingPE"),
                        raw(stats, "priceToBook"),
                        ""
                    )
                }
            } catch (e: Exception) {
                scrapeFundamentalFallback(emiten)
            }
        }

    private suspend fun scrapeFundamentalFallback(emiten: String): FundamentalData? =
        withContext(Dispatchers.IO) {
            val h = getHargaLive(emiten) ?: 0.0
            if (h > 0) FundamentalData(emiten, h, null, null, null, null, "") else null
        }

    private fun sanitize(raw: String): String =
        if (raw.contains("{")) raw.substring(raw.indexOf("{"), raw.lastIndexOf("}") + 1) else raw

    suspend fun syncFundamentalToBackend(
        emiten: String,
        deviceId: String = "auto_scrape"
    ): Boolean = withContext(Dispatchers.IO) {
        val f = scrapeFundamentalFromYahoo(emiten) ?: return@withContext false
        sendFundamentalDataList(deviceId, listOf(f))
    }

    suspend fun sendFundamentalDataList(deviceId: String, items: List<FundamentalData>): Boolean =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext false
            try {
                val arr = JSONArray()
                items.forEach { itm ->
                    arr.put(JSONObject().apply {
                        put(
                            "emiten",
                            itm.emiten.uppercase().trim()
                        ); put("harga_sekarang", itm.hargaSekarang ?: JSONObject.NULL); put(
                        "eps",
                        itm.eps ?: JSONObject.NULL
                    ); put("book_value", itm.bookValue ?: JSONObject.NULL); put(
                        "pe_ratio",
                        itm.peRatio ?: JSONObject.NULL
                    ); put("pb_ratio", itm.pbRatio ?: JSONObject.NULL)
                    })
                }
                val body =
                    JSONObject().apply { put("device_id", deviceId); put("data", arr) }.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                client.newCall(
                    Request.Builder().url("$PHP_API_URL?api_key=$SECRET_API_KEY")
                        .addHeader("X-API-Key", SECRET_API_KEY).post(body).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val res = JSONObject(sanitize(response.body?.string() ?: "{}"))
                    res.optString("status") == "success" || (res.optJSONObject("summary")
                        ?.optInt("success", 0) ?: 0) > 0
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun checkDataExists(emiten: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.newCall(
                Request.Builder().url("$PHP_GET_URL?emiten=${emiten.uppercase()}")
                    .addHeader("X-API-Key", SECRET_API_KEY).get().build()
            ).execute().use { response ->
                response.isSuccessful && JSONObject(
                    sanitize(
                        response.body?.string() ?: "{}"
                    )
                ).optString("status") == "success"
            }
        } catch (e: Exception) {
            false
        }
    }

    fun buildSymbol(e: String): String = when (val c = e.uppercase().trim()) {
        "IHSG" -> "^JKSE"; "LQ45" -> "^JKLQ45"; "DJIA" -> "^DJI"; "NASDAQ" -> "^IXIC"; "S&P500" -> "^GSPC"; else -> if (c.startsWith(
                "^"
            ) || c.endsWith(".JK") || c.contains("=")
        ) c else "$c.JK"
    }
}

data class FundamentalData(
    val emiten: String,
    val hargaSekarang: Double?,
    val eps: Double?,
    val bookValue: Double?,
    val peRatio: Double?,
    val pbRatio: Double?,
    val lastUpdated: String = ""
)
