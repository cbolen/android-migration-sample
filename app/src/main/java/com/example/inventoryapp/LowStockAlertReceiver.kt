package com.example.inventoryapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Issue: notification trampoline — this receiver is triggered by a notification tap and
 * immediately starts MainActivity. Starting an Activity from a BroadcastReceiver in response
 * to a notification content intent is blocked on API 31+ for apps targeting API 31+.
 *
 * Fix: remove this receiver entirely. Attach a PendingIntent that targets MainActivity
 * directly as the notification's content intent.
 */
class LowStockAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(launchIntent)
    }
}
