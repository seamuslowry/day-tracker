package seamuslowry.daytracker.ui.shared

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import seamuslowry.daytracker.models.LimitedOptionTrackingType
import seamuslowry.daytracker.models.TextEntryTrackingType
import seamuslowry.daytracker.models.TrackingType

@Composable
fun TrackerEntry(
    trackerType: TrackingType,
    modifier: Modifier = Modifier,
    value: Int? = null,
    onChange: (Int) -> Unit = {},
    enabled: Boolean = true,
) {
    when (trackerType) {
        is LimitedOptionTrackingType -> SegmentedButtons(
            values = trackerType.options,
            value = trackerType.options.find { it.value == value },
            onChange = { onChange(it.value) },
            enabled = enabled,
            modifier = modifier
                .defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight),
        ) {
            Text(text = it.text?.let { text -> stringResource(id = text) } ?: it.value.toString())
        }
        is TextEntryTrackingType -> OutlinedTextField(value = "", placeholder = { Text(text = "Free Text Entry") }, onValueChange = {}, enabled = enabled, modifier = modifier)
    }
}
