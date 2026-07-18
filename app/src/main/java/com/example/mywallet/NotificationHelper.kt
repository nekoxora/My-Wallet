package com.example.mywallet

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "berita_saham_channel"
    private const val ALERT_CHANNEL_ID = "price_alert_channel"
    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val m = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            m.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Berita Saham",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            m.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun canSend(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED else true

    private fun pi(ctx: Context, id: Int): PendingIntent? {
        val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }; return PendingIntent.getActivity(
            ctx,
            id,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun sendBeritaNotif(ctx: Context, id: Int, e: String, j: String) {
        if (!canSend(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_wallet)
            .setContentTitle(e).setContentText(j)
            .setStyle(NotificationCompat.BigTextStyle().bigText(j))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(pi(ctx, id))
            .setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    @SuppressLint("MissingPermission")
    fun sendPriceAlertNotif(ctx: Context, e: String, p: Double, r: Double) {
        if (!canSend(ctx)) return
        val msg = "Price : ${
            "%,.0f".format(Locale.US, p).replace(',', '.')
        }         ${if (r >= 0) "+" else ""}${"%.0f".format(Locale.US, r)}%"
        val n = NotificationCompat.Builder(ctx, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet).setContentTitle(e).setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pi(ctx, e.hashCode()))
            .setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(e.hashCode(), n)
    }

    @SuppressLint("MissingPermission")
    fun sendBiRateNotif(ctx: Context, t: String, r: String) {
        if (!canSend(ctx)) return
        val n = NotificationCompat.Builder(ctx, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet).setContentTitle("BI Rate Update")
            .setContentText("Tanggal: $t | Rate: $r").setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi(ctx, 999)).setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(999, n)
    }
}
