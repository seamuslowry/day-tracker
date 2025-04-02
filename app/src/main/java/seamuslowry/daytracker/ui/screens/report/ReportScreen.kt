package seamuslowry.daytracker.ui.screens.report

import android.animation.ArgbEvaluator
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import seamuslowry.daytracker.R
import seamuslowry.daytracker.models.ItemConfiguration
import seamuslowry.daytracker.models.localeFormat
import seamuslowry.daytracker.ui.shared.ArrowPicker
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportScreen(
    onSelectDate: (d: LocalDate) -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colorOverrides by viewModel.colorOverrides.collectAsState()
    val groupedItems by viewModel.displayItems.collectAsState()
    val earliestDate by viewModel.earliestDate.collectAsState()
    val selectedOptionSingularText = stringResource(state.selectedOption.singular)

    Column(modifier = Modifier.fillMaxWidth()) {
        ArrowPicker(
            value = state.dateRange,
            onIncrement = { viewModel.increment() },
            onDecrement = { viewModel.decrement() },
            compare = { a, b -> a.start.compareTo(b.start) },
            incrementEnabled = state.dateRange.endInclusive < LocalDate.now(),
            decrementEnabled = state.dateRange.start > earliestDate,
            incrementLabel = stringResource(R.string.see_next, selectedOptionSingularText),
            decrementLabel = stringResource(R.string.see_previous, selectedOptionSingularText),
        ) {
            Text(
                text = stringResource(
                    id = R.string.date_range,
                    it.start.localeFormat(),
                    it.endInclusive.localeFormat(),
                ),
                textAlign = TextAlign.Center,
            )
        }
        DisplaySelection(
            selected = state.selectedOption,
            onSelect = viewModel::select,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 192.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(items = groupedItems.entries.toList(), key = { it.key.id }) {
                DisplayDates(entry = it, onSelectDate = onSelectDate, colorOverrides = colorOverrides)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplaySelection(
    selected: DisplayOption,
    onSelect: (d: DisplayOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        DisplayOption.entries.forEach {
            FilterChip(selected = selected == it, modifier = Modifier.padding(horizontal = 4.dp), onClick = { onSelect(it) }, label = { Text(text = stringResource(it.label)) })
        }
    }
}

@Composable
fun DisplayDates(
    entry: Map.Entry<ItemConfiguration, List<List<DateDisplay>>>,
    modifier: Modifier = Modifier,
    colorOverrides: DisplayColors = DisplayColors(null, null),
    onSelectDate: (d: LocalDate) -> Unit = {},
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(300))
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = entry.key.name, style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                entry.value.first().forEach {
                    Text(
                        text = it.date.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault(),
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            entry.value.forEach {
                Row(modifier = Modifier.fillMaxWidth().aspectRatio(7f), horizontalArrangement = Arrangement.Center) {
                    it.forEach {
                        DisplayDate(
                            date = it,
                            modifier = Modifier.weight(1f),
                            onSelectDate = { onSelectDate(it.date) },
                            colorOverrides = colorOverrides,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayDate(
    date: DateDisplay,
    modifier: Modifier = Modifier,
    colorOverrides: DisplayColors = DisplayColors(null, null),
    onSelectDate: () -> Unit = {},
) {
    val flipConfigurationAgnosticLowColor = colorOverrides.lowValueColor ?: MaterialTheme.colorScheme.error
    val flipConfigurationAgnosticHighColor = colorOverrides.highValueColor ?: MaterialTheme.colorScheme.primary

    val (lowColor, highColor) = if (date.reverseColor) flipConfigurationAgnosticHighColor to flipConfigurationAgnosticLowColor else flipConfigurationAgnosticLowColor to flipConfigurationAgnosticHighColor

    val color = when {
        !date.inRange -> Color.Transparent
        date.value == null || date.maxValue == null -> Color.Transparent
        else -> Color(ArgbEvaluator().evaluate(date.value.toFloat().div(date.maxValue), lowColor.toArgb(), highColor.toArgb()) as Int)
    }

    val textAlpha = when {
        !date.inRange -> 0.5f
        else -> 1f
    }

    val textColor = when {
        color == Color.Transparent -> MaterialTheme.colorScheme.onBackground
        ColorUtils.calculateContrast(MaterialTheme.colorScheme.onPrimary.toArgb(), color.toArgb()) > 2.5f -> MaterialTheme.colorScheme.onPrimary
        ColorUtils.calculateContrast(MaterialTheme.colorScheme.onError.toArgb(), color.toArgb()) > 2.5f -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onBackground
    }

    val value = when {
        date.text != null -> stringResource(date.text)
        date.value != null -> date.value.toString()
        else -> null
    }

    val smallText = when {
        date.showValue -> date.date.dayOfMonth.toString()
        else -> null
    }

    val largeText = when {
        date.showValue -> value
        else -> date.date.dayOfMonth.toString()
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color)
            .fillMaxHeight()
            .semantics {
                contentDescription = date.date.localeFormat()
            }
            .clickable(
                enabled = date.date <= LocalDate.now(),
                onClick = onSelectDate,
            ),
    ) {
        // specifically allowing this because Caroline likes her phone supporting date displays this small
        val enoughSpaceToLookNice = maxHeight >= 35.dp
        val (smallTextAlignment, largeTextAlignment) = Pair(
            if (enoughSpaceToLookNice) Alignment.TopStart else Alignment.TopCenter,
            if (smallText == null || enoughSpaceToLookNice) Alignment.Center else Alignment.BottomCenter,
        )

        if (smallText != null) {
            Text(
                text = smallText,
                color = textColor,
                modifier = Modifier
                    .align(smallTextAlignment)
                    .alpha(textAlpha)
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraLight,
            )
        }
        if (largeText != null) {
            Text(
                text = largeText,
                color = textColor,
                modifier = Modifier
                    .alpha(textAlpha)
                    .align(largeTextAlignment),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light,
            )
        }
    }
}
