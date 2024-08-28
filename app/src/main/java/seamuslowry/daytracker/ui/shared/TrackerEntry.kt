package seamuslowry.daytracker.ui.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
            // notes: possible... text gets a little cramped. see what can be done about that
            SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
                trackerType.options.forEachIndexed { index, option ->
                    SegmentedButton(
                        enabled = enabled,
                        selected = option.value == item?.value,
                        icon = {},
                        onClick = { item?.let { onChange(it.copy(value = option.value)) } },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = trackerType.options.size,
                        ),
                    ) {
                        Text(
                            text = option.text?.let { text -> stringResource(id = text) }
                                ?: option.value.toString(),
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
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
