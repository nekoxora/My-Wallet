package com.example.mywallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationDeleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val beritaId = intent.getStringExtra("berita_id")
        if (beritaId != null) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val clearedIds =
                prefs.getStringSet("cleared_berita_ids", mutableSetOf())?.toMutableSet()
                    ?: mutableSetOf()

            clearedIds.add(beritaId)

            prefs.edit().putStringSet("cleared_berita_ids", clearedIds).apply()
            Log.d("NOTIF_DELETE", "Berita ID $beritaId marked as cleared from notification tray")
        }
    }
}