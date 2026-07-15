package com.example.mywallet
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.ChatBotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MarketCrawlerWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    private var cookie: String? = null; private var crumb: String? = null
    private suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        try {
            val r1 = client.newCall(Request.Builder().url("https://fc.yahoo.com").addHeader("User-Agent", "Mozilla/5.0").get().build()).execute()
            val c = r1.headers("Set-Cookie"); if (c.isNotEmpty()) cookie = c.joinToString("; ") { it.split(";")[0] }; r1.close()
            val r2 = client.newCall(Request.Builder().url("https://query2.finance.yahoo.com/v1/test/getcrumb").addHeader("Cookie", cookie ?: "").addHeader("User-Agent", "Mozilla/5.0").get().build()).execute()
            if (r2.isSuccessful) { crumb = r2.body?.string(); r2.close(); return@withContext !crumb.isNullOrEmpty() }; r2.close(); false
        } catch (e: Exception) { false }
    }
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CRAWLER", "Starting Dynamic Market Crawler...")
            if (cookie == null || crumb == null) refresh()
            val p = applicationContext.getSharedPreferences("crawler_prefs", Context.MODE_PRIVATE)
            val off = p.getInt("last_offset", 0); val size = 100
            val tks = fetchAllIndoTickers(off, size)
            if (tks.isEmpty()) { Log.d("CRAWLER", "Reached end or no data, resetting"); p.edit().putInt("last_offset", 0).apply(); return@withContext Result.success() }
            Log.d("CRAWLER", "Fetched ${tks.size} tickers from market. Processing batch...")
            val sem = Semaphore(3)
            tks.map { e -> async { sem.withPermit { ChatBotService.scrapeAndSaveEmiten(e, "crawler") } } }.awaitAll()
            p.edit().putInt("last_offset", off + size).apply(); Result.success()
        } catch (e: Exception) { Result.retry() }
    }
    private suspend fun fetchAllIndoTickers(off: Int, sz: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://query1.finance.yahoo.com/v1/finance/screener?crumb=$crumb&lang=en-US&region=US"
            val body = "{\"size\":$sz,\"offset\":$off,\"sortField\":\"intradaymarketcap\",\"sortType\":\"DESC\",\"quoteType\":\"EQUITY\",\"query\":{\"operator\":\"and\",\"operands\":[{\"operator\":\"eq\",\"operands\":[\"region\",\"id\"]}]}}".toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url).post(body).addHeader("Cookie", cookie ?: "").addHeader("User-Agent", "Mozilla/5.0").addHeader("Origin", "https://finance.yahoo.com").build()
            client.newCall(req).execute().use { r ->
                if (r.code == 401 && refresh()) return@withContext fetchAllIndoTickers(off, sz)
                if (!r.isSuccessful) return@withContext emptyList()
                val res = JSONObject(r.body?.string() ?: "").optJSONObject("finance")?.optJSONArray("result")?.optJSONObject(0)?.optJSONArray("quotes") ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until res.length()) { val s = res.optJSONObject(i)?.optString("symbol", ""); if (s != null && s.endsWith(".JK")) list.add(s.replace(".JK", "")) }
                list
            }
        } catch (e: Exception) { emptyList() }
    }
}
