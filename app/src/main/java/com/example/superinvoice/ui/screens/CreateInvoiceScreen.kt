package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.components.SettingsOption
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.CreateInvoiceViewModel
import com.example.superinvoice.util.getCurrencySymbol
import online.isdevapps.superinvoice.R

@Composable
fun CreateInvoiceScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNavigateToSelectClient: () -> Unit = {},
    onNavigateToSelectProduct: () -> Unit = {},
    pendingClientSelection: Client? = null,
    pendingProductSelection: ProductService? = null,
    clientSelectionVersion: Int = 0,
    productSelectionVersion: Int = 0,
    onClientSelectionProcessed: () -> Unit = {},
    onProductSelectionProcessed: () -> Unit = {},
    shouldReset: Boolean = false,
    documentType: DocumentType = DocumentType.INVOICE,
    viewModel: CreateInvoiceViewModel = hiltViewModel()
) {
    val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDate.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val lineItems by viewModel.form.lineItems.collectAsStateWithLifecycle()
    val isEstimate = viewModel.documentType.collectAsStateWithLifecycle().value == DocumentType.ESTIMATE

    val currencySymbol = getCurrencySymbol(currency)

    LaunchedEffect(shouldReset) {
        if (shouldReset) {
            viewModel.resetForm(documentType)
        }
    }

    LaunchedEffect(clientSelectionVersion) {
        if (clientSelectionVersion > 0 && pendingClientSelection != null) {
            viewModel.setSelectedClient(pendingClientSelection)
            onClientSelectionProcessed()
        }
    }

    LaunchedEffect(productSelectionVersion) {
        if (productSelectionVersion > 0 && pendingProductSelection != null) {
            viewModel.form.addLineItem(pendingProductSelection)
            onProductSelectionProcessed()
        }
    }

    InvSettingsSubScreen(
        title = stringResource(if (isEstimate) R.string.title_new_estimate else R.string.title_new_invoice),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        saveEnabled = selectedClient != null && lineItems.isNotEmpty(),
        onSave = { viewModel.saveInvoice { onSave() } }
    ) {
        InvField(
            label = stringResource(if (isEstimate) R.string.estimate_name_label else R.string.invoice_name_label),
            value = invoiceNumber,
            onValueChange = { viewModel.setInvoiceNumber(it) },
            opensSection = true
        )

        DatePickerField(
            label = stringResource(if (isEstimate) R.string.valid_until else R.string.due_date),
            value = dueDate,
            onValueChange = { viewModel.setDueDate(it) },
            dateFormatPattern = dateFormat
        )

        SettingsOption(
            text = selectedClient?.name ?: stringResource(R.string.add_client),
            icon = InvIcons.Person,
            onClick = onNavigateToSelectClient
        )

        SettingsOption(
            text = stringResource(R.string.add_product_or_service),
            icon = InvIcons.Plus,
            onClick = onNavigateToSelectProduct
        )

        InvoiceLineItemsSection(form = viewModel.form, currencySymbol = currencySymbol)

        Spacer(modifier = Modifier.height(Space.section))

        InvoiceAdjustmentChips(form = viewModel.form, currencySymbol = currencySymbol)

        Spacer(modifier = Modifier.height(Space.section))

        InvoiceNotesField(
            value = notes,
            onValueChange = { viewModel.setNotes(it) },
            label = stringResource(R.string.notes),
            opensSection = true
        )

        Spacer(modifier = Modifier.height(Space.section))

        InvoiceTotalsSection(form = viewModel.form, currencySymbol = currencySymbol, currency = currency)

        Spacer(modifier = Modifier.height(Space.xl))
    }
}
