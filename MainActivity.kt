package com.arpontols.paygateway

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatusBadge: TextView
    private lateinit var etDeviceName: EditText
    private lateinit var tvDeviceKey: TextView
    private lateinit var etWebhookUrl: EditText
    private lateinit var btnToggleService: Button
    private lateinit var tvLogHeader: TextView
    private lateinit var tvLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        etDeviceName = findViewById(R.id.etDeviceName)
        tvDeviceKey = findViewById(R.id.tvDeviceKey)
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        btnToggleService = findViewById(R.id.btnToggleService)
        tvLogHeader = findViewById(R.id.tvLogHeader)
        tvLogs = findViewById(R.id.tvLogs)

        findViewById<Button>(R.id.btnGenerateKey).setOnClickListener {
            val name = etDeviceName.text.toString().trim().ifEmpty { "Arpon-Pay-Phone" }
            val key = Prefs.generateDeviceKey(this, name)
            tvDeviceKey.text = key
            Toast.makeText(this, "New Device Key generated!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCopyKey).setOnClickListener {
            val key = Prefs.getDeviceKey(this)
            if (key.isBlank()) {
                Toast.makeText(this, "Generate a key first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ArpontolsKey", key))
            Toast.makeText(this, "Device Key copied!", Toast.LENGTH_SHORT).show()
        }

        btnToggleService.setOnClickListener {
            if (Prefs.isServiceActive(this)) {
                stopGateway()
            } else {
                startGateway()
            }
        }

        findViewById<Button>(R.id.btnTestWebhook).setOnClickListener {
            saveInputs()
            if (Prefs.getWebhookUrl(this).isBlank()) {
                Toast.makeText(this, "Enter Webhook URL first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Sending test ping...", Toast.LENGTH_SHORT).show()
            WebhookSender.sendTestPing(this) { ok, msg ->
                runOnUiThread {
                    refreshUi()
                    Toast.makeText(this, if (ok) "Connected ($msg)" else "Failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun saveInputs() {
        Prefs.setDeviceName(this, etDeviceName.text.toString().ifEmpty { "Arpon-Pay-Phone" })
        Prefs.setWebhookUrl(this, etWebhookUrl.text.toString().trim())
        if (Prefs.getDeviceKey(this).isBlank()) {
            Prefs.generateDeviceKey(this, Prefs.getDeviceName(this))
        }
    }

    private fun startGateway() {
        saveInputs()
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions()
            return
        }
        Prefs.setServiceActive(this, true)
        val svc = Intent(this, GatewayForegroundService::class.java)
        ContextCompat.startForegroundService(this, svc)
        Prefs.appendLog(this, "Gateway STARTED")
        refreshUi()
    }

    private fun stopGateway() {
        Prefs.setServiceActive(this, false)
        stopService(Intent(this, GatewayForegroundService::class.java))
        Prefs.appendLog(this, "Gateway STOPPED")
        refreshUi()
    }

    private fun hasRequiredPermissions(): Boolean {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestRequiredPermissions() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 2026)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2026 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startGateway()
        } else {
            Toast.makeText(this, "SMS permission is required for auto-payment reading!", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshUi() {
        etDeviceName.setText(Prefs.getDeviceName(this))
        val key = Prefs.getDeviceKey(this)
        tvDeviceKey.text = key.ifBlank { "Tap 'Generate Key' below" }
        etWebhookUrl.setText(Prefs.getWebhookUrl(this))

        val active = Prefs.isServiceActive(this)
        if (active) {
            tvStatusBadge.text = "Status: RUNNING (Auto-SMS Active)"
            tvStatusBadge.setTextColor(0xFF4ADE80.toInt())
            btnToggleService.text = "Stop Auto-Gateway"
            btnToggleService.setBackgroundColor(0xFFDC2626.toInt())
        } else {
            tvStatusBadge.text = "Status: STOPPED"
            tvStatusBadge.setTextColor(0xFFF87171.toInt())
            btnToggleService.text = "Start Auto-Gateway"
            btnToggleService.setBackgroundColor(0xFF16A34A.toInt())
        }

        tvLogHeader.text = "3. Live Forwarded SMS Log (${Prefs.getCount(this)})"
        val logs = Prefs.getLogs(this)
        tvLogs.text = logs.ifBlank { "Waiting for incoming bKash / Nagad / Rocket SMS..." }
    }
}
