package com.example.mywallet

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.RetrofitClient

class BeritaCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            val beritaResponse = RetrofitClient.instance.getBerita()

            if (beritaResponse.status == "success") {
                val totalBerita = beritaResponse.total
                val sudahDibaca = prefs.getInt("notif_dibaca", 0)
                val belumDibaca = (totalBerita - sudahDibaca).coerceAtLeast(0)

                if (belumDibaca > 0) {
                    val beritaBaru = beritaResponse.data.take(belumDibaca)
                    beritaBaru.forEachIndexed { index, berita ->
                        NotificationHelper.sendBeritaNotif(
                            context = context,
                            notifId = 2000 + index,
                            emiten = berita.emiten,
                            judul = berita.judul
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}