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
                val deviceId = DeviceIdHelper.getDeviceId(context)
                val historiResponse = RetrofitClient.instance.getHistori(deviceId)
                val userEmitens = historiResponse.map { it.emiten.uppercase().trim() }.toSet()

                val idTerkirimSebelumnya =
                    prefs.getStringSet("notif_id_terkirim", emptySet()) ?: emptySet()
                val idCleared =
                    prefs.getStringSet("cleared_berita_ids", emptySet()) ?: emptySet()

                val beritaBaru = beritaResponse.data.filter {
                    it.id !in idTerkirimSebelumnya &&
                            it.id !in idCleared &&
                            BeritaFilterHelper.isBeritaRelevant(it, userEmitens)
                }

                if (beritaBaru.isNotEmpty()) {
                    beritaBaru.forEach { berita ->
                        NotificationHelper.sendBeritaNotif(
                            context = context,
                            notifId = berita.id.hashCode(),
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