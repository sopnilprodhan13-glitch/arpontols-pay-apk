package com.arpontols.paygateway

import android.content.Context
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefs {
    private const val FILE = "arpontols_pay_prefs"
    private const val K_NAME = "device_name"
    private const val K_KEY = "device_key"
    private const val K_URL = "webhook_url"
    private const val K_ACTIVE = "service_active"
    private const val K_COUNT = "forwarded_count"
    private const val K_LOGS = "logs_text"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getDeviceName(ctx: Context): String = sp(ctx).getString(K_NAME, "Arpon-Pay-Phone") ?: "Arpon-Pay-Phone"
    fun setDeviceName(ctx: Context, v: String) = sp(ctx).edit().putString(K_NAME, v.trim()).apply()

    fun getDeviceKey(ctx: Context): String = sp(ctx).getString(K_KEY, "") ?: ""
    fun generateDeviceKey(ctx: Context, deviceName: String): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        val cleanPrefix = deviceName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").take(6).ifEmpty { "pay" }
        val key = "arp_${cleanPrefix}_$hex"
        sp(ctx).edit().putString(K_KEY, key).putString(K_NAME, deviceName.trim()).apply()
        return key
    }

    fun getWebhookUrl(ctx: Context): String = sp(ctx).getString(K_URL, "") ?: ""
    fun setWebhookUrl(ctx: Context, v: String) = sp(ctx).edit().putString(K_URL, v.trim()).apply()

    fun isServiceActive(ctx: Context): Boolean = sp(ctx).getBoolean(K_ACTIVE, false)
    fun setServiceActive(ctx: Context, active: Boolean) = sp(ctx).edit().putBoolean(K_ACTIVE, active).apply()

    fun getCount(ctx: Context): Int = sp(ctx).getInt(K_COUNT, 0)
    fun incrementCount(ctx: Context) {
        val c = getCount(ctx) + 1
        sp(ctx).edit().putInt(K_COUNT, c).apply()
    }

    fun appendLog(ctx: Context, line: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val existing = sp(ctx).getString(K_LOGS, "") ?: ""
        val merged = ("[$ts] $line\n$existing").lines().take(25).joinToString("\n")
        sp(ctx).edit().putString(K_LOGS, merged).apply()
    }

    fun getLogs(ctx: Context): String = sp(ctx).getString(K_LOGS, "") ?: ""
}
