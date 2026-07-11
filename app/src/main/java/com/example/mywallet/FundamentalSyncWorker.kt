package com.example.mywallet

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mywallet.data.ChatBotService
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class FundamentalSyncWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val id = DeviceIdHelper.getDeviceId(context);
            val list = RetrofitClient.instance.getHistori(id).map { it.emiten.uppercase().trim() }
                .distinct()
            if (list.isEmpty()) return@withContext Result.success()
            val sem = Semaphore(5)
            val res = list.map { e ->
                async {
                    sem.withPermit {
                        ChatBotService.scrapeAndSaveEmiten(
                            e,
                            "worker"
                        )
                    }
                }
            }.awaitAll()
            if (res.all { !it } && res.isNotEmpty()) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
