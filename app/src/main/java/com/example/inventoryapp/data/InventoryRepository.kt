package com.example.inventoryapp.data

import android.content.Context
import android.os.AsyncTask
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.util.NotificationHelper

class InventoryRepository(private val context: Context) {

    private val database = InventoryDatabase.getInstance(context)
    private val notificationHelper = NotificationHelper(context)

    fun getAllItems(callback: (List<InventoryItem>) -> Unit) {
        FetchAllTask(database, callback).execute()
    }

    fun findByBarcode(barcode: String, callback: (InventoryItem?) -> Unit) {
        FindByBarcodeTask(database, barcode, callback).execute()
    }

    fun insertItem(item: InventoryItem, callback: (Boolean) -> Unit) {
        InsertItemTask(database, item, callback).execute()
    }

    fun updateItem(item: InventoryItem, callback: (Boolean) -> Unit) {
        UpdateItemTask(database, item, callback).execute()
    }

    fun checkLowStock() {
        val prefs = context.getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE)
        val threshold = prefs.getInt("low_stock_threshold", 5)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) return

        CheckLowStockTask(database, threshold, notificationHelper).execute()
    }

    private class FetchAllTask(
        private val db: InventoryDatabase,
        private val callback: (List<InventoryItem>) -> Unit
    ) : AsyncTask<Void, Void, List<InventoryItem>>() {

        override fun doInBackground(vararg params: Void?): List<InventoryItem> {
            return db.getAllItems()
        }

        override fun onPostExecute(result: List<InventoryItem>) {
            callback(result)
        }
    }

    private class FindByBarcodeTask(
        private val db: InventoryDatabase,
        private val barcode: String,
        private val callback: (InventoryItem?) -> Unit
    ) : AsyncTask<Void, Void, InventoryItem?>() {

        override fun doInBackground(vararg params: Void?): InventoryItem? {
            return db.findByBarcode(barcode)
        }

        override fun onPostExecute(result: InventoryItem?) {
            callback(result)
        }
    }

    private class InsertItemTask(
        private val db: InventoryDatabase,
        private val item: InventoryItem,
        private val callback: (Boolean) -> Unit
    ) : AsyncTask<InventoryItem, Void, Boolean>() {

        override fun doInBackground(vararg params: InventoryItem?): Boolean {
            val rowId = db.insertItem(item)
            return rowId > 0
        }

        override fun onPostExecute(result: Boolean) {
            callback(result)
        }
    }

    private class UpdateItemTask(
        private val db: InventoryDatabase,
        private val item: InventoryItem,
        private val callback: (Boolean) -> Unit
    ) : AsyncTask<InventoryItem, Void, Boolean>() {

        override fun doInBackground(vararg params: InventoryItem?): Boolean {
            val rowsAffected = db.updateItem(item)
            return rowsAffected > 0
        }

        override fun onPostExecute(result: Boolean) {
            callback(result)
        }
    }

    private class CheckLowStockTask(
        private val db: InventoryDatabase,
        private val threshold: Int,
        private val notificationHelper: NotificationHelper
    ) : AsyncTask<Void, Void, List<InventoryItem>>() {

        override fun doInBackground(vararg params: Void?): List<InventoryItem> {
            return db.getLowStockItems(threshold)
        }

        override fun onPostExecute(result: List<InventoryItem>) {
            if (result.isNotEmpty()) {
                notificationHelper.showLowStockNotification(result)
            }
        }
    }
}