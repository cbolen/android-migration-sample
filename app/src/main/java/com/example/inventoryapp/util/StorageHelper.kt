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
     * Issue: uses Environment.getExternalStorageDirectory() — this path is not writable
     * on API 30+ without WRITE_EXTERNAL_STORAGE (denied on API 29+ targets).
     * Fix: replace with context.getExternalFilesDir("exports") or MediaStore.Downloads.
     */
    fun getExportDirectory(context: Context): File {
        @Suppress("DEPRECATION")
        val rootDir = Environment.getExternalStorageDirectory()
        val exportDir = File(rootDir, "$APP_FOLDER/exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    /**
     * Returns the directory where item photos are stored.
     * Issue: hardcoded /sdcard/ path — not portable, not accessible under scoped storage.
     * Fix: replace with context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).
     */
    fun getPhotosDirectory(context: Context): File {
        val photosDir = File("/sdcard/$APP_FOLDER/photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        return photosDir
    }

    /**
     * Returns the directory for temporary working files (e.g., import staging).
     * Issue: uses Environment.getExternalStorageDirectory() — same problem as getExportDirectory.
     * Fix: replace with context.getExternalFilesDir() or context.cacheDir.
     */
    fun getTempDirectory(context: Context): File {
        @Suppress("DEPRECATION")
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
        return File(getExportDirectory(context), fileName)
    }

    fun getPhotoFile(context: Context, barcode: String): File {
        val photosDir = getPhotosDirectory(context)
        return File(photosDir, generatePhotoFileName(barcode))
    }

    fun isExternalStorageWritable(): Boolean {
        @Suppress("DEPRECATION")
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun isExternalStorageReadable(): Boolean {
        @Suppress("DEPRECATION")
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
}
