package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    dateFormatPattern: String = "MM/dd/yyyy",
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { showDatePicker = true }
    ) {
        // Label floating at top
        if (value.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
            )
        }

        // Value or placeholder
        Text(
            text = if (value.isEmpty()) label else value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (value.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = 16.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (value.isEmpty()) 16.dp else 28.dp,
                bottom = 16.dp
            )
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val dateFormat = SimpleDateFormat(dateFormatPattern, Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val formattedDate = dateFormat.format(calendar.time)
                            onValueChange(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "OK",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFFF9FAFB)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFFF9FAFB),
                    titleContentColor = Color.Black,
                    headlineContentColor = Color.Black,
                    weekdayContentColor = Color.Black,
                    subheadContentColor = Color.Black,
                    yearContentColor = Color.Black,
                    currentYearContentColor = Color.Black,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = Color(0xFF9DEA6E),
                    dayContentColor = Color.Black,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = Color(0xFF9DEA6E),
                    todayContentColor = Color.Black,
                    todayDateBorderColor = Color(0xFF9DEA6E),
                    dayInSelectionRangeContentColor = Color.Black,
                    dayInSelectionRangeContainerColor = Color(0xFF9DEA6E).copy(alpha = 0.3f)
                )
            )
        }
    }
}
