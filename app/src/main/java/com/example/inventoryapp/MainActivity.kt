package com.example.inventoryapp

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.datawedge.DataWedgeManager
import com.example.inventoryapp.datawedge.ScanReceiver
import com.example.inventoryapp.model.InventoryItem
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ADD_ITEM_REQUEST = 1001
        private const val EDIT_ITEM_REQUEST = 1002
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter
    private lateinit var repository: InventoryRepository
    private lateinit var dwManager: DataWedgeManager
    private lateinit var scanReceiver: ScanReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Issue: no edge-to-edge / WindowInsetsCompat handling — content will be clipped by
        // system bars on API 35+. Fix: call WindowCompat.setDecorFitsSystemWindows(window, false)
        // and apply ViewCompat.setOnApplyWindowInsetsListener on the root view.
        setContentView(R.layout.activity_main)

        repository = InventoryRepository(this)
        dwManager = DataWedgeManager(this)

        recyclerView = findViewById(R.id.recycler_inventory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = InventoryAdapter(mutableListOf()) { item ->
            // Issue: startActivityForResult is deprecated — replace with registerForActivityResult
            // using ActivityResultContracts.StartActivityForResult.
            val intent = Intent(this, AddItemActivity::class.java).apply {
                putExtra("item_id", item.id)
                putExtra("item_barcode", item.barcode)
                putExtra("item_name", item.name)
                putExtra("item_quantity", item.quantity)
                putExtra("item_location", item.location)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, EDIT_ITEM_REQUEST)
        }
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            // Issue: startActivityForResult is deprecated.
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, AddItemActivity::class.java), ADD_ITEM_REQUEST)
        }

        loadInventory()
        setupScanReceiver()
    }

    // Issue: onActivityResult is deprecated — remove this override and handle results in the
    // ActivityResultCallback registered with registerForActivityResult.
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val item = data?.getParcelableExtraCompat<InventoryItem>("new_item") ?: return
        when (requestCode) {
            ADD_ITEM_REQUEST -> repository.insertItem(item) { success ->
                if (success) {
                    loadInventory()
                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
                }
            }
            EDIT_ITEM_REQUEST -> repository.updateItem(item) { success ->
                if (success) {
                    loadInventory()
                    Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadInventory() {
        repository.getAllItems { items ->
            adapter.updateItems(items)
        }
    }

    private fun setupScanReceiver() {
        scanReceiver = ScanReceiver { barcode, labelType ->
            handleScannedBarcode(barcode, labelType)
        }

        val filter = IntentFilter()
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION")
        filter.addAction(ScanReceiver.ACTION_SCAN_RESULT)
        filter.addCategory(Intent.CATEGORY_DEFAULT)

        // Issue: registerReceiver called without RECEIVER_NOT_EXPORTED or RECEIVER_EXPORTED flag
        // — on API 34+, dynamically registered receivers that omit the export flag throw an
        // exception at runtime. Fix: use ContextCompat.registerReceiver with
        // ContextCompat.RECEIVER_NOT_EXPORTED (DataWedge broadcasts are from a system service
        // but targeted at this app — use NOT_EXPORTED).
        @Suppress("UnspecifiedRegisterReceiverFlag")
        registerReceiver(scanReceiver, filter)
        dwManager.enableScanning()
    }

    private fun handleScannedBarcode(barcode: String, labelType: String) {
        repository.findByBarcode(barcode) { item ->
            if (item != null) {
                Toast.makeText(
                    this,
                    "Found: ${item.name} — Qty: ${item.quantity} @ ${item.location}",
                    Toast.LENGTH_LONG
                ).show()
                highlightItem(item)
            } else {
                val intent = Intent(this, AddItemActivity::class.java).apply {
                    putExtra("item_barcode", barcode)
                    putExtra("label_type", labelType)
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, ADD_ITEM_REQUEST)
            }
        }
    }

    private fun highlightItem(item: InventoryItem) {
        val position = adapter.getPositionForId(item.id)
        if (position >= 0) {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.isSelected = true
            }
        }
    }

    // Issue: onBackPressed() is deprecated from API 33 and the override is removed in future
    // versions. Fix: replace with onBackPressedDispatcher.addCallback(this, OnBackPressedCallback).
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit the inventory app?")
            .setPositiveButton("Exit") { _, _ ->
                dwManager.disableScanning()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        dwManager.switchToProfile("InventoryApp")
    }

    override fun onPause() {
        super.onPause()
        dwManager.disableScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                startActivity(Intent(this, ExportActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
        @Suppress("DEPRECATION")
        return getParcelableExtra(key)
    }
}
