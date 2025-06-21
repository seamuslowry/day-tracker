package seamuslowry.daytracker.ui.shared

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import seamuslowry.daytracker.R
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.LimitedOptionTrackingType
import seamuslowry.daytracker.models.TextEntryTrackingType
import seamuslowry.daytracker.models.TrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerEntry(
    trackerType: TrackingType,
    modifier: Modifier = Modifier,
    item: Item? = null,
    onChange: (Item) -> Unit = {},
    enabled: Boolean = true,
) {
    when (trackerType) {
        is LimitedOptionTrackingType ->
            Slider(
                value = item?.value?.toFloat() ?: 1f,
                onValueChange = { v -> item?.let { onChange(it.copy(value = v.toInt())) } },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight),
                enabled = enabled,
                thumb = {
                    Text(
                        text = it.value.toInt().toString(),
                        softWrap = false,
                        modifier = Modifier.requiredWidth(IntrinsicSize.Max),
                        overflow = TextOverflow.Visible,
                    )
                },
            )
        is TextEntryTrackingType -> DelayedSaveTextField(
            onSave = { newText -> item?.let { onChange(it.copy(comment = newText, value = -1)) } },
            value = item?.comment ?: "",
            placeholder = {
                Text(
                    text = stringResource(
                        R.string.text_tracker_placeholder,
                    ),
                )
            },
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}
