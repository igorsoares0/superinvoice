package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvAmountDialog
import com.example.superinvoice.ui.components.InvChip
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvLineItemRow
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.components.InvTotalSummary
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.components.SettingsOption
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
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
    viewModel: CreateInvoiceViewModel = hiltViewModel()
) {
    val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDate.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tax by viewModel.tax.collectAsStateWithLifecycle()
    val discount by viewModel.discount.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val lineItems by viewModel.lineItems.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()

    val currencySymbol = getCurrencySymbol(currency)

    var showTaxDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var taxInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }

    LaunchedEffect(shouldReset) {
        if (shouldReset) {
            viewModel.resetForm()
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
            viewModel.addLineItem(pendingProductSelection, 1)
            onProductSelectionProcessed()
        }
    }

    if (showTaxDialog) {
        InvAmountDialog(
            title = stringResource(R.string.tax_amount),
            label = stringResource(R.string.enter_tax_amount),
            value = taxInput,
            onValueChange = { taxInput = it },
            onConfirm = {
                viewModel.setTax(taxInput)
                showTaxDialog = false
            },
            onDismiss = { showTaxDialog = false },
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel)
        )
    }

    if (showDiscountDialog) {
        InvAmountDialog(
            title = stringResource(R.string.discount_amount),
            label = stringResource(R.string.enter_discount_amount),
            value = discountInput,
            onValueChange = { discountInput = it },
            onConfirm = {
                viewModel.setDiscount(discountInput)
                showDiscountDialog = false
            },
            onDismiss = { showDiscountDialog = false },
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel)
        )
    }

    InvSettingsSubScreen(
        title = stringResource(R.string.title_new_invoice),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        saveEnabled = selectedClient != null && lineItems.isNotEmpty(),
        onSave = { viewModel.saveInvoice { onSave() } }
    ) {
        InvField(
            label = stringResource(R.string.invoice_name_label),
            value = invoiceNumber,
            onValueChange = { viewModel.setInvoiceNumber(it) },
            opensSection = true
        )

        DatePickerField(
            label = stringResource(R.string.due_date),
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

        if (lineItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Space.section))
            Text(
                text = stringResource(R.string.items).uppercase(),
                style = InvType.label,
                color = Neutral,
                modifier = Modifier.padding(bottom = Space.md)
            )
            InvSectionRule()
            lineItems.forEachIndexed { index, item ->
                InvLineItemRow(
                    name = item.productService.name,
                    unitPrice = stringResource(
                        R.string.price_per_unit,
                        "$currencySymbol${String.format("%.2f", item.productService.pricePerUnit)}"
                    ),
                    quantity = item.quantity,
                    lineTotal = "$currencySymbol${String.format("%.2f", item.lineTotal)}",
                    onDecrease = {
                        viewModel.updateLineItemQuantity(index, item.quantity - 1)
                    },
                    onIncrease = {
                        viewModel.updateLineItemQuantity(index, item.quantity + 1)
                    },
                    onRemove = { viewModel.removeLineItem(index) },
                    removeContentDescription = stringResource(R.string.remove)
                )
            }
        }

        Spacer(modifier = Modifier.height(Space.section))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            InvChip(
                text = if (tax.isNotEmpty() && (tax.toDoubleOrNull() ?: 0.0) > 0) {
                    stringResource(R.string.tax_with_amount, "$currencySymbol$tax")
                } else {
                    stringResource(R.string.tax)
                },
                onClick = {
                    taxInput = tax
                    showTaxDialog = true
                }
            )
            InvChip(
                text = if (discount.isNotEmpty() && (discount.toDoubleOrNull() ?: 0.0) > 0) {
                    stringResource(R.string.discount_with_amount, "$currencySymbol$discount")
                } else {
                    stringResource(R.string.discount)
                },
                onClick = {
                    discountInput = discount
                    showDiscountDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(Space.section))

        InvoiceNotesField(
            value = notes,
            onValueChange = { viewModel.setNotes(it) },
            label = stringResource(R.string.notes),
            opensSection = true
        )

        Spacer(modifier = Modifier.height(Space.section))

        InvTotalSummary(
            label = stringResource(R.string.total),
            amount = "$currencySymbol${String.format("%.2f", totalAmount)}",
            meta = currency
        )

        Spacer(modifier = Modifier.height(Space.xl))
    }
}
