package seamuslowry.daytracker.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import seamuslowry.daytracker.MainActivity
import seamuslowry.daytracker.R
import seamuslowry.daytracker.data.repos.ItemConfigurationRepo
import seamuslowry.daytracker.data.repos.ItemRepo
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "ReminderBroadcastReceiver"

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var itemRepo: ItemRepo

    @Inject lateinit var itemConfigurationRepo: ItemConfigurationRepo
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Entering reminder broadcast receiver")
        context ?: return

        Log.d(TAG, "Processing reminder broadcast receiver")

        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Entering reminder broadcast receiver coroutine scope")

            val date = LocalDate.now()
            val completedConfigurationIds = itemRepo.get(date).firstOrNull()?.filter { it.value != null }?.map { it.configuration }?.toSet() ?: setOf()
            val notifiableItemConfigurationIds = itemConfigurationRepo.getAll().firstOrNull()?.filter { it.trackingType.notify }?.map { it.id }?.toSet() ?: setOf()
            Log.d(
                TAG,
                "Determining reminder for $date with notifiable configuration items $notifiableItemConfigurationIds and completed configurations $completedConfigurationIds",
            )
            if (!completedConfigurationIds.containsAll(notifiableItemConfigurationIds)) {
                Log.d(TAG, "Sending reminder notification for $date")
                showNotification(context)
            }

            pending.finish()
        }
    }

    private fun showNotification(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Prepare the intent for notification click action
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        // Create the notification
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setVisibility(VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "entry_reminder"
        const val NOTIFICATION_ID = 1
        const val REQUEST_CODE = 0
    }
}
