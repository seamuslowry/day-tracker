package seamuslowry.daytracker.ui.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> SegmentedButtons(
    values: List<T>,
    onChange: (value: T) -> Unit,
    modifier: Modifier = Modifier,
    value: T? = null,
    enabled: Boolean = true,
    buttonContent: @Composable (value: T) -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        values.forEach {
            val startPercentage = if (it == values.first()) 50 else 0
            val endPercentage = if (it == values.last()) 50 else 0
            val colors = if (value == it) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                contentPadding = PaddingValues(),
                colors = colors,
                enabled = enabled,
                onClick = { onChange(it) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(
                    topStartPercent = startPercentage,
                    bottomStartPercent = startPercentage,
                    topEndPercent = endPercentage,
                    bottomEndPercent = endPercentage,
                ),
            ) {
                buttonContent(it)
            }
        }
    }
}
