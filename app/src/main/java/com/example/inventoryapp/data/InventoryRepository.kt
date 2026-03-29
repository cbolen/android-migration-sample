package com.example.inventoryapp.data

import android.content.Context
import android.os.AsyncTask
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.util.NotificationHelper

class InventoryRepository(private val context: Context) {

    private val database = InventoryDatabase.getInstance(context)
    private val notificationHelper = NotificationHelper(context)

    fun getAllItems(callback: (List<InventoryItem>) -> Unit) {
        FetchAllTask(callback).execute()
    }

    fun findByBarcode(barcode: String, callback: (InventoryItem?) -> Unit) {
        FindByBarcodeTask(barcode, callback).execute()
    }

    fun insertItem(item: InventoryItem, callback: (Boolean) -> Unit) {
        InsertItemTask(item, callback).execute()
    }

    fun updateItem(item: InventoryItem, callback: (Boolean) -> Unit) {
        UpdateItemTask(item, callback).execute()
    }

    fun checkLowStock() {
        CheckLowStockTask().execute()
    }

    // Issue: AsyncTask was deprecated in API 30 and removed in API 33.
    // Fix: replace with Kotlin coroutines (viewModelScope.launch / Dispatchers.IO).

    private inner class FetchAllTask(
        private val callback: (List<InventoryItem>) -> Unit
    ) : AsyncTask<Void, Void, List<InventoryItem>>() {
        override fun doInBackground(vararg p: Void) = database.getAllItems()
        override fun onPostExecute(result: List<InventoryItem>) = callback(result)
    }

    private inner class FindByBarcodeTask(
        private val barcode: String,
        private val callback: (InventoryItem?) -> Unit
    ) : AsyncTask<Void, Void, InventoryItem?>() {
        override fun doInBackground(vararg p: Void) = database.findByBarcode(barcode)
        override fun onPostExecute(result: InventoryItem?) = callback(result)
    }

    private inner class InsertItemTask(
        private val item: InventoryItem,
        private val callback: (Boolean) -> Unit
    ) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p: Void) = database.insertItem(item) > 0
        override fun onPostExecute(result: Boolean) = callback(result)
    }

    private inner class UpdateItemTask(
        private val item: InventoryItem,
        private val callback: (Boolean) -> Unit
    ) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p: Void) = database.updateItem(item) > 0
        override fun onPostExecute(result: Boolean) = callback(result)
    }

    private inner class CheckLowStockTask : AsyncTask<Void, Void, List<InventoryItem>>() {
        override fun doInBackground(vararg p: Void): List<InventoryItem> {
            val prefs = context.getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE)
            val threshold = prefs.getInt("low_stock_threshold", 5)
            return database.getLowStockItems(threshold)
        }

        override fun onPostExecute(result: List<InventoryItem>) {
            val prefs = context.getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
            if (notificationsEnabled && result.isNotEmpty()) {
                notificationHelper.showLowStockNotification(result)
            }
        }
    }
}
