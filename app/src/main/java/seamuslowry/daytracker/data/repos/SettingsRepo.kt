package seamuslowry.daytracker.data.repos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

private const val SETTINGS_STORE = "settingsStore"

class SettingsRepo @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_STORE)
        val REMINDER_ENABLED = booleanPreferencesKey("REMINDER_ENABLED_KEY")
        val REMINDER_TIME = stringPreferencesKey("REMINDER_TIME")
        val SHOW_RECORDED_VALUES = booleanPreferencesKey("SHOW_RECORDED_VALUES")
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(time: LocalTime) {
        context.dataStore.edit {
            it[REMINDER_TIME] = time.toString()
        }
    }

    suspend fun setShowRecordedValues(show: Boolean) {
        context.dataStore.edit {
            it[SHOW_RECORDED_VALUES] = show
        }
    }

    val settings: Flow<Settings> = context.dataStore.data
        .map {
            Settings(
                reminderEnabled = it[REMINDER_ENABLED] == true,
                reminderTime = it[REMINDER_TIME]?.let { time -> LocalTime.parse(time) } ?: LocalTime.of(18, 0),
                showRecordedValues = it[SHOW_RECORDED_VALUES] == true,
            )
        }
}

data class Settings(
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(18, 0),
    val showRecordedValues: Boolean = false,
)
