package com.arpontols.paygateway

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object WebhookSender {
    private val executor = Executors.newSingleThreadExecutor()

    fun sendPaymentAsync(ctx: Context, payment: ParsedPaymentSms, onDone: ((Boolean, String) -> Unit)? = null) {
        executor.execute {
            val urlStr = Prefs.getWebhookUrl(ctx)
            val key = Prefs.getDeviceKey(ctx)
            val name = Prefs.getDeviceName(ctx)
            if (urlStr.isBlank()) {
                Prefs.appendLog(ctx, "SKIP ${payment.txId}: Webhook URL not set")
                onDone?.invoke(false, "Webhook URL not set")
                return@execute
            }

            val payload = JSONObject().apply {
                put("gateway", payment.gateway)
                put("tx_id", payment.txId)
                put("amount_bdt", payment.amountBdt)
                put("sender", payment.sender)
                put("device_name", name)
                put("device_key", key)
                put("text", payment.rawText)
            }

            var lastErr = "Unknown error"
            for (attempt in 1..3) {
                try {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 12000
                        readTimeout = 12000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("X-Device-Key", key)
                    }
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code in 200..299) {
                        Prefs.incrementCount(ctx)
                        Prefs.appendLog(ctx, "OK ${payment.gateway} | ${payment.txId} | Tk ${payment.amountBdt}")
                        onDone?.invoke(true, "HTTP $code")
                        return@execute
                    } else {
                        lastErr = "HTTP $code"
                    }
                } catch (e: Exception) {
                    lastErr = e.message ?: e.javaClass.simpleName
                }
                Thread.sleep(2000L * attempt)
            }
            Prefs.appendLog(ctx, "FAIL ${payment.txId}: $lastErr")
            onDone?.invoke(false, lastErr)
        }
    }

    fun sendTestPing(ctx: Context, onResult: (Boolean, String) -> Unit) {
        val sample = ParsedPaymentSms(
            gateway = "bkash_manual",
            txId = "TESTPING01",
            amountBdt = 1.0,
            sender = "ArpontolsTest",
            rawText = "Test ping from ${Prefs.getDeviceName(ctx)} TrxID TESTPING01 Tk 1.00"
        )
        sendPaymentAsync(ctx, sample, onResult)
    }
}
