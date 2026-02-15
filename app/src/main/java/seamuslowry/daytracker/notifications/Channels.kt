package seamuslowry.daytracker.notifications

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import seamuslowry.daytracker.R
import seamuslowry.daytracker.reminders.ReminderBroadcastReceiver

fun Application.createReminderNotificationChannel() {
    val name = getString(R.string.reminders_channel_name)
    val descriptionText = getString(R.string.reminder_channel_description)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(ReminderBroadcastReceiver.NOTIFICATION_CHANNEL, name, importance).apply {
        description = descriptionText
        enableVibration(true)
    }
    // Register the channel with the system
    val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}
