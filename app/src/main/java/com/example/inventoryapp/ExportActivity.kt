package com.example.inventoryapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    companion object {
        private const val WRITE_STORAGE_PERMISSION_REQUEST = 4001
    }

    private lateinit var repository: InventoryRepository
    private lateinit var tvStatus: TextView
    private lateinit var tvLastExport: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        repository = InventoryRepository(this)
        tvStatus = findViewById(R.id.tv_export_status)
        tvLastExport = findViewById(R.id.tv_last_export)

        val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
        val lastExport = prefs.getString("last_export_path", "Never")
        tvLastExport.text = "Last export: $lastExport"

        findViewById<Button>(R.id.btn_export_csv).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    WRITE_STORAGE_PERMISSION_REQUEST
                )
            } else {
                exportInventory()
            }
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportInventory()
            } else {
                Toast.makeText(this, "Storage permission required to export", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportInventory() {
        tvStatus.text = "Exporting..."
        repository.getAllItems { items ->
            runOnUiThread {
                performExport(items)
            }
        }
    }

    private fun performExport(items: List<InventoryItem>) {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(Environment.getExternalStorageDirectory(), "InventoryApp/exports")
        exportDir.mkdirs()

        val exportFile = File(exportDir, "inventory_${date}.csv")

        try {
            FileWriter(exportFile).use { writer ->
                writer.append("ID,Barcode,Name,Quantity,Location,Notes,Last Updated\n")
                for (item in items) {
                    writer.append("${item.id},")
                    writer.append("\"${item.barcode}\",")
                    writer.append("\"${item.name}\",")
                    writer.append("${item.quantity},")
                    writer.append("\"${item.location}\",")
                    writer.append("\"${item.notes}\",")
                    writer.append("\"${item.lastUpdated}\"\n")
                }
            }

            val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
            prefs.edit().putString("last_export_path", exportFile.absolutePath).apply()
            tvLastExport.text = "Last export: ${exportFile.absolutePath}"
            tvStatus.text = "Export complete: ${exportFile.absolutePath}\n(${items.size} items)"
            Toast.makeText(this, "Exported ${items.size} items", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            tvStatus.text = "Export failed: ${e.message}"
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
}