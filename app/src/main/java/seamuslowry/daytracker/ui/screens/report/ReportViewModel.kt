package seamuslowry.daytracker.ui.screens.report

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import seamuslowry.daytracker.R
import seamuslowry.daytracker.data.repos.ItemRepo
import seamuslowry.daytracker.data.repos.SettingsRepo
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemConfiguration
import seamuslowry.daytracker.ui.shared.DateDisplay
import seamuslowry.daytracker.ui.shared.mapToCalendarStructure
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    itemRepo: ItemRepo,
    settingsRepo: SettingsRepo,
) : ViewModel() {
    var state = MutableStateFlow(ReportState())
        private set

    private val showRecordedValues: StateFlow<Boolean> = settingsRepo.settings.map {
        it.showRecordedValues
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val earliestDate: StateFlow<LocalDate> = itemRepo.getEarliest()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = LocalDate.now(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items: Flow<Map<ItemConfiguration, List<Item>>> = state
        .flatMapLatest {
            itemRepo.getFull(it.dateRange.start, it.dateRange.endInclusive)
        }
        .map {
            it.groupBy(
                keySelector = { itemWithConfiguration -> itemWithConfiguration.configuration },
                valueTransform = { itemWithConfiguration -> itemWithConfiguration.item },
            )
        }

    val displayItems: StateFlow<Map<ItemConfiguration, List<List<DateDisplay>>>> = combine(state, items, showRecordedValues) {
            s, i, srv ->
        mapToCalendarStructure(s.dateRange, i, srv)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = emptyMap(),
    )

    fun select(option: DisplayOption) {
        state.value = state.value.copy(selectedOption = option)
    }

    fun increment() {
        state.value = state.value.copy(anchorDate = state.value.anchorDate.plus(1, state.value.selectedOption.unit))
    }

    fun decrement() {
        state.value = state.value.copy(anchorDate = state.value.anchorDate.minus(1, state.value.selectedOption.unit))
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

enum class DisplayOption(@StringRes val label: Int, val field: (locale: Locale) -> TemporalField, val unit: ChronoUnit) {
    MONTH(R.string.display_month, { ChronoField.DAY_OF_MONTH }, ChronoUnit.MONTHS),
    WEEK(R.string.display_week, { WeekFields.of(it).dayOfWeek() }, ChronoUnit.WEEKS),
}

data class ReportState(
    val selectedOption: DisplayOption = DisplayOption.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
) {
    val dateRange: ClosedRange<LocalDate>
        get() {
            val temporalField = selectedOption.field(Locale.getDefault())
            val range = anchorDate.range(temporalField)
            return anchorDate.with(temporalField, range.minimum)..anchorDate.with(temporalField, range.maximum)
        }
}
