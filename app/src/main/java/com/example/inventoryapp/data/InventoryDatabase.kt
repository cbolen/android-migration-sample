package com.example.inventoryapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.inventoryapp.model.InventoryItem

class InventoryDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "inventory.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_INVENTORY = "inventory"
        const val COL_ID = "_id"
        const val COL_BARCODE = "barcode"
        const val COL_NAME = "name"
        const val COL_QUANTITY = "quantity"
        const val COL_LOCATION = "location"
        const val COL_NOTES = "notes"
        const val COL_PHOTO_PATH = "photo_path"
        const val COL_LAST_UPDATED = "last_updated"

        @Volatile
        private var instance: InventoryDatabase? = null

        fun getInstance(context: Context): InventoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: InventoryDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_INVENTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BARCODE TEXT NOT NULL UNIQUE,
                $COL_NAME TEXT NOT NULL,
                $COL_QUANTITY INTEGER NOT NULL DEFAULT 0,
                $COL_LOCATION TEXT,
                $COL_NOTES TEXT,
                $COL_PHOTO_PATH TEXT,
                $COL_LAST_UPDATED TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INVENTORY")
        onCreate(db)
    }

    fun getAllItems(): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        val cursor = readableDatabase.query(
            TABLE_INVENTORY, null, null, null, null, null,
            "$COL_NAME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }
        return items
    }

    fun findByBarcode(barcode: String): InventoryItem? {
        val cursor = readableDatabase.query(
            TABLE_INVENTORY, null,
            "$COL_BARCODE = ?", arrayOf(barcode),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToItem(it) else null
        }
    }

    fun insertItem(item: InventoryItem): Long {
        val values = ContentValues().apply {
            put(COL_BARCODE, item.barcode)
            put(COL_NAME, item.name)
            put(COL_QUANTITY, item.quantity)
            put(COL_LOCATION, item.location)
            put(COL_NOTES, item.notes)
            put(COL_PHOTO_PATH, item.photoPath)
            put(COL_LAST_UPDATED, item.lastUpdated)
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_INVENTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateItem(item: InventoryItem): Int {
        val values = ContentValues().apply {
            put(COL_NAME, item.name)
            put(COL_QUANTITY, item.quantity)
            put(COL_LOCATION, item.location)
            put(COL_NOTES, item.notes)
            put(COL_PHOTO_PATH, item.photoPath)
            put(COL_LAST_UPDATED, item.lastUpdated)
        }
        return writableDatabase.update(
            TABLE_INVENTORY, values,
            "$COL_ID = ?", arrayOf(item.id.toString())
        )
    }

    fun deleteItem(itemId: Long): Int {
        return writableDatabase.delete(
            TABLE_INVENTORY,
            "$COL_ID = ?", arrayOf(itemId.toString())
        )
    }

    fun getLowStockItems(threshold: Int): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        val cursor = readableDatabase.query(
            TABLE_INVENTORY, null,
            "$COL_QUANTITY <= ?", arrayOf(threshold.toString()),
            null, null, "$COL_QUANTITY ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }
        return items
    }

    private fun cursorToItem(cursor: android.database.Cursor): InventoryItem {
        return InventoryItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_BARCODE)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)) ?: "",
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)) ?: "",
            photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)) ?: "",
            lastUpdated = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_UPDATED)) ?: ""
        )
    }
}