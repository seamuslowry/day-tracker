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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
import java.time.Instant
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items: StateFlow<List<ItemWithConfiguration>> = date
        .flatMapLatest {
            viewModelScope.launch { ensureItems(date.value) }
            itemRepo.getFull(it)
        }
        // keep track of the previous value to delay if it would be too jumpy
        .runningFold(
            Pair(emptyList<ItemWithConfiguration>(), emptyList<ItemWithConfiguration>()),
        ) { lastValue, newValue -> Pair(lastValue.second, newValue) }
        .map {
            // add a _slight_ delay on adds so it doesn't look jumpy
            delay(if (it.second.size > it.first.size) 300 else 0)
            state = state.copy(items = mergeItemWithConfigurations(it.second, state.items))
            state.items
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

    fun swap(from: ItemConfiguration, to: ItemConfiguration) {
        state = state.copy(
            items = mergeItemWithConfigurations(
                state.items,
                listOfNotNull(
                    state.items.firstOrNull { it.configuration.id == from.id }?.copy(
                        configuration = from.copy(
                            orderOverride = to.order,
                            lastModified = Instant.now(),
                        ),
                    ),
                    state.items.firstOrNull { it.configuration.id == to.id }?.copy(
                        configuration = to.copy(
                            orderOverride = from.order,
                            lastModified = Instant.now(),
                        ),
                    ),
                ),
            ),
        )

        runBlocking { itemConfigurationRepo.updateAll(from.copy(orderOverride = to.order), to.copy(orderOverride = from.order)) }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ConfigurationState(
    val items: List<ItemWithConfiguration> = emptyList(),
    val unsavedConfiguration: ItemConfiguration? = null,
)

private fun mergeItemWithConfigurations(
    savedItems: List<ItemWithConfiguration>,
    awaitingSaveItems: List<ItemWithConfiguration>,
) = (savedItems + awaitingSaveItems)
    .filter { el -> savedItems.firstOrNull { it.configuration.id == el.configuration.id } != null }
    .groupBy { it.configuration.id }
    .mapNotNull { (_, values) ->
        values.maxByOrNull { it.configuration.lastModified }
    }.sorted()
