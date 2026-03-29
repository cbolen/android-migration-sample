package com.example.inventoryapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.inventoryapp.LowStockAlertReceiver
import com.example.inventoryapp.MainActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.model.InventoryItem

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "inventory_alerts"
        private const val CHANNEL_NAME = "Inventory Alerts"
        private const val CHANNEL_DESCRIPTION = "Alerts for low stock and inventory events"
        const val NOTIFICATION_ID_LOW_STOCK = 1001
        private const val NOTIFICATION_ID_EXPORT_DONE = 1002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showLowStockNotification(items: List<InventoryItem>) {
        val title = "Low Stock Alert"
        val body = if (items.size == 1) {
            "${items[0].name} is running low (qty: ${items[0].quantity})"
        } else {
            "${items.size} items are running low"
        }

        // Issue: notification trampoline — PendingIntent targets a BroadcastReceiver
        // (LowStockAlertReceiver) which then calls startActivity(). This pattern is blocked
        // on API 31+ for apps targeting API 31+. The activity must be launched directly from
        // a PendingIntent attached to the notification, not via an intermediary receiver.
        val trampolineIntent = Intent(context, LowStockAlertReceiver::class.java).apply {
            action = "com.example.inventoryapp.action.LOW_STOCK_ALERT"
        }
        // Issue: PendingIntent created with flags = 0 — must include FLAG_IMMUTABLE or
        // FLAG_MUTABLE on API 31+, otherwise throws IllegalArgumentException at runtime.
        val pendingIntent = PendingIntent.getBroadcast(context, 0, trampolineIntent, 0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Issue: no POST_NOTIFICATIONS runtime permission check before notify() — on API 33+
        // this call is silently ignored unless the user has granted POST_NOTIFICATIONS.
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_LOW_STOCK, notification)
    }

    fun showExportCompleteNotification(filePath: String, itemCount: Int) {
        val mainIntent = Intent(context, MainActivity::class.java)

        // Issue: PendingIntent created with flags = 0 — must include FLAG_IMMUTABLE on API 31+.
        val pendingIntent = PendingIntent.getActivity(context, 1, mainIntent, 0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Export Complete")
            .setContentText("$itemCount items exported to $filePath")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Issue: no POST_NOTIFICATIONS runtime permission check before notify().
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_EXPORT_DONE, notification)
    }
}
