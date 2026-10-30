package com.israfilhossen.dawahchinese

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** Daily study reminder: an inexact daily alarm that posts one notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            c, 1, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(c.getString(R.string.reminder_title))
            .setContentText(c.getString(R.string.reminder_text))
            .setStyle(Notification.BigTextStyle().bigText(c.getString(R.string.reminder_text)))
            .setColor(0xFF0C3A2D.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(1, n) }
    }

    companion object {
        private const val CHANNEL = "daily"

        private fun pending(c: Context) = PendingIntent.getBroadcast(
            c, 0, Intent(c, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        fun schedule(c: Context) {
            Store.init(c)
            val am = c.getSystemService(AlarmManager::class.java)
            am.cancel(pending(c))
            if (!Store.reminderOn) return
            val t = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, Store.reminderHour); set(Calendar.MINUTE, Store.reminderMinute); set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.timeInMillis, AlarmManager.INTERVAL_DAY, pending(c))
        }
    }
}

/** Alarms are cleared on reboot: set the daily reminder again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderReceiver.schedule(c)
    }
}
