package com.example.inventoryapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventoryapp.datawedge.DataWedgeManager
import com.example.inventoryapp.util.StockAlarmScheduler

class SettingsActivity : AppCompatActivity() {

    private lateinit var etProfileName: EditText
    private lateinit var etLowStockThreshold: EditText
    private lateinit var etDefaultLocation: EditText
    private lateinit var switchVibrate: Switch
    private lateinit var switchNotifications: Switch
    private lateinit var switchAutoExport: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_settings)
        applyWindowInsets()

        etProfileName = findViewById(R.id.et_dw_profile_name)
        etLowStockThreshold = findViewById(R.id.et_low_stock_threshold)
        etDefaultLocation = findViewById(R.id.et_default_location)
        switchVibrate = findViewById(R.id.switch_vibrate)
        switchNotifications = findViewById(R.id.switch_notifications)
        switchAutoExport = findViewById(R.id.switch_auto_export)

        loadSettings()

        findViewById<Button>(R.id.btn_save_settings).setOnClickListener {
            saveSettings()
        }

        findViewById<Button>(R.id.btn_reconfigure_dw).setOnClickListener {
            val dwManager = DataWedgeManager(this)
            dwManager.createInventoryProfile()
            Toast.makeText(this, "DataWedge profile reconfigured", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_clear_data).setOnClickListener {
            val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
            prefs.edit()
                .remove("last_export_path")
                .remove("last_sync_time")
                .apply()
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)

        etProfileName.setText(prefs.getString("dw_profile_name", "InventoryApp"))
        etLowStockThreshold.setText(prefs.getInt("low_stock_threshold", 5).toString())
        etDefaultLocation.setText(prefs.getString("default_location", "Warehouse A"))
        switchVibrate.isChecked = prefs.getBoolean("vibrate_on_scan", true)
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        switchAutoExport.isChecked = prefs.getBoolean("auto_export_enabled", false)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
        val threshold = etLowStockThreshold.text.toString().toIntOrNull() ?: 5

        prefs.edit()
            .putString("dw_profile_name", etProfileName.text.toString().trim())
            .putInt("low_stock_threshold", threshold)
            .putString("default_location", etDefaultLocation.text.toString().trim())
            .putBoolean("vibrate_on_scan", switchVibrate.isChecked)
            .putBoolean("notifications_enabled", switchNotifications.isChecked)
            .putBoolean("auto_export_enabled", switchAutoExport.isChecked)
            .apply()

        // Schedule or cancel the daily low-stock alarm based on the notifications toggle
        if (prefs.getBoolean("notifications_enabled", true)) {
            StockAlarmScheduler.scheduleDailyCheck(this)
        } else {
            StockAlarmScheduler.cancel(this)
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun applyWindowInsets() {
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }
}