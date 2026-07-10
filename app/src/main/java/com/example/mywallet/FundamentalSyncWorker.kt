package com.example.mywallet

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.ChatBotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class FundamentalSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "FundamentalWorker"
        private const val MAX_PARALLEL_SYNC = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val daftarEmiten = getDaftarEmitenPantauan()
            if (daftarEmiten.isEmpty()) {
                saveLastSyncTime()
                return@withContext Result.success()
            }
            val semaphore = Semaphore(MAX_PARALLEL_SYNC)
            val results = daftarEmiten.map { emiten ->
                async {
                    semaphore.withPermit {
                        ChatBotService.scrapeAndSaveEmiten(
                            emiten,
                            "background_worker"
                        )
                    }
                }
            }.awaitAll()
            val jumlahBerhasil = results.count { it }
            saveLastSyncTime()
            if (jumlahBerhasil == 0 && results.isNotEmpty()) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getDaftarEmitenPantauan(): List<String> = emptyList()
    private fun saveLastSyncTime() {
        try {
            applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE).edit()
                .putLong("last_sync_time", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
        }
    }
}
