package com.example.mywallet

import android.content.Context
import java.util.UUID

object DeviceIdHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getString(KEY_DEVICE_ID, null)

        if (existingId != null) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }
}