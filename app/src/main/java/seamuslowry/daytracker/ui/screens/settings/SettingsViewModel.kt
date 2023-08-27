package seamuslowry.daytracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import seamuslowry.daytracker.data.repos.Settings
import seamuslowry.daytracker.data.repos.SettingsRepo
import seamuslowry.daytracker.workers.cancelReminderWorker
import seamuslowry.daytracker.workers.scheduleReminderWorker
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepo,
    private val workManager: WorkManager,
) : ViewModel() {
    val state: StateFlow<Settings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        initialValue = Settings(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    suspend fun setReminderEnabled(value: Boolean) {
        if (value) scheduleReminder(state.value.reminderTime) else cancelReminder()
        settingsRepo.setReminderEnabled(value)
    }

    suspend fun setReminderTime(value: LocalTime) {
        scheduleReminder(value)
        settingsRepo.setReminderTime(value)
    }

    suspend fun setShowRecordedValues(value: Boolean) {
        settingsRepo.setShowRecordedValues(value)
    }

    suspend fun setMinCalendarSize(value: Float) {
        settingsRepo.setMinCalendarSize(value)
    }

    private fun scheduleReminder(time: LocalTime) {
        workManager.scheduleReminderWorker(time)
    }

    private fun cancelReminder() {
        workManager.cancelReminderWorker()
    }
}
