package seamuslowry.daytracker.ui.shared

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
            SingleChoiceSegmentedButtonRow(
                modifier = modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight),
            ) {
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
                        colors = SegmentedButtonDefaults.colors(
                            // active buttons use the default button colors
                            activeContainerColor = ButtonDefaults.buttonColors().containerColor,
                            activeContentColor = ButtonDefaults.buttonColors().contentColor,
                            disabledActiveContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                            disabledActiveContentColor = ButtonDefaults.buttonColors().disabledContentColor,
                            // inactive buttons use the default outlined button colors
                            inactiveContainerColor = ButtonDefaults.outlinedButtonColors().containerColor,
                            inactiveContentColor = ButtonDefaults.outlinedButtonColors().contentColor,
                            disabledInactiveContainerColor = ButtonDefaults.outlinedButtonColors().disabledContainerColor,
                            disabledInactiveContentColor = ButtonDefaults.outlinedButtonColors().disabledContentColor,
                            // all borders use outline
                            activeBorderColor = MaterialTheme.colorScheme.outline,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                            disabledActiveBorderColor = MaterialTheme.colorScheme.outline,
                            disabledInactiveBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(
                            text = option.text?.let { text -> stringResource(id = text) }
                                ?: option.value.toString(),
                            softWrap = false,
                            modifier = Modifier.requiredWidth(IntrinsicSize.Max),
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
