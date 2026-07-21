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
    private var cookie: String? = null;
    private var crumb: String? = null
    private fun k(): String = "KODE_" + "RAHASIA_" + "ANDROID_123"
    private suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        try {
            val r1 = client.newCall(
                Request.Builder().url("https://fc.yahoo.com").addHeader("User-Agent", "Mozilla/5.0")
                    .get().build()
            ).execute()
            val c = r1.headers("Set-Cookie"); if (c.isNotEmpty()) cookie =
                c.joinToString("; ") { it.split(";")[0] }; r1.close()
            val r2 = client.newCall(
                Request.Builder().url("https://query2.finance.yahoo.com/v1/test/getcrumb")
                    .addHeader("Cookie", cookie ?: "").addHeader("User-Agent", "Mozilla/5.0").get()
                    .build()
            ).execute()
            if (r2.isSuccessful) {
                crumb = r2.body?.string(); r2.close(); return@withContext !crumb.isNullOrEmpty()
            }; r2.close(); false
        } catch (e: Exception) {
            false
        }
    }

    private const val API = "http://43.133.150.113/api_keuangan/update_fundamental.php";
    private const val GET = "http://43.133.150.113/api_keuangan/get_fundamental.php"
    private const val BI_API = "http://43.133.150.113/api_keuangan/get_bi_rate.php"
    suspend fun getHargaLiveWithMeta(s: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            client.newCall(
                Request.Builder()
                    .url("https://query1.finance.yahoo.com/v8/finance/chart/$s?interval=1d&range=1d")
                    .addHeader("User-Agent", "Mozilla/5.0").build()
            ).execute().use { r ->
                if (!r.isSuccessful) null else JSONObject(
                    r.body?.string() ?: ""
                ).optJSONObject("chart")?.optJSONArray("result")?.getJSONObject(0)
                    ?.optJSONObject("meta")
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getHargaLive(e: String): Double? =
        getHargaLiveWithMeta(buildSymbol(e))?.optDouble("regularMarketPrice")
            ?.takeIf { !it.isNaN() }

    suspend fun getPersentaseLive(s: String): String? = try {
        val m = getHargaLiveWithMeta(s);
        val c = m?.optDouble("regularMarketPrice") ?: Double.NaN;
        val p = m?.optDouble("chartPreviousClose") ?: Double.NaN
        if (c.isNaN() || p.isNaN() || p == 0.0) null else "${if (c >= p) "+" else ""}${
            "%.1f".format(
                java.util.Locale.US,
                ((c - p) / p) * 100
            )
        }%"
    } catch (e: Exception) {
        null
    }

    suspend fun getHistoricalPrices(e: String, p1: Long, p2: Long): Map<Int, Double> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(
                    Request.Builder()
                        .url("https://query1.finance.yahoo.com/v8/finance/chart/${buildSymbol(e)}?period1=$p1&period2=$p2&interval=1d")
                        .addHeader("User-Agent", "Mozilla/5.0").build()
                ).execute().use { r ->
                    if (!r.isSuccessful) emptyMap() else {
                        val j = JSONObject(r.body?.string() ?: "").optJSONObject("chart")
                            ?.optJSONArray("result")?.getJSONObject(0)
                            ?: return@withContext emptyMap()
                        val ts = j.optJSONArray("timestamp") ?: return@withContext emptyMap();
                        val cl =
                            j.optJSONObject("indicators")?.optJSONArray("quote")?.getJSONObject(0)
                                ?.optJSONArray("close") ?: return@withContext emptyMap()
                        val map = mutableMapOf<Int, Double>();
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                        for (i in 0 until ts.length()) {
                            if (!cl.isNull(i)) {
                                cal.timeInMillis =
                                    ts.getLong(i) * 1000; map[cal.get(Calendar.DAY_OF_MONTH)] =
                                    cl.getDouble(i)
                            }
                        }; map
                    }
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun getMonthlyHistoricalPrices(e: String): Map<Int, Double> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(
                    Request.Builder()
                        .url("https://query1.finance.yahoo.com/v8/finance/chart/${buildSymbol(e)}?range=1y&interval=1mo")
                        .addHeader("User-Agent", "Mozilla/5.0").build()
                ).execute().use { r ->
                    if (!r.isSuccessful) emptyMap() else {
                        val j = JSONObject(r.body?.string() ?: "").optJSONObject("chart")
                            ?.optJSONArray("result")?.getJSONObject(0)
                            ?: return@withContext emptyMap()
                        val ts = j.optJSONArray("timestamp") ?: return@withContext emptyMap();
                        val cl =
                            j.optJSONObject("indicators")?.optJSONArray("quote")?.getJSONObject(0)
                                ?.optJSONArray("close") ?: return@withContext emptyMap()
                        val map = mutableMapOf<Int, Double>();
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                        for (i in 0 until ts.length()) {
                            if (!cl.isNull(i)) {
                                cal.timeInMillis =
                                    ts.getLong(i) * 1000; map[cal.get(Calendar.MONTH) + 1] =
                                    cl.getDouble(i)
                            }
                        }; map
                    }
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun scrapeFundamentalFromYahoo(e: String): FundamentalData? =
        withContext(Dispatchers.IO) {
            try {
                if (cookie == null || crumb == null) refresh()
                val url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/${
                    buildSymbol(
                        e,
                        false
                    )
                }?modules=defaultKeyStatistics,financialData,summaryDetail&crumb=$crumb"
                client.newCall(
                    Request.Builder().url(url).addHeader("Cookie", cookie ?: "")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .addHeader("Origin", "https://finance.yahoo.com").build()
                ).execute().use { r ->
                    if (r.code == 401 && refresh()) return@withContext scrapeFundamentalFromYahoo(e)
                    if (!r.isSuccessful) return@withContext fallback(e)
                    val json = JSONObject(r.body?.string() ?: "")
                    val d =
                        json.optJSONObject("quoteSummary")?.optJSONArray("result")?.optJSONObject(0)
                            ?: return@withContext fallback(e)

                    fun rw(o: JSONObject?, f: String): Double? =
                        o?.optJSONObject(f)?.optDouble("raw")?.takeIf { !it.isNaN() }

                    val stats = d.optJSONObject("defaultKeyStatistics");
                    val summ = d.optJSONObject("summaryDetail");
                    val fin = d.optJSONObject("financialData")
                    FundamentalData(
                        e,
                        rw(fin, "currentPrice"),
                        rw(stats, "trailingEps"),
                        rw(stats, "bookValue"),
                        rw(summ, "trailingPE"),
                        rw(stats, "priceToBook"),
                        ""
                    )
                }
            } catch (ex: Exception) {
                fallback(e)
            }
        }

    private suspend fun fallback(e: String): FundamentalData? {
        val h = getHargaLive(e) ?: 0.0; return if (h > 0) FundamentalData(
            e,
            h,
            null,
            null,
            null,
            null,
            ""
        ) else null
    }

    private fun sz(r: String): String =
        if (r.contains("{")) r.substring(r.indexOf("{"), r.lastIndexOf("}") + 1) else r

    suspend fun syncFundamentalToBackend(e: String, id: String = "auto"): Boolean =
        withContext(Dispatchers.IO) {
            val f =
                scrapeFundamentalFromYahoo(e) ?: return@withContext false; sendFundamentalDataList(
            id,
            listOf(f)
        )
        }

    suspend fun sendFundamentalDataList(id: String, l: List<FundamentalData>): Boolean =
        withContext(Dispatchers.IO) {
            val filt = l.filter { it.eps != null || it.bookValue != null }
            if (filt.isEmpty()) return@withContext false
            try {
                val a = JSONArray(); l.forEach { i ->
                    a.put(JSONObject().apply {
                        put(
                            "emiten",
                            i.emiten.uppercase().trim()
                        ); put("harga_sekarang", i.hargaSekarang ?: JSONObject.NULL); put(
                        "eps",
                        i.eps ?: JSONObject.NULL
                    ); put("book_value", i.bookValue ?: JSONObject.NULL); put(
                        "pe_ratio",
                        i.peRatio ?: JSONObject.NULL
                    ); put("pb_ratio", i.pbRatio ?: JSONObject.NULL)
                    })
                }
                val b = JSONObject().apply { put("device_id", id); put("data", a) }.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                client.newCall(
                    Request.Builder().url("$API?api_key=${k()}").addHeader("X-API-Key", k()).post(b)
                        .build()
                ).execute().use { r ->
                    if (!r.isSuccessful) false else {
                        val res = JSONObject(
                            sz(
                                r.body?.string() ?: "{}"
                            )
                        ); res.optString("status") == "success" || (res.optJSONObject("summary")
                            ?.optInt("success", 0) ?: 0) > 0
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun checkDataExists(e: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.newCall(
                Request.Builder().url("$GET?emiten=${e.uppercase()}").addHeader("X-API-Key", k())
                    .get().build()
            ).execute().use { r ->
                r.isSuccessful && JSONObject(
                    sz(
                        r.body?.string() ?: "{}"
                    )
                ).optString("status") == "success"
            }
        } catch (ex: Exception) {
            false
        }
    }

    fun buildSymbol(e: String, isGlobal: Boolean = false): String {
        val c = e.uppercase().trim()
        val indMap = mapOf(
            "IHSG" to "^JKSE",
            "LQ45" to "^JKLQ45",
            "NASDAQ" to "^IXIC",
            "DJIA" to "^DJI",
            "S&P500" to "^GSPC"
        )
        val comMap =
            mapOf("COAL" to "MTF=F", "GOLD" to "GC=F", "WTI OIL" to "CL=F", "OIL" to "CL=F")
        if (indMap.containsKey(c)) return indMap[c]!!
        if (isGlobal && comMap.containsKey(c)) return comMap[c]!!
        return if (c.startsWith("^") || c.endsWith(".JK") || c.contains("=")) c else "$c.JK"
    }

    suspend fun getBiRateValue(): String? = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(BI_API).build()).execute().use { r ->
                if (!r.isSuccessful) "-" else JSONObject(
                    r.body?.string() ?: "{}"
                ).optString("rate", "-")
            }
        } catch (e: Exception) {
            "-"
        }
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
