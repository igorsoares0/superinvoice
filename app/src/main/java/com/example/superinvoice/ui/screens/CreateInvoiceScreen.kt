package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.isdevapps.superinvoice.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvoiceInputField
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.viewmodel.CreateInvoiceViewModel
import com.example.superinvoice.util.getCurrencySymbol

@Composable
fun CreateInvoiceScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNavigateToSelectClient: () -> Unit = {},
    onNavigateToSelectProduct: () -> Unit = {},
    pendingClientSelection: com.example.superinvoice.data.Client? = null,
    pendingProductSelection: com.example.superinvoice.data.ProductService? = null,
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
    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()

    val currencySymbol = getCurrencySymbol(currency)

    var showTaxDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var taxInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }

    // Reset form only when shouldReset is true (first open)
    LaunchedEffect(shouldReset) {
        if (shouldReset) {
            viewModel.resetForm()
        }
    }

    // Process pending client selection using version counter
    LaunchedEffect(clientSelectionVersion) {
        if (clientSelectionVersion > 0 && pendingClientSelection != null) {
            viewModel.setSelectedClient(pendingClientSelection)
            onClientSelectionProcessed()
        }
    }

    // Process pending product selection using version counter
    LaunchedEffect(productSelectionVersion) {
        if (productSelectionVersion > 0 && pendingProductSelection != null) {
            viewModel.addLineItem(pendingProductSelection, 1) // Default quantity 1
            onProductSelectionProcessed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.title_new_invoice),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Invoice Number and Due Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Invoice Number Field (editable)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Label at top
                            Text(
                                text = stringResource(R.string.invoice_name_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // Editable value field
                            BasicTextField(
                                value = invoiceNumber,
                                onValueChange = { viewModel.setInvoiceNumber(it) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 16.dp)
                            )
                        }
                    }

                    // Due Date Field
                    DatePickerField(
                        label = stringResource(R.string.due_date),
                        value = dueDate,
                        onValueChange = { viewModel.setDueDate(it) },
                        dateFormatPattern = dateFormat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Add Client Button
                NavigationButton(
                    icon = Icons.Default.Person,
                    text = selectedClient?.name ?: stringResource(R.string.add_client),
                    onClick = onNavigateToSelectClient,
                    useGreenCircle = true
                )

                // Add Product or Service Button
                NavigationButton(
                    icon = Icons.Default.Add,
                    text = stringResource(R.string.add_product_or_service),
                    onClick = onNavigateToSelectProduct,
                    useGreenCircle = true
                )

                // Items Section (below the button)
                if (lineItems.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.items),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )

                    lineItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productService.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(R.string.price_per_unit, "$currencySymbol${String.format("%.2f", item.productService.pricePerUnit)}"),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                // Quantity controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(
                                                1.dp,
                                                if (item.quantity > 1) Color.Black else Color.Gray,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable(enabled = item.quantity > 1) {
                                                if (item.quantity > 1) {
                                                    viewModel.updateLineItemQuantity(index, item.quantity - 1)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "-",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.quantity > 1) Color.Black else Color.Gray
                                        )
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                            .clickable {
                                                viewModel.updateLineItemQuantity(index, item.quantity + 1)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = stringResource(R.string.total_with_amount, "$currencySymbol${String.format("%.2f", item.lineTotal)}"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.removeLineItem(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = Color.Red
                                )
                            }
                        }
                        if (index < lineItems.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Tax Button
                NavigationButton(
                    icon = null,
                    iconText = "%",
                    text = if (tax.isNotEmpty() && tax.toDoubleOrNull() != null && tax.toDouble() > 0)
                        stringResource(R.string.tax_with_amount, "$currencySymbol$tax") else stringResource(R.string.tax),
                    onClick = {
                        taxInput = tax
                        showTaxDialog = true
                    }
                )

                // Discount Button
                NavigationButton(
                    icon = null,
                    iconText = "%",
                    text = if (discount.isNotEmpty() && discount.toDoubleOrNull() != null && discount.toDouble() > 0)
                        stringResource(R.string.discount_with_amount, "$currencySymbol$discount") else stringResource(R.string.discount),
                    onClick = {
                        discountInput = discount
                        showDiscountDialog = true
                    }
                )

                // Notes Field
                Column {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.setNotes(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.notes), color = Color.Gray)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9DEA6E),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.total),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                    Text(
                        text = "$currency $currencySymbol${String.format("%.2f", totalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    viewModel.saveInvoice {
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9DEA6E),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedClient != null && lineItems.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Tax Dialog
        if (showTaxDialog) {
            AlertDialog(
                onDismissRequest = { showTaxDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.tax_amount),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = taxInput,
                        onValueChange = { taxInput = it },
                        label = { Text(stringResource(R.string.enter_tax_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9DEA6E),
                            focusedLabelColor = Color(0xFF9DEA6E),
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setTax(taxInput)
                            showTaxDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.ok), color = Color(0xFF9DEA6E), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTaxDialog = false }) {
                        Text(stringResource(R.string.cancel), color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color(0xFFF9FAFB)
            )
        }

        // Discount Dialog
        if (showDiscountDialog) {
            AlertDialog(
                onDismissRequest = { showDiscountDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.discount_amount),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text(stringResource(R.string.enter_discount_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9DEA6E),
                            focusedLabelColor = Color(0xFF9DEA6E),
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setDiscount(discountInput)
                            showDiscountDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.ok), color = Color(0xFF9DEA6E), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscountDialog = false }) {
                        Text(stringResource(R.string.cancel), color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color(0xFFF9FAFB)
            )
        }
    }
}

@Composable
private fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconText: String? = null,
    text: String,
    onClick: () -> Unit,
    useGreenCircle: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(if (useGreenCircle) 12.dp else 8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.weight(1f)
        ) {
            if (useGreenCircle) {
                // Green circle with icon (for Add Client and Add Product)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFF9DEA6E),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color.Black
                        )
                    } else if (iconText != null) {
                        Text(
                            text = iconText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                // Original style (for Tax and Discount)
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                } else if (iconText != null) {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(if (useGreenCircle) 24.dp else 20.dp),
            tint = if (useGreenCircle) Color.Black else Color.Gray
        )
    }
}
