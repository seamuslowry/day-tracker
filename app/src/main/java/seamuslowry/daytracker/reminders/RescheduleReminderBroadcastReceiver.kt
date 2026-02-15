package seamuslowry.daytracker.reminders

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import seamuslowry.daytracker.data.repos.SettingsRepo
import javax.inject.Inject

private const val TAG = "RescheduleReminderBroadcastReceiver"

@AndroidEntryPoint
class RescheduleReminderBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsRepo: SettingsRepo

    @Inject
    lateinit var scheduler: Scheduler

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Entering reschedule reminder broadcast receiver")
        context ?: return

        Log.d(TAG, "Processing reschedule reminder broadcast receiver")

        val settings = runBlocking { settingsRepo.settings.firstOrNull() }

        // if reminders aren't enabled, exit early and return
        if (settings?.reminderEnabled != true) {
            Log.d(TAG, "Exiting early because reminders are not enabled")

            return
        }

        Log.d(TAG, "Scheduling reminder after ${intent?.action}")
        val reminderTime = settings.reminderTime
        scheduler.scheduleReminder(reminderTime)
    }
}
