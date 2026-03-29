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
import com.example.inventoryapp.util.StorageHelper
import java.io.File
import java.io.FileWriter

class ExportActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_WRITE_STORAGE = 3001
    }

    private lateinit var repository: InventoryRepository
    private lateinit var tvStatus: TextView
    private lateinit var tvLastExport: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Issue: no edge-to-edge / WindowInsetsCompat handling.
        setContentView(R.layout.activity_export)

        repository = InventoryRepository(this)
        tvStatus = findViewById(R.id.tv_export_status)
        tvLastExport = findViewById(R.id.tv_last_export)

        val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
        val lastExport = prefs.getString("last_export_path", "Never")
        tvLastExport.text = "Last export: $lastExport"

        findViewById<Button>(R.id.btn_export_csv).setOnClickListener {
            exportInventory()
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun exportInventory() {
        // Issue: checking WRITE_EXTERNAL_STORAGE via onRequestPermissionsResult — this permission
        // is ignored on API 30+ and onRequestPermissionsResult is deprecated on API 33+.
        // Fix: remove the permission check (not needed with getExternalFilesDir); replace
        // onRequestPermissionsResult with registerForActivityResult(RequestPermission).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
            return
        }
        doExport()
    }

    // Issue: onRequestPermissionsResult is deprecated — replace with registerForActivityResult.
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            doExport()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doExport() {
        tvStatus.text = "Exporting..."
        repository.getAllItems { items ->
            try {
                val exportPath = writeExportFile(items)
                val prefs = getSharedPreferences("inventory_prefs", MODE_PRIVATE)
                prefs.edit().putString("last_export_path", exportPath).apply()
                tvLastExport.text = "Last export: $exportPath"
                tvStatus.text = "Export complete: $exportPath\n(${items.size} items)"
                Toast.makeText(this, "Exported ${items.size} items", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                tvStatus.text = "Export failed: ${e.message}"
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeExportFile(items: List<InventoryItem>): String {
        // Issue: Environment.getExternalStorageDirectory() returns public external storage —
        // not writable on API 30+ without WRITE_EXTERNAL_STORAGE (denied on API 29+ targets).
        // Fix: replace with getExternalFilesDir("exports") for private storage, or
        // MediaStore.Downloads if the file must be visible in the shared Downloads folder.
        @Suppress("DEPRECATION")
        val exportDir = File(Environment.getExternalStorageDirectory(), "InventoryApp/exports")
        exportDir.mkdirs()
        val exportFile = File(exportDir, StorageHelper.generateExportFileName())

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
        return exportFile.absolutePath
    }
}
