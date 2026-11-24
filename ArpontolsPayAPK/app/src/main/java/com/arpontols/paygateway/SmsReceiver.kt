package com.arpontols.paygateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isServiceActive(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: ""
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val parsed = SmsParser.parse(sender, fullBody) ?: return

        val pending = goAsync()
        WebhookSender.sendPaymentAsync(context, parsed) { _, _ ->
            pending.finish()
        }
    }
}
