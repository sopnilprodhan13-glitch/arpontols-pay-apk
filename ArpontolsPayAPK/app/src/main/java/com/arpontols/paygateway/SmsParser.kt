package com.arpontols.paygateway

import java.util.Locale

data class ParsedPaymentSms(
    val gateway: String,
    val txId: String,
    val amountBdt: Double,
    val sender: String,
    val rawText: String
)

object SmsParser {
    private val TRX_REGEX = Regex("""(?:TrxID|TxnId|TxID|Trx\s*ID)\s*[:\-]?\s*([A-Za-z0-9]{8,14})""", RegexOption.IGNORE_CASE)
    private val AMT_REGEX = Regex("""(?:Tk|BDT)\s*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    fun parse(sender: String, body: String): ParsedPaymentSms? {
        val sLow = sender.lowercase(Locale.US)
        val bLow = body.lowercase(Locale.US)

        // Only process incoming money-received payment SMS (skip OTPs & outgoing transfers)
        val isIncoming = bLow.contains("received") || bLow.contains("cash in") || bLow.contains("payment") || bLow.contains("credited")
        if (!isIncoming) return null
        if (bLow.contains("otp") || bLow.contains("pin") || bLow.contains("verification code")) return null

        val gateway = when {
            sLow.contains("bkash") || sLow == "16247" || bLow.contains("bkash") -> "bkash_manual"
            sLow.contains("nagad") || sLow == "16167" || bLow.contains("nagad") -> "nagad_manual"
            sLow.contains("rocket") || sLow.contains("dbbl") || bLow.contains("rocket") -> "rocket_manual"
            sLow.contains("upay") || bLow.contains("upay") -> "upay_manual"
            else -> return null
        }

        val txMatch = TRX_REGEX.find(body) ?: return null
        val txId = txMatch.groupValues[1].uppercase(Locale.US)

        val amtMatch = AMT_REGEX.find(body)
        val amtBdt = amtMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        return ParsedPaymentSms(
            gateway = gateway,
            txId = txId,
            amountBdt = amtBdt,
            sender = sender,
            rawText = body
        )
    }
}
