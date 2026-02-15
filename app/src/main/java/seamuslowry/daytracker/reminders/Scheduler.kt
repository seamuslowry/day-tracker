package seamuslowry.daytracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

private const val TAG = "Scheduler"

class Scheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun scheduleReminder(startTime: LocalTime) {
        // attempt to schedule the new reminders over the old ones
        val (alarmManager, reminderPendingIntent, reschedulePendingIntent) = alarmPieces(PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val now = ZonedDateTime.now()

        val todayStart = now.with(startTime)
        val tomorrowStart = todayStart.plusDays(1)

        val start = listOf(todayStart, tomorrowStart).firstOrNull { it.isAfter(now) } ?: todayStart

        val epochMilli = start.toInstant().toEpochMilli()

        Log.d(TAG, "Setting reminder to run once per day starting $start. Epoch Milli: $epochMilli")

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, epochMilli, AlarmManager.INTERVAL_DAY, reminderPendingIntent!!)

        Log.d(TAG, "Attempting to set next transition reschedule.")
        val nextTransition = start.zone.rules.nextTransition(start.toInstant())
        Log.d(TAG, "Next transition in ${start.zone} is $nextTransition.")

        if (nextTransition != null) {
            val rescheduleDate = nextTransition.dateTimeAfter.atZone(nextTransition.offsetAfter)
            val rescheduleMillis = rescheduleDate.toInstant().toEpochMilli()
            Log.d(TAG, "Setting transition reschedule to run once $rescheduleDate. Epoch Milli: $rescheduleMillis")

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, rescheduleMillis, reschedulePendingIntent!!)
        } else {
            Log.d(TAG, "Cancelling transition reschedule since timezone has no transitions")

            alarmManager.cancel(reschedulePendingIntent!!)
        }
    }

    fun tryCancelReminder() {
        val (alarmManager, reminderPendingIntent, reschedulePendingIntent) = alarmPieces(PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (reminderPendingIntent != null) {
            Log.d(TAG, "Cancelling reminder")
            alarmManager.cancel(reminderPendingIntent)
        }
        if (reschedulePendingIntent != null) {
            Log.d(TAG, "Cancelling transition reschedule")
            alarmManager.cancel(reschedulePendingIntent)
        }
    }

    private fun alarmPieces(flags: Int): Triple<AlarmManager, PendingIntent?, PendingIntent?> = Triple(
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
        PendingIntent.getBroadcast(context, 0, Intent(context, ReminderBroadcastReceiver::class.java).apply { action = REMINDER_ACTION }, flags),
        PendingIntent.getBroadcast(context, 0, Intent(context, RescheduleReminderBroadcastReceiver::class.java).apply { action = ZONE_TRANSITION_RESCHEDULE_ACTION }, flags),
    )

    companion object {
        const val REMINDER_ACTION = "seamuslowry.daytracker.REMINDER_ACTION"
        const val ZONE_TRANSITION_RESCHEDULE_ACTION = "seamuslowry.daytracker.ZONE_TRANSITION_RESCHEDULE_ACTION"
    }
}
