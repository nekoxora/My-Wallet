package com.example.mywallet
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.ChatBotService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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
            if (cookie == null || crumb == null) refresh()
            val p = applicationContext.getSharedPreferences("crawler_prefs", Context.MODE_PRIVATE)
            val off = p.getInt("last_offset", 0); val size = 100
            val res = fetchWithTotal(off, size)
            val tks = res.first; val total = res.second
            if (tks.isEmpty()) { Log.d("CRAWLER", "End reached. Resetting."); p.edit().putInt("last_offset", 0).apply(); return@withContext Result.success() }
            val current = off + tks.size
            Log.d("CRAWLER", "Progress: $current / $total (" + String.format("%.1f", (current.toFloat()/total)*100) + "%)")
            Log.d("CRAWLER", "Remaining: " + (total - current))
            val sem = Semaphore(3)
            tks.map { e -> async { sem.withPermit { ChatBotService.scrapeAndSaveEmiten(e, "crawler") } } }.awaitAll()
            p.edit().putInt("last_offset", off + size).apply(); Result.success()
        } catch (e: Exception) { Result.retry() }
    }
    private suspend fun fetchWithTotal(off: Int, sz: Int): Pair<List<String>, Int> = withContext(Dispatchers.IO) {
        try {
            val url = "https://query1.finance.yahoo.com/v1/finance/screener?crumb=$crumb&lang=en-US&region=US"
            val body = "{\"size\":$sz,\"offset\":$off,\"sortField\":\"intradaymarketcap\",\"sortType\":\"DESC\",\"quoteType\":\"EQUITY\",\"query\":{\"operator\":\"and\",\"operands\":[{\"operator\":\"eq\",\"operands\":[\"region\",\"id\"]}]}}".toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url).post(body).addHeader("Cookie", cookie ?: "").addHeader("User-Agent", "Mozilla/5.0").addHeader("Origin", "https://finance.yahoo.com").build()
            client.newCall(req).execute().use { r ->
                if (r.code == 401 && refresh()) return@withContext fetchWithTotal(off, sz)
                if (!r.isSuccessful) return@withContext Pair(emptyList(), 0)
                val j = JSONObject(r.body?.string() ?: "").optJSONObject("finance")?.optJSONArray("result")?.optJSONObject(0) ?: return@withContext Pair(emptyList(), 0)
                val total = j.optInt("total", 0)
                val quotes = j.optJSONArray("quotes") ?: return@withContext Pair(emptyList(), total)
                val list = mutableListOf<String>()
                for (i in 0 until quotes.length()) { val s = quotes.optJSONObject(i)?.optString("symbol", ""); if (s != null && s.endsWith(".JK")) list.add(s.replace(".JK", "")) }
                Pair(list, total)
            }
        } catch (e: Exception) { Pair(emptyList(), 0) }
    }
}
