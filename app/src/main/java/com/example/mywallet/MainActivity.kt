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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(); NotificationHelper.createChannel(this)
        val wm = WorkManager.getInstance(this);
        val repl = ExistingPeriodicWorkPolicy.REPLACE;
        val keep = ExistingPeriodicWorkPolicy.KEEP
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
            PeriodicWorkRequestBuilder<FundamentalSyncWorker>(24, TimeUnit.HOURS).setInitialDelay(
                1,
                TimeUnit.HOURS
            ).build()
        )
        wm.enqueueUniquePeriodicWork(
            "market_crawler",
            repl,
            PeriodicWorkRequestBuilder<MarketCrawlerWorker>(2, TimeUnit.HOURS).setInitialDelay(
                0,
                TimeUnit.MINUTES
            ).build()
        )
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
        if (BuildConfig.DEBUG) lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!ChatBotService.isDataExists("OASA")) ChatBotService.scrapeAndSaveEmiten(
                    "OASA",
                    "test"
                )
            } catch (e: Exception) {
                Log.e("Main", "Error: ${e.message}")
            }
        }
    }
}
