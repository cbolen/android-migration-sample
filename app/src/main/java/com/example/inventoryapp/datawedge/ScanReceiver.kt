package com.example.inventoryapp.datawedge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives scan results broadcast by DataWedge.
 * The DataWedge profile for this app is configured to broadcast to
 * com.example.inventoryapp.SCAN_RESULT with the standard DataWedge extras.
 */
class ScanReceiver(
    private val onScanResult: (barcode: String, labelType: String) -> Unit
) : BroadcastReceiver() {

    companion object {
        // DataWedge standard intent extras
        const val DW_EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
        const val DW_EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type"
        const val DW_EXTRA_SOURCE = "com.symbol.datawedge.source"
        const val DW_EXTRA_DECODE_DATA = "com.symbol.datawedge.decode_data"

        // The intent action configured in the DataWedge profile output plugin
        const val ACTION_SCAN_RESULT = "com.example.inventoryapp.SCAN_RESULT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCAN_RESULT) return

        val barcode = intent.getStringExtra(DW_EXTRA_DATA_STRING)
        val labelType = intent.getStringExtra(DW_EXTRA_LABEL_TYPE) ?: "UNKNOWN"

        if (barcode.isNullOrBlank()) return

        val source = intent.getStringExtra(DW_EXTRA_SOURCE) ?: "scanner"

        // Only handle data from the physical scanner or imager, not from camera
        if (source == "msr") return

        onScanResult(barcode.trim(), labelType)
    }
}