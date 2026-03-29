package com.example.inventoryapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.inventoryapp.data.InventoryRepository
import java.util.Calendar

/**
 * Schedules a daily exact alarm to trigger a low-stock check at 08:00.
 *
 * Issue: calls AlarmManager.setExactAndAllowWhileIdle() without first checking
 * canScheduleExactAlarms(). On API 31+, apps must hold the SCHEDULE_EXACT_ALARM permission
 * (or USE_EXACT_ALARM for alarm/calendar apps) and verify it is granted before scheduling
 * exact alarms. Without the permission, setExactAndAllowWhileIdle() throws a SecurityException
 * at runtime.
 *
 * Fix:
 *   val alarmManager = context.getSystemService(AlarmManager::class.java)
 *   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
 *       context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
 *       return
 *   }
 *   alarmManager.setExactAndAllowWhileIdle(...)
 *
 * Also: SCHEDULE_EXACT_ALARM permission is missing from AndroidManifest.xml.
 */
object StockAlarmScheduler {

    private const val REQUEST_CODE = 4001

    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, StockCheckReceiver::class.java)
        // Issue: PendingIntent created with flags = 0 — must include FLAG_IMMUTABLE on API 31+.
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0)

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        // Issue: setExactAndAllowWhileIdle called without canScheduleExactAlarms() check —
        // throws SecurityException on API 31+ if SCHEDULE_EXACT_ALARM is not granted.
        // Also: SCHEDULE_EXACT_ALARM is missing from AndroidManifest.xml.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StockCheckReceiver::class.java)
        @Suppress("UnspecifiedImmutableFlag")
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0)
        alarmManager.cancel(pendingIntent)
    }
}

/**
 * Receives the daily alarm and triggers a low-stock check.
 */
class StockCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        InventoryRepository(context).checkLowStock()
        // Reschedule for the following day
        StockAlarmScheduler.scheduleDailyCheck(context)
    }
}
