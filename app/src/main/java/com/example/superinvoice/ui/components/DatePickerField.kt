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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = value.ifEmpty { "Select date" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (value.isEmpty()) Color.Gray else Color.Black
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            val dateFormat = SimpleDateFormat("MMM, dd", Locale.getDefault())
                            val formattedDate = dateFormat.format(calendar.time).lowercase()
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
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
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
