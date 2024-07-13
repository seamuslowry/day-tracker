package seamuslowry.daytracker.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color.parseColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import seamuslowry.daytracker.R
import seamuslowry.daytracker.models.localeFormat
import java.time.LocalTime

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        ReminderSection(
            reminderEnabled = state.reminderEnabled,
            reminderTime = state.reminderTime,
            onSetReminderEnabled = { scope.launch { viewModel.setReminderEnabled(it) } },
            onSetReminderTime = { scope.launch { viewModel.setReminderTime(it) } },
        )
        CalendarSection(showValues = state.showRecordedValues, onSetShowValues = { scope.launch { viewModel.setShowRecordedValues(it) } })
        ColorSection(
            lowColor = state.lowValueColor ?: MaterialTheme.colorScheme.error,
            highColor = state.highValueColor ?: MaterialTheme.colorScheme.primary,
            onSetLowColor = { scope.launch { viewModel.setLowValueArgb(it) } },
            onSetHighColor = { scope.launch { viewModel.setHighValueArgb(it) } },
        )
    }
}

@Composable
fun CalendarSection(
    showValues: Boolean,
    onSetShowValues: (value: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.calendar_section_title), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.show_recorded_values))
            Switch(checked = showValues, onCheckedChange = onSetShowValues)
        }
    }
}

@Composable
fun ReminderSection(
    reminderEnabled: Boolean,
    onSetReminderEnabled: (value: Boolean) -> Unit,
    reminderTime: LocalTime,
    onSetReminderTime: (time: LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onSetReminderEnabled,
    )
    var pickingTime by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val requiredPermission = Manifest.permission.POST_NOTIFICATIONS

    LaunchedEffect(key1 = reminderEnabled) {
        if (reminderEnabled && ContextCompat.checkSelfPermission(context, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(requiredPermission)
        }
    }

    Column(modifier = modifier) {
        Text(text = stringResource(R.string.reminders_section_title), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.enabled))
            Switch(checked = reminderEnabled, onCheckedChange = onSetReminderEnabled)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.time))
            TextButton(onClick = { pickingTime = true }, enabled = reminderEnabled) {
                Text(text = reminderTime.localeFormat())
            }
        }
    }
    if (pickingTime) {
        ReminderTimePicker(
            initialTime = reminderTime,
            onConfirm = {
                onSetReminderTime(it)
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
}

@Composable
fun ColorSection(
    lowColor: Color,
    highColor: Color,
    onSetLowColor: (c: Color) -> Unit,
    onSetHighColor: (c: Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.color_section_title), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.low_argb), modifier = Modifier.weight(1f))
            ColorTextField(color = lowColor, onColorChange = onSetLowColor, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.high_argb), modifier = Modifier.weight(1f))
            ColorTextField(color = highColor, onColorChange = onSetHighColor, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
@OptIn(ExperimentalStdlibApi::class)
private fun ColorTextField(
    color: Color,
    onColorChange: (c: Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textColor by remember { mutableStateOf(color.toArgb().toHexString(HexFormat.UpperCase)) }

    LaunchedEffect(key1 = textColor) {
        try {
            onColorChange(Color(parseColor("#$textColor")))
        } catch (e: Exception) {
            onColorChange(Color.Unspecified)
        }
    }

    OutlinedTextField(
        value = textColor,
        onValueChange = { textColor = it.uppercase() },
        modifier = modifier,
        prefix = { Text(text = stringResource(R.string.hex_prefix)) },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = color,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePicker(
    initialTime: LocalTime,
    onConfirm: (time: LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = rememberTimePickerState(initialTime.hour, initialTime.minute, false)

    AlertDialog(
        modifier = modifier,
        text = {
            TimePicker(state = time)
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(time.hour, time.minute))
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
