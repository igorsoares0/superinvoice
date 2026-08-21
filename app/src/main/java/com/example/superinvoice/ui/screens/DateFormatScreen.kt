package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvSelectableRow
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.DateFormatViewModel
import online.isdevapps.superinvoice.R

data class DateFormat(
    val format: String,
    val example: String,
    val description: String
)

@Composable
fun DateFormatScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: DateFormatViewModel = hiltViewModel()
) {
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()

    val dateFormats = listOf(
        DateFormat("MM/dd/yyyy", "12/31/2024", "US Format"),
        DateFormat("dd/MM/yyyy", "31/12/2024", "European Format"),
        DateFormat("yyyy-MM-dd", "2024-12-31", "ISO Format"),
        DateFormat("dd.MM.yyyy", "31.12.2024", "German Format"),
        DateFormat("dd-MM-yyyy", "31-12-2024", "Alternative Format"),
        DateFormat("MMMM dd, yyyy", "December 31, 2024", "Long Format"),
        DateFormat("dd MMMM yyyy", "31 December 2024", "British Format"),
        DateFormat("MMM dd, yyyy", "Dec 31, 2024", "Short Month Format")
    )

    InvSettingsSubScreen(
        title = stringResource(R.string.title_date_format),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = {
            viewModel.saveFormat()
            onSave()
        }
    ) {
        InvSectionRule()
        dateFormats.forEach { dateFormat ->
            InvSelectableRow(
                title = dateFormat.example,
                subtitle = dateFormat.description,
                isSelected = selectedFormat == dateFormat.format,
                onClick = { viewModel.setSelectedFormat(dateFormat.format) }
            )
        }
        Spacer(modifier = Modifier.height(Space.xl))
    }
}
