package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdenseDatePickerDialog(
    initialDateStr: String, // "YYYY-MM-DD"
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateStr) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(initialDateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onDateSelected(sdf.format(Date(millis)))
                    }
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Clickable input field that displays selected date and opens the Material 3 DatePickerDialog.
 */
@Composable
fun EdenseDatePickerField(
    label: String,
    dateValue: String, // "YYYY-MM-DD"
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = dateValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Pick Date"
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = OutlinedTextFieldDefaults.colors().unfocusedTextColor,
                disabledBorderColor = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
                disabledLabelColor = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
                disabledTrailingIconColor = OutlinedTextFieldDefaults.colors().unfocusedTrailingIconColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
        )
        // Transparent clickable overlay to guarantee click capture across entire box
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        EdenseDatePickerDialog(
            initialDateStr = dateValue.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            },
            onDateSelected = onDateSelected,
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Clickable input field that displays time and opens a TimePickerDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdenseTimePickerField(
    label: String,
    timeValue: String, // "HH:mm"
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = timeValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Pick Time"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        val parts = timeValue.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val timeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select $label") },
            text = {
                TimePicker(state = timeState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = String.format("%02d:%02d", timeState.hour, timeState.minute)
                        onTimeSelected(formatted)
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
