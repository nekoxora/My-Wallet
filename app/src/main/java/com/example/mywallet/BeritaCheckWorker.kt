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
                val idTerkirimSebelumnya =
                    prefs.getStringSet("notif_id_terkirim", emptySet()) ?: emptySet()
                val beritaBaru = beritaResponse.data.filter { it.id !in idTerkirimSebelumnya }

                if (beritaBaru.isNotEmpty()) {
                    prefs.edit().putBoolean("berita_cleared", false).apply()

                    beritaBaru.forEachIndexed { index, berita ->
                        NotificationHelper.sendBeritaNotif(
                            context = context,
                            notifId = 2000 + index,
                            emiten = berita.emiten,
                            judul = berita.judul
                        )
                    }

                    val semuaIdSekarang = beritaResponse.data.map { it.id }.toSet()
                    prefs.edit().putStringSet("notif_id_terkirim", semuaIdSekarang).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}