package com.example.superinvoice.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvButtonVariant
import com.example.superinvoice.ui.components.InvDivider
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvStatusLabel
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.components.SettingsOption
import com.example.superinvoice.ui.components.documentStatusLabel
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.EditInvoiceViewModel
import com.example.superinvoice.util.getCurrencySymbol
import online.isdevapps.superinvoice.R

@Composable
fun EditInvoiceScreen(
    invoiceId: Int,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNavigateToSelectClient: () -> Unit = {},
    onNavigateToSelectProduct: () -> Unit = {},
    onNavigateToPreview: () -> Unit = {},
    pendingClientSelection: Client? = null,
    pendingProductSelection: ProductService? = null,
    clientSelectionVersion: Int = 0,
    productSelectionVersion: Int = 0,
    onClientSelectionProcessed: () -> Unit = {},
    onProductSelectionProcessed: () -> Unit = {},
    viewModel: EditInvoiceViewModel = hiltViewModel()
) {
    LaunchedEffect(invoiceId) {
        viewModel.loadInvoice(invoiceId)
    }

    val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDate.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val lineItems by viewModel.form.lineItems.collectAsStateWithLifecycle()
    val invoice by viewModel.invoice.collectAsStateWithLifecycle()

    val currencySymbol = getCurrencySymbol(currency)

    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEstimate = invoice?.type == DocumentType.ESTIMATE
    val (statusLabel, statusColor) = documentStatusLabel(
        invoice?.type ?: DocumentType.INVOICE,
        invoice?.status ?: InvoiceStatus.DRAFT
    )
    val balance = invoice?.let { InvoiceCalculator.balanceDue(it.totalAmount, it.amountPaid) } ?: 0.0

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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Paper,
            shape = InvShape.card,
            title = {
                Text(
                    text = stringResource(if (isEstimate) R.string.delete_estimate_title else R.string.delete_invoice_title),
                    style = InvType.sectionTitle,
                    color = Ink
                )
            },
            text = {
                Text(
                    text = stringResource(
                        if (isEstimate) R.string.delete_estimate_confirmation else R.string.delete_invoice_confirmation
                    ),
                    style = InvType.body,
                    color = Neutral
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInvoice {
                            showDeleteDialog = false
                            onClose()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        style = InvType.action,
                        color = Red
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = InvType.action,
                        color = Ink
                    )
                }
            }
        )
    }

    InvScaffold(modifier = Modifier.imePadding()) {
        InvScreenHeader(
            title = stringResource(if (isEstimate) R.string.title_edit_estimate else R.string.title_edit_invoice),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
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

            InvSectionRule()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Space.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                InvStatusLabel(
                    label = statusLabel,
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )
                // Pago e parcial vêm dos pagamentos; aqui só o atalho de quitar o saldo.
                // Orçamento não recebe pagamento: a resposta do cliente fica na pré-visualização.
                if (!isEstimate && balance > 0.0) {
                    Text(
                        text = stringResource(R.string.mark_as_paid),
                        style = InvType.action,
                        color = Orange,
                        modifier = Modifier.clickable { viewModel.markAsPaid() }
                    )
                }
            }
            InvDivider()

            Spacer(modifier = Modifier.height(Space.section))

            InvoiceNotesField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = stringResource(R.string.notes),
                opensSection = true
            )

            Spacer(modifier = Modifier.height(Space.section))

            InvoiceTotalsSection(form = viewModel.form, currencySymbol = currencySymbol, currency = currency)

            Text(
                text = stringResource(if (isEstimate) R.string.delete_estimate else R.string.delete_invoice),
                style = InvType.action,
                color = Red,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true }
                    .padding(vertical = Space.xl)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen, vertical = Space.xl),
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            InvButton(
                text = stringResource(R.string.preview),
                variant = InvButtonVariant.Secondary,
                onClick = onNavigateToPreview,
                modifier = Modifier.weight(1f)
            )
            InvButton(
                text = stringResource(R.string.save_changes),
                enabled = selectedClient != null && lineItems.isNotEmpty(),
                onClick = { viewModel.updateInvoice { onSave() } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
