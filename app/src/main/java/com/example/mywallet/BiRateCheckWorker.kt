package com.example.mywallet.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.NotificationHelper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class BiRateCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val data = BiRateHelper.fetchLatestBiRate() ?: return Result.retry()
        val body = FormBody.Builder().add("tanggal", data.tanggal).add("rate", data.rate)
            .add("link", data.linkSiaranPers).add("api_key", "KODE_RAHASIA_ANDROID_123").build()
        val request = Request.Builder().url("http://43.133.150.113/api_keuangan/simpan_bi_rate.php")
            .post(body).build()
        return try {
            OkHttpClient().newCall(request).execute().use { r ->
                if (!r.isSuccessful) return Result.retry()
                if ((r.body?.string()
                        ?: "").contains("\"is_new\":true")
                ) NotificationHelper.sendBiRateNotif(applicationContext, data.tanggal, data.rate)
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
