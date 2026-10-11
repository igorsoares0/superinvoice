package com.example.superinvoice.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.OnOrange
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.OrangeWash
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.util.InvoiceDates
import online.isdevapps.superinvoice.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * [value] e o retorno de [onValueChange] são a meia-noite UTC do dia (convenção de
 * [InvoiceDates]), que é exatamente o que o `DatePicker` usa; o texto só existe na exibição.
 */
fun DatePickerField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    dateFormatPattern: String = "MM/dd/yyyy",
    modifier: Modifier = Modifier,
    opensSection: Boolean = false
) {
    var showDatePicker by remember { mutableStateOf(false) }

    InvSelectField(
        label = label,
        value = InvoiceDates.format(value, dateFormatPattern),
        placeholder = label,
        onClick = { showDatePicker = true },
        modifier = modifier,
        opensSection = opensSection
    )

    if (showDatePicker) {
        // Criado a cada abertura: na edição a data chega depois da primeira composição.
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onValueChange)
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        style = InvType.action,
                        color = Orange
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = InvType.action,
                        color = Ink
                    )
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Paper)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Paper,
                    titleContentColor = Ink,
                    headlineContentColor = Ink,
                    weekdayContentColor = Ink,
                    subheadContentColor = Ink,
                    yearContentColor = Ink,
                    currentYearContentColor = Ink,
                    selectedYearContentColor = OnOrange,
                    selectedYearContainerColor = Orange,
                    dayContentColor = Ink,
                    selectedDayContentColor = OnOrange,
                    selectedDayContainerColor = Orange,
                    todayContentColor = Orange,
                    todayDateBorderColor = Orange,
                    dayInSelectionRangeContentColor = Ink,
                    dayInSelectionRangeContainerColor = OrangeWash
                )
            )
        }
    }
}
