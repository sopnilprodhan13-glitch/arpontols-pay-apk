package com.arpontols.paygateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isServiceActive(context)) {
            val svc = Intent(context, GatewayForegroundService::class.java)
            ContextCompat.startForegroundService(context, svc)
        }
    }
}
