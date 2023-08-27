package seamuslowry.daytracker.ui.shared

import android.animation.ArgbEvaluator
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemConfiguration
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

data class DateDisplay(
    val value: Int? = null,
    @StringRes val text: Int? = null,
    val maxValue: Int? = null,
    val date: LocalDate,
    val inRange: Boolean = true,
    val showValue: Boolean = false,
)

@Composable
fun CalendarGrid(
    groupedItems: Map<ItemConfiguration, List<List<DateDisplay>>>,
    onSelectDate: (d: LocalDate) -> Unit = {},
    minCalendarSize: Dp,
    spacing: Dp = minCalendarSize.div(20).coerceAtLeast(5.dp),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCalendarSize),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = PaddingValues(spacing),
    ) {
        items(items = groupedItems.entries.toList(), key = { it.key.id }) {
            DisplayDates(entry = it, onSelectDate = onSelectDate)
        }
    }
}

@Composable
fun DisplayDates(
    entry: Map.Entry<ItemConfiguration, List<List<DateDisplay>>>,
    modifier: Modifier = Modifier,
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
    onSelectDate: () -> Unit = {},
) {
    val color = when {
        !date.inRange -> Color.Transparent
        date.value == null || date.maxValue == null -> Color.Transparent
        else -> Color(ArgbEvaluator().evaluate(date.value.toFloat().div(date.maxValue), MaterialTheme.colorScheme.error.toArgb(), MaterialTheme.colorScheme.primary.toArgb()) as Int)
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

    Box(
        modifier = modifier
            .background(color)
            .fillMaxHeight()
            .clickable(
                enabled = date.date <= LocalDate.now(),
                onClick = onSelectDate,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (smallText != null) {
            Text(
                text = smallText,
                color = textColor,
                modifier = Modifier
                    .alpha(textAlpha)
                    .align(Alignment.TopStart)
                    .padding(3.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
            )
        }
        if (largeText != null) {
            Text(
                text = largeText,
                color = textColor,
                modifier = Modifier
                    .alpha(textAlpha),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

fun mapToCalendarStructure(dateRange: ClosedRange<LocalDate>, itemsMap: Map<ItemConfiguration, List<Item>>, showRecordedValues: Boolean): Map<ItemConfiguration, List<List<DateDisplay>>> {
    val dayOfWeekField = WeekFields.of(Locale.getDefault()).dayOfWeek()
    val range = dateRange.start.range(dayOfWeekField)
    val blanksFrom = dateRange.start.with(dayOfWeekField, range.minimum)
    val blanksTo = dateRange.endInclusive.with(dayOfWeekField, range.maximum)
    val baseDate = DateDisplay(showValue = showRecordedValues, date = dateRange.start)

    val startingBlanks = List(ChronoUnit.DAYS.between(blanksFrom, dateRange.start).toInt()) { baseDate.copy(date = dateRange.start.minusDays(it.toLong() + 1), inRange = false) }.reversed()
    val endingBlanks = List(ChronoUnit.DAYS.between(dateRange.endInclusive, blanksTo).toInt()) { baseDate.copy(date = dateRange.endInclusive.plusDays(it.toLong() + 1), inRange = false) }

    val sequence = generateSequence(dateRange.start) { it.plusDays(1) }.takeWhile { it <= dateRange.endInclusive }

    return itemsMap.mapValues { entry ->
        val sequenceDisplays = sequence.map { date ->
            entry.value.firstOrNull { item -> item.date == date }?.let { item ->
                val selection = entry.key.trackingType.options.firstOrNull { it.value == item.value }
                baseDate.copy(value = item.value, text = selection?.shortText, maxValue = entry.key.trackingType.options.size, date = date)
            } ?: baseDate.copy(date = date)
        }.toList()

        (startingBlanks + sequenceDisplays + endingBlanks).chunked(range.maximum.toInt())
    }
}

@Composable
@Preview(widthDp = 41, heightDp = 41)
fun TestDisplayDate() {
    DisplayDate(date = DateDisplay(value = 10, maxValue = 10, date = LocalDate.now(), inRange = true, showValue = true))
}

@Composable
@Preview(widthDp = 25, heightDp = 25)
fun SmallDisplayDate() {
    DisplayDate(date = DateDisplay(value = 10, maxValue = 10, date = LocalDate.now(), inRange = true, showValue = true))
}

@Composable
@Preview(widthDp = 29, heightDp = 29)
fun CarolineDisplayDate() {
    DisplayDate(date = DateDisplay(value = 10, maxValue = 10, date = LocalDate.now().withDayOfMonth(22), inRange = true, showValue = true))
}

@Composable
@Preview(widthDp = 50, heightDp = 50)
fun NormalDisplayDate() {
    DisplayDate(date = DateDisplay(value = 10, maxValue = 10, date = LocalDate.now(), inRange = true, showValue = true))
}

@Composable
@Preview(widthDp = 50, heightDp = 50)
fun NormalNoValueDisplayDate() {
    DisplayDate(date = DateDisplay(value = 10, maxValue = 10, date = LocalDate.now(), inRange = true, showValue = false))
}
