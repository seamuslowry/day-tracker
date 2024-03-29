package seamuslowry.daytracker.ui.shared

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import kotlin.math.sign

@Composable
fun <T> ArrowPicker(
    value: T,
    onIncrement: (oldValue: T) -> Unit,
    onDecrement: (oldValue: T) -> Unit,
    compare: (leftValue: T, rightValue: T) -> Int,
    @StringRes incrementResource: Int,
    @StringRes decrementResource: Int,
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
            Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = stringResource(incrementResource))
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
            Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = stringResource(decrementResource))
        }
    }
}

@Composable
fun <T : Comparable<T>> ArrowPicker(
    value: T,
    onIncrement: (oldValue: T) -> Unit,
    onDecrement: (oldValue: T) -> Unit,
    @StringRes incrementResource: Int,
    @StringRes decrementResource: Int,
    modifier: Modifier = Modifier,
    incrementEnabled: Boolean = true,
    decrementEnabled: Boolean = true,
    content: @Composable (value: T) -> Unit,
) {
    ArrowPicker(
        value = value,
        onIncrement = onIncrement,
        onDecrement = onDecrement,
        incrementResource = incrementResource,
        decrementResource = decrementResource,
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
    @StringRes incrementResource: Int,
    @StringRes decrementResource: Int,
    modifier: Modifier = Modifier,
    content: @Composable (value: Long) -> Unit,
) {
    ArrowPicker(
        value = value,
        onIncrement = { onChange(it.plus(1)) },
        onDecrement = { onChange(it.minus(1)) },
        incrementEnabled = value < range.last,
        decrementEnabled = value > range.first,
        incrementResource = incrementResource,
        decrementResource = decrementResource,
        modifier = modifier,
        content = content,
    )
}
