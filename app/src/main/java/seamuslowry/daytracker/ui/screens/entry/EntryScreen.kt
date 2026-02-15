package seamuslowry.daytracker.ui.screens.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.fornewid.placeholder.foundation.PlaceholderHighlight
import io.github.fornewid.placeholder.material3.fade
import io.github.fornewid.placeholder.material3.placeholder
import kotlinx.coroutines.launch
import seamuslowry.daytracker.R
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemConfiguration
import seamuslowry.daytracker.models.ItemWithConfiguration
import seamuslowry.daytracker.models.LimitedOptionTrackingType
import seamuslowry.daytracker.models.TextEntryTrackingType
import seamuslowry.daytracker.models.localeFormat
import seamuslowry.daytracker.ui.shared.ArrowPicker
import seamuslowry.daytracker.ui.shared.TrackerEntry
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate

val SUPPORTED_TRACKING_TYPES =
    listOf(
        LimitedOptionTrackingType.ONE_TO_TEN,
        LimitedOptionTrackingType.YES_NO,
        TextEntryTrackingType,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryScreen(viewModel: EntryViewModel = hiltViewModel()) {
    val itemsLoading by viewModel.itemsLoading.collectAsState()
    val state = viewModel.state
    val date by viewModel.date.collectAsState()
    val scope = rememberCoroutineScope()
    val items = state.items

    val lazyColumnState = rememberLazyListState()
    val reorderableLazyColumnState =
        rememberReorderableLazyListState(
            lazyListState = lazyColumnState,
            scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
        ) { from, to ->
            val fromElement = items.find { it.item.id == from.key } ?: return@rememberReorderableLazyListState
            val toElement = items.find { it.item.id == to.key } ?: return@rememberReorderableLazyListState

            viewModel.swap(fromElement.configuration, toElement.configuration)
        }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        state = lazyColumnState,
    ) {
        item("date") {
            ArrowPicker(
                value = date.toEpochDay(),
                onChange = { viewModel.changeDate(LocalDate.ofEpochDay(it)) },
                range =
                    LongRange(
                        LocalDate.now().minusYears(1).toEpochDay(),
                        LocalDate.now().toEpochDay(),
                    ),
                incrementLabel = stringResource(R.string.change_date, date.plusDays(1).localeFormat()),
                decrementLabel = stringResource(R.string.change_date, date.minusDays(1).localeFormat()),
            ) {
                Text(text = LocalDate.ofEpochDay(it).localeFormat(), textAlign = TextAlign.Center)
            }
        }
        items(items = items, key = { it.item.id }) {
            val interactionSource = remember { MutableInteractionSource() }
            ReorderableItem(state = reorderableLazyColumnState, key = it.item.id) { _ ->
                ItemEntry(
                    itemWithConfiguration = it,
                    onChange = viewModel::saveItem,
                    onDelete = viewModel::deleteConfiguration,
                    onEdit = viewModel::saveItemConfiguration,
                    modifier = Modifier.longPressDraggableHandle(interactionSource = interactionSource),
                    interactionSource = interactionSource,
                )
            }
        }
        items(itemsLoading.coerceAtLeast(0)) {
            ItemEntry()
        }
        item("button") {
            AddConfigurationButton(
                itemConfiguration = state.unsavedConfiguration,
                onChange = viewModel::updateUnsaved,
                onSave = {
                    scope.launch {
                        viewModel.saveNewConfiguration()
                    }
                },
                onDiscard = { viewModel.updateUnsaved(null) },
            )
        }
    }
}

@Composable
fun ItemEntry(
    modifier: Modifier = Modifier,
    itemWithConfiguration: ItemWithConfiguration? = null,
    onChange: (item: Item) -> Unit = {},
    onDelete: (itemConfiguration: ItemConfiguration) -> Unit = {},
    onEdit: (itemConfiguration: ItemConfiguration) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val item = itemWithConfiguration?.item
    val configuration = itemWithConfiguration?.configuration ?: ItemConfiguration()

    var editingConfiguration: ItemConfiguration? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(key1 = configuration) {
        editingConfiguration = null
    }

    Card(
        onClick = {},
        interactionSource = interactionSource,
        modifier =
            modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .placeholder(
                    visible = itemWithConfiguration == null,
                    highlight = PlaceholderHighlight.fade(),
                    color = CardDefaults.cardColors().containerColor,
                ).animateContentSize(),
    ) {
        editingConfiguration?.let {
            UpsertConfigurationContent(
                itemConfiguration = it,
                onChange = { newConfiguration -> editingConfiguration = newConfiguration },
                onSave = { onEdit(it) },
                onDiscard = { editingConfiguration = null },
                disableSave = configuration == editingConfiguration,
            )
        } ?: run {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 25.dp, top = 10.dp),
            ) {
                Text(text = configuration.name.ifEmpty { stringResource(R.string.default_name) })
                ItemEntryMenu(onEvent = {
                    when (it) {
                        ItemEntryMenuAction.DELETE -> {
                            onDelete(configuration)
                        }

                        ItemEntryMenuAction.EDIT -> {
                            editingConfiguration = configuration
                        }
                    }
                })
            }
            TrackerEntry(
                trackerType = configuration.trackingType,
                item = item,
                onChange = onChange,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }
    }
}

enum class ItemEntryMenuAction {
    DELETE,
    EDIT,
}

@Composable
fun ItemEntryMenu(
    onEvent: (action: ItemEntryMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var deleteConfirmationNeeded by remember { mutableStateOf(false) }

    if (deleteConfirmationNeeded) {
        AlertDialog(
            onDismissRequest = {
                deleteConfirmationNeeded = false
            },
            title = {
                Text(text = stringResource(R.string.confirm_deletion))
            },
            text = {
                Text(text = stringResource(R.string.confirm_deletion_detail))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationNeeded = false
                        onEvent(ItemEntryMenuAction.DELETE)
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationNeeded = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Box {
        IconButton(onClick = {
            expanded = true
        }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.tracker_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    expanded = false
                    onEvent(ItemEntryMenuAction.EDIT)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    expanded = false
                    deleteConfirmationNeeded = true
                },
            )
        }
    }
}

@Composable
fun AddConfigurationButton(
    itemConfiguration: ItemConfiguration?,
    onChange: (itemConfiguration: ItemConfiguration) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainDuration = 500
    val extraDuration = 100
    val cardColor by animateColorAsState(
        targetValue = if (itemConfiguration != null) CardDefaults.cardColors().containerColor else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = mainDuration),
        label = "cardColor",
    )
    val textColor by animateColorAsState(
        targetValue = if (itemConfiguration != null) Color.Transparent else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(durationMillis = extraDuration, delayMillis = mainDuration),
        label = "textColor",
    )
    val corner by animateIntAsState(
        targetValue = if (itemConfiguration != null) 10 else 50,
        animationSpec = tween(durationMillis = mainDuration),
        label = "corner",
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            modifier
                .padding(20.dp)
                .fillMaxWidth(),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier.clip(RoundedCornerShape(corner)),
        ) {
            Column(
                modifier =
                    Modifier.animateContentSize(
                        animationSpec = tween(durationMillis = mainDuration),
                    ),
            ) {
                if (itemConfiguration == null) {
                    TextButton(
                        onClick = { onChange(ItemConfiguration()) },
                        colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                    ) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = stringResource(R.string.add_item_config),
                            modifier = Modifier.scale(0.75f),
                        )
                        Text(
                            text = stringResource(R.string.add_item_config),
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                } else {
                    UpsertConfigurationContent(
                        itemConfiguration = itemConfiguration,
                        onChange = onChange,
                        onSave = onSave,
                        onDiscard = onDiscard,
                    )
                }
            }
        }
    }
}

@Composable
fun UpsertConfigurationContent(
    itemConfiguration: ItemConfiguration,
    onChange: (itemConfiguration: ItemConfiguration) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    disableSave: Boolean = false,
) {
    val creating = itemConfiguration.id == 0L
    val currentTrackingTypeIndex = SUPPORTED_TRACKING_TYPES.indexOf(itemConfiguration.trackingType).toLong()

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
    ) {
        OutlinedTextField(
            value = itemConfiguration.name,
            onValueChange = { onChange(itemConfiguration.copy(name = it)) },
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 5.dp),
            label = { Text(text = stringResource(R.string.name)) },
        )
        IconButton(onClick = onDiscard) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.discard_configuration),
            )
        }
    }
    ArrowPicker(
        value = currentTrackingTypeIndex,
        onChange = {
            onChange(itemConfiguration.copy(trackingType = SUPPORTED_TRACKING_TYPES[it.toInt()]))
        },
        range =
            if (creating) {
                LongRange(
                    0,
                    (SUPPORTED_TRACKING_TYPES.size - 1).toLong(),
                )
            } else {
                LongRange(currentTrackingTypeIndex, currentTrackingTypeIndex)
            },
        modifier = Modifier.padding(5.dp),
        incrementLabel =
            SUPPORTED_TRACKING_TYPES.getOrNull(currentTrackingTypeIndex.toInt() + 1)?.let {
                stringResource(R.string.change_tracking_type, stringResource(it.label))
            }
                ?: stringResource(R.string.no_more_tracking_types),
        decrementLabel =
            SUPPORTED_TRACKING_TYPES.getOrNull(currentTrackingTypeIndex.toInt() - 1)?.let {
                stringResource(R.string.change_tracking_type, stringResource(it.label))
            }
                ?: stringResource(R.string.no_more_tracking_types),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TrackerEntry(trackerType = SUPPORTED_TRACKING_TYPES[it.toInt()], enabled = false)
        }
    }
    Button(
        enabled = !disableSave && itemConfiguration.name.isNotBlank(),
        onClick = onSave,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.confirm_configuration),
            modifier = Modifier.padding(0.dp),
        )
    }
}
