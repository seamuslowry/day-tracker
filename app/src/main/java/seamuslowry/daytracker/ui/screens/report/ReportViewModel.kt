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
import seamuslowry.daytracker.models.LimitedOptionTrackingType
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
        val dayOfWeekField = WeekFields.of(Locale.getDefault()).dayOfWeek()
        val range = s.dateRange.start.range(dayOfWeekField)
        val blanksFrom = s.dateRange.start.with(dayOfWeekField, range.minimum)
        val blanksTo = s.dateRange.endInclusive.with(dayOfWeekField, range.maximum)
        val baseDate = DateDisplay(showValue = srv, date = s.anchorDate)

        val startingBlanks = List(ChronoUnit.DAYS.between(blanksFrom, s.dateRange.start).toInt()) { baseDate.copy(date = s.dateRange.start.minusDays(it.toLong() + 1), inRange = false) }.reversed()
        val endingBlanks = List(ChronoUnit.DAYS.between(s.dateRange.endInclusive, blanksTo).toInt()) { baseDate.copy(date = s.dateRange.endInclusive.plusDays(it.toLong() + 1), inRange = false) }

        val sequence = generateSequence(s.dateRange.start) { it.plusDays(1) }.takeWhile { it <= s.dateRange.endInclusive }

        i
            .filterKeys { it.trackingType is LimitedOptionTrackingType }
            .mapValues { entry ->
                val sequenceDisplays = sequence.map { date ->
                    entry.value.firstOrNull { item -> item.date == date }?.let { item ->
                        val trackingType = entry.key.trackingType as LimitedOptionTrackingType
                        val selection = trackingType.options.firstOrNull { it.value == item.value }
                        baseDate.copy(value = item.value, text = selection?.shortText, maxValue = trackingType.options.size, date = date)
                    } ?: baseDate.copy(date = date)
                }.toList()

                (startingBlanks + sequenceDisplays + endingBlanks).chunked(range.maximum.toInt())
            }
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

data class DateDisplay(
    val value: Int? = null,
    @StringRes val text: Int? = null,
    val maxValue: Int? = null,
    val date: LocalDate,
    val inRange: Boolean = true,
    val showValue: Boolean = false,
)

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
