package com.example.inventoryapp.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageHelper {

    private const val APP_FOLDER = "InventoryApp"

    /**
     * Returns the directory used for CSV exports.
     * Placed under the public external storage so warehouse managers can access
     * the files via USB or a file manager app.
     */
    fun getExportDirectory(): File {
        val exportDir = File(Environment.getExternalStorageDirectory(), "$APP_FOLDER/exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    /**
     * Returns the directory where item photos are stored.
     */
    fun getPhotosDirectory(): File {
        val photosDir = File("/sdcard/$APP_FOLDER/photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        return photosDir
    }

    /**
     * Returns the directory for temporary working files (e.g., import staging).
     */
    fun getTempDirectory(): File {
        val tempDir = File(Environment.getExternalStorageDirectory(), "$APP_FOLDER/temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    fun generateExportFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "inventory_${timestamp}.csv"
    }

    fun generatePhotoFileName(barcode: String): String {
        return "${barcode}.jpg"
    }

    fun getExportFile(context: Context): File {
        val fileName = generateExportFileName()
        return File(getExportDirectory(), fileName)
    }

    fun getPhotoFile(barcode: String): File {
        val photosDir = getPhotosDirectory()
        return File(photosDir, generatePhotoFileName(barcode))
    }

    /**
     * Checks whether the external storage is mounted and writable.
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Checks whether the external storage is at least readable.
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
}