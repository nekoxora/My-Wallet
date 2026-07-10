package com.example.mywallet

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mywallet.data.ChatBotService
import com.example.mywallet.ui.theme.BgDark
import com.example.mywallet.ui.theme.MainApp
import com.example.mywallet.ui.theme.MyWalletTheme
import com.google.ai.client.generativeai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "berita_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BeritaCheckWorker>(15, TimeUnit.MINUTES).build()
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "price_alert_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES).build()
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fundamental_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FundamentalSyncWorker>(24, TimeUnit.HOURS).setInitialDelay(
                1,
                TimeUnit.HOURS
            ).build()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        setContent {
            MyWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark,
                    contentColor = Color.White
                ) { MainApp() }
            }
        }
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val emitenTest = "OASA"
                    if (!ChatBotService.isDataExists(emitenTest)) {
                        ChatBotService.scrapeAndSaveEmiten(emitenTest, "test_device")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                }
            }
        }
    }
}
