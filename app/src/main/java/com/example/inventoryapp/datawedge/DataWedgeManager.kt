package com.example.inventoryapp.datawedge

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Manages DataWedge configuration and scanning control via the DataWedge Intent API.
 * All scanner interaction goes through DataWedge — no direct EMDK scanner access.
 */
class DataWedgeManager(private val context: Context) {

    companion object {
        private const val DW_ACTION = "com.symbol.datawedge.api.ACTION"
        private const val DW_EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
        private const val DW_EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        private const val DW_EXTRA_SWITCH_PROFILE = "com.symbol.datawedge.api.SWITCH_TO_PROFILE"
        private const val DW_EXTRA_SOFT_SCAN = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"
        private const val DW_EXTRA_SCANNER_INPUT_PLUGIN = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN"

        private const val PROFILE_NAME = "InventoryApp"
        private const val PACKAGE_NAME = "com.example.inventoryapp"
        private const val SCAN_ACTION = "com.example.inventoryapp.SCAN_RESULT"
    }

    fun createInventoryProfile() {
        // Step 1: create the profile
        sendDataWedgeIntent(DW_EXTRA_CREATE_PROFILE, PROFILE_NAME)

        // Step 2: configure it with SET_CONFIG
        val profileConfig = Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")

            // Associate the app with this profile
            val appConfig = Bundle().apply {
                putString("PACKAGE_NAME", PACKAGE_NAME)
                putStringArray("ACTIVITY_LIST", arrayOf("*"))
            }
            putParcelableArray("APP_LIST", arrayOf(appConfig))

            // Configure barcode input plugin
            val barcodePlugin = Bundle().apply {
                putString("PLUGIN_NAME", "BARCODE")
                putString("RESET_CONFIG", "true")
                val barcodeParams = Bundle().apply {
                    putString("scanner_selection", "auto")
                    putString("scanner_input_enabled", "true")
                    putString("decoder_code128", "true")
                    putString("decoder_code39", "true")
                    putString("decoder_upca", "true")
                    putString("decoder_upce0", "true")
                    putString("decoder_ean13", "true")
                    putString("decoder_ean8", "true")
                    putString("decoder_qrcode", "true")
                    putString("decoder_datamatrix", "true")
                }
                putBundle("PARAM_LIST", barcodeParams)
            }

            // Configure intent output plugin
            val intentPlugin = Bundle().apply {
                putString("PLUGIN_NAME", "INTENT")
                putString("RESET_CONFIG", "true")
                val intentParams = Bundle().apply {
                    putString("intent_output_enabled", "true")
                    putString("intent_action", SCAN_ACTION)
                    putString("intent_delivery", "2") // broadcast
                }
                putBundle("PARAM_LIST", intentParams)
            }

            // Configure keystroke output — disable it so we only get intents
            val keystrokePlugin = Bundle().apply {
                putString("PLUGIN_NAME", "KEYSTROKE")
                putString("RESET_CONFIG", "true")
                val ksParams = Bundle().apply {
                    putString("keystroke_output_enabled", "false")
                }
                putBundle("PARAM_LIST", ksParams)
            }

            putParcelableArray(
                "PLUGIN_CONFIG",
                arrayOf(barcodePlugin, intentPlugin, keystrokePlugin)
            )
        }

        val configIntent = Intent().apply {
            action = DW_ACTION
            putExtra(DW_EXTRA_SET_CONFIG, profileConfig)
        }
        context.sendBroadcast(configIntent)
    }

    fun switchToProfile(profileName: String) {
        sendDataWedgeIntent(DW_EXTRA_SWITCH_PROFILE, profileName)
    }

    fun enableScanning() {
        sendDataWedgeIntent(DW_EXTRA_SCANNER_INPUT_PLUGIN, "ENABLE_PLUGIN")
    }

    fun disableScanning() {
        sendDataWedgeIntent(DW_EXTRA_SCANNER_INPUT_PLUGIN, "DISABLE_PLUGIN")
    }

    fun triggerSoftScan(start: Boolean) {
        sendDataWedgeIntent(DW_EXTRA_SOFT_SCAN, if (start) "START_SCANNING" else "STOP_SCANNING")
    }

    private fun sendDataWedgeIntent(extraKey: String, extraValue: String) {
        val intent = Intent().apply {
            action = DW_ACTION
            putExtra(extraKey, extraValue)
        }
        context.sendBroadcast(intent)
    }
}