package seamuslowry.daytracker.ui.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.sign

@Composable
fun <T> ArrowPicker(
    value: T,
    onIncrement: (oldValue: T) -> Unit,
    onDecrement: (oldValue: T) -> Unit,
    compare: (leftValue: T, rightValue: T) -> Int,
    incrementLabel: String,
    decrementLabel: String,
    modifier: Modifier = Modifier,
    incrementEnabled: Boolean = true,
    decrementEnabled: Boolean = true,
    content: @Composable (value: T) -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onDecrement(value) },
            enabled = decrementEnabled,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = decrementLabel)
        }
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = value,
            transitionSpec = {
                val inModifier = compare(targetState, initialState).sign
                val outModifier = -inModifier
                slideInHorizontally { height -> height * inModifier } + fadeIn() togetherWith slideOutHorizontally { height -> height * outModifier } + fadeOut() using SizeTransform(clip = false)
            },
            label = "arrowPickerContent",
        ) { targetType ->
            content(targetType)
        }
        IconButton(
            onClick = { onIncrement(value) },
            enabled = incrementEnabled,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = incrementLabel)
        }
    }
}

@Composable
fun <T : Comparable<T>> ArrowPicker(
    value: T,
    onIncrement: (oldValue: T) -> Unit,
    onDecrement: (oldValue: T) -> Unit,
    incrementLabel: String,
    decrementLabel: String,
    modifier: Modifier = Modifier,
    incrementEnabled: Boolean = true,
    decrementEnabled: Boolean = true,
    content: @Composable (value: T) -> Unit,
) {
    ArrowPicker(
        value = value,
        onIncrement = onIncrement,
        onDecrement = onDecrement,
        incrementLabel = incrementLabel,
        decrementLabel = decrementLabel,
        modifier = modifier,
        incrementEnabled = incrementEnabled,
        decrementEnabled = decrementEnabled,
        compare = Comparable<T>::compareTo,
        content = content,
    )
}

@Composable
fun ArrowPicker(
    value: Long,
    onChange: (value: Long) -> Unit,
    range: LongRange,
    incrementLabel: String,
    decrementLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable (value: Long) -> Unit,
) {
    ArrowPicker(
        value = value,
        onIncrement = { onChange(it.plus(1)) },
        onDecrement = { onChange(it.minus(1)) },
        incrementEnabled = value < range.last,
        decrementEnabled = value > range.first,
        incrementLabel = incrementLabel,
        decrementLabel = decrementLabel,
        modifier = modifier,
        content = content,
    )
}
