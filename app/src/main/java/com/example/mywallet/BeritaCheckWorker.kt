package com.example.mywallet

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BeritaCheckWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.instance.getBerita()
            if (res.status == "success") {
                val prefs =
                    applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val sent = prefs.getStringSet("notif_id_terkirim", emptySet()) ?: emptySet()
                val id = DeviceIdHelper.getDeviceId(applicationContext);
                val h = RetrofitClient.instance.getHistori(id)
                val uE = h.map { it.emiten.uppercase().trim() }.toSet()
                val new = res.data.filter {
                    it.id !in sent && BeritaFilterHelper.isBeritaRelevant(
                        it,
                        uE
                    )
                }
                new.forEach { b ->
                    NotificationHelper.sendBeritaNotif(
                        applicationContext,
                        b.id.hashCode(),
                        b.emiten,
                        b.judul
                    )
                }
                val all = res.data.map { it.id }.toSet(); prefs.edit()
                    .putStringSet("notif_id_terkirim", sent + all).apply()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
