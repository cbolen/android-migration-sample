package com.example.inventoryapp

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
        const val ADD_ITEM_REQUEST = 1001
        const val EDIT_ITEM_REQUEST = 1002
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter
    private lateinit var repository: InventoryRepository
    private lateinit var dwManager: DataWedgeManager
    private lateinit var scanReceiver: ScanReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = InventoryRepository(this)
        dwManager = DataWedgeManager(this)

        recyclerView = findViewById(R.id.recycler_inventory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = InventoryAdapter(mutableListOf()) { item ->
            val intent = Intent(this, AddItemActivity::class.java).apply {
                putExtra("item_id", item.id)
                putExtra("item_barcode", item.barcode)
                putExtra("item_name", item.name)
                putExtra("item_quantity", item.quantity)
                putExtra("item_location", item.location)
            }
            startActivityForResult(intent, EDIT_ITEM_REQUEST)
        }
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivityForResult(intent, ADD_ITEM_REQUEST)
        }

        loadInventory()
        setupScanReceiver()
    }

    private fun loadInventory() {
        repository.getAllItems { items ->
            runOnUiThread {
                adapter.updateItems(items)
            }
        }
    }

    private fun setupScanReceiver() {
        scanReceiver = ScanReceiver { barcode, labelType ->
            handleScannedBarcode(barcode, labelType)
        }

        val filter = IntentFilter()
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION")
        filter.addAction("com.example.inventoryapp.SCAN_RESULT")
        filter.addCategory(Intent.CATEGORY_DEFAULT)

        registerReceiver(scanReceiver, filter)
        dwManager.enableScanning()
    }

    private fun handleScannedBarcode(barcode: String, labelType: String) {
        repository.findByBarcode(barcode) { item ->
            runOnUiThread {
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
                    startActivityForResult(intent, ADD_ITEM_REQUEST)
                }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ADD_ITEM_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val newItem = data?.getParcelableExtra<InventoryItem>("new_item")
                    if (newItem != null) {
                        repository.insertItem(newItem) { success ->
                            if (success) {
                                runOnUiThread {
                                    loadInventory()
                                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
            EDIT_ITEM_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val updatedItem = data?.getParcelableExtra<InventoryItem>("new_item")
                    if (updatedItem != null) {
                        repository.updateItem(updatedItem) { success ->
                            if (success) {
                                runOnUiThread {
                                    loadInventory()
                                    Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit the inventory app?")
            .setPositiveButton("Exit") { _, _ ->
                dwManager.disableScanning()
                super.onBackPressed()
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
}