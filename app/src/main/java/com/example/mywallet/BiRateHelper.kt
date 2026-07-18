package com.example.mywallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class BiRateData(
    val tanggal: String,
    val rate: String,
    val linkSiaranPers: String
)

object BiRateHelper {

    private const val URL = "https://www.bi.go.id/id/statistik/indikator/bi-rate.aspx"
    private const val BASE_URL = "https://www.bi.go.id"

    suspend fun fetchLatestBiRate(): BiRateData? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .timeout(15000)
                .get()

            val firstRow =
                doc.select("#tableData table tbody tr").first() ?: return@withContext null
            val cols = firstRow.select("td")

            val tanggal = cols[0].text().trim()
            val rate = cols[1].text().replace("%", "").trim()
            val linkRelatif = cols[2].select("a").attr("href")
            val link = if (linkRelatif.startsWith("http")) linkRelatif else "$BASE_URL$linkRelatif"

            BiRateData(tanggal, rate, link)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}