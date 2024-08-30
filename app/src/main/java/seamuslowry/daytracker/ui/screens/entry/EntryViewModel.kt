package seamuslowry.daytracker.ui.screens.entry

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import seamuslowry.daytracker.data.repos.ItemConfigurationRepo
import seamuslowry.daytracker.data.repos.ItemRepo
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemConfiguration
import seamuslowry.daytracker.models.ItemWithConfiguration
import java.time.LocalDate
import javax.inject.Inject

const val TAG = "EntryViewModel"

@HiltViewModel
class EntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemConfigurationRepo: ItemConfigurationRepo,
    private val itemRepo: ItemRepo,
) : ViewModel() {
    var state by mutableStateOf(ConfigurationState())
        private set

    var date = MutableStateFlow(
        savedStateHandle.get<Long?>("initialDate")?.let {
            LocalDate.ofEpochDay(it)
        } ?: LocalDate.now(),
    )

    private val configurations: StateFlow<List<ItemConfiguration>> = itemConfigurationRepo.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val items: StateFlow<List<ItemWithConfiguration>> = date
        .flatMapLatest {
            viewModelScope.launch { ensureItems(date.value) }
            itemRepo.getFull(it)
        }
        // keep track of the previous value to delay if it would be too jumpy
        .runningFold(
            Pair(emptyList<ItemWithConfiguration>(), emptyList<ItemWithConfiguration>()),
        ) { lastValue, newValue -> Pair(lastValue.second, newValue) }
        .debounce { if (it.second.size > it.first.size) 300 else 0 }
        .map {
            it.second.sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val itemsLoading = combine(items, configurations) { items, configs ->
        configs.size - items.size
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = 0,
        )

    private suspend fun ensureItems(date: LocalDate) {
        Log.d(TAG, "Ensuring items created on $date")
        val items = itemRepo.get(date).firstOrNull().orEmpty()

        val configurations = itemConfigurationRepo.getAll().firstOrNull()
            ?.filter { config -> items.firstOrNull { item -> item.configuration == config.id } == null }
            .orEmpty()

        val missingItems = configurations.map { Item(date = date, configuration = it.id) }

        itemRepo.save(*missingItems.toTypedArray())
    }

    fun changeDate(input: LocalDate) {
        date.value = input
    }

    fun deleteConfiguration(configuration: ItemConfiguration) {
        runBlocking { itemConfigurationRepo.delete(configuration) }
    }

    fun updateUnsaved(itemConfiguration: ItemConfiguration?) {
        state = state.copy(unsavedConfiguration = itemConfiguration)
    }

    suspend fun saveNewConfiguration() {
        state.unsavedConfiguration?.let {
            val newConfigurationId = itemConfigurationRepo.save(it)
            itemRepo.save(Item(date = date.value, configuration = newConfigurationId))
            state = state.copy(unsavedConfiguration = null)
        }
    }

    fun saveItem(item: Item) {
        runBlocking { itemRepo.save(item) }
    }

    fun saveItemConfiguration(itemConfiguration: ItemConfiguration) {
        runBlocking { itemConfigurationRepo.save(itemConfiguration) }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ConfigurationState(
    val unsavedConfiguration: ItemConfiguration? = null,
)
