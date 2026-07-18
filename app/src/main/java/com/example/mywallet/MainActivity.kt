package com.example.mywallet

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.example.mywallet.data.BiRateCheckWorker
import com.example.mywallet.data.ChatBotService
import com.example.mywallet.ui.theme.BgDark
import com.example.mywallet.ui.theme.MainApp
import com.example.mywallet.ui.theme.MyWalletTheme
import com.google.ai.client.generativeai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(); NotificationHelper.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MyWalletTheme {
                Surface(
                    Modifier.fillMaxSize(),
                    color = BgDark,
                    contentColor = Color.White
                ) { MainApp() }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val wm = WorkManager.getInstance(applicationContext);
            val repl = ExistingPeriodicWorkPolicy.REPLACE;
            val keep = ExistingPeriodicWorkPolicy.KEEP
            wm.enqueueUniquePeriodicWork(
                "bi_rate_sync",
                repl,
                PeriodicWorkRequestBuilder<BiRateCheckWorker>(12, TimeUnit.HOURS).build()
            )
            wm.enqueueUniquePeriodicWork(
                "market_crawler",
                repl,
                PeriodicWorkRequestBuilder<MarketCrawlerWorker>(2, TimeUnit.HOURS).build()
            )
            wm.enqueueUniquePeriodicWork(
                "berita_check",
                keep,
                PeriodicWorkRequestBuilder<BeritaCheckWorker>(15, TimeUnit.MINUTES).build()
            )
            wm.enqueueUniquePeriodicWork(
                "price_alert_check",
                keep,
                PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES).build()
            )
            wm.enqueueUniquePeriodicWork(
                "fundamental_check",
                keep,
                PeriodicWorkRequestBuilder<FundamentalSyncWorker>(24, TimeUnit.HOURS).build()
            )
            if (BuildConfig.DEBUG) try {
                if (!ChatBotService.isDataExists("OASA")) ChatBotService.scrapeAndSaveEmiten(
                    "OASA",
                    "test"
                )
            } catch (e: Exception) {
            }
        }
    }
}
