package com.example.mywallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateHelper {
    private const val TAG = "UPDATE_CHECK"
    private const val URL = "http://43.133.150.113/api_keuangan/version.json"
    suspend fun checkUpdate(ctx: Context): JSONObject? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking update from: $URL")
            val res = OkHttpClient().newCall(Request.Builder().url(URL).build()).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: "{}"
                Log.d(TAG, "Response: $body")
                val json = JSONObject(body)
                val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                val cur =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
                val server = json.optLong("version_code")
                Log.d(TAG, "Local version: $cur, Server version: $server")
                if (server > cur) return@withContext json
            } else {
                Log.e(TAG, "Request failed: ${res.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update: ${e.message}")
        }
        null
    }
}

@Composable
fun UpdateDialog(json: JSONObject, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembaruan Tersedia") },
        text = { Text("Versi baru (${json.optString("version_name")}) sudah tersedia. Klik Perbarui untuk mengunduh versi terbaru.") },
        confirmButton = {
            Button(onClick = {
                ctx.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(json.optString("download_url"))
                    )
                ); onDismiss()
            }) { Text("Perbarui") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Nanti") } })
}
