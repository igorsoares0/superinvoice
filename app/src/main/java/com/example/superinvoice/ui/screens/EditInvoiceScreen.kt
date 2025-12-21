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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvoiceInputField
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.viewmodel.EditInvoiceViewModel

@Composable
fun EditInvoiceScreen(
    invoiceId: Int,
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
    viewModel: EditInvoiceViewModel = hiltViewModel()
) {
    LaunchedEffect(invoiceId) {
        viewModel.loadInvoice(invoiceId)
    }

    val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDate.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tax by viewModel.tax.collectAsStateWithLifecycle()
    val discount by viewModel.discount.collectAsStateWithLifecycle()
    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val lineItems by viewModel.lineItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()

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
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 72.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }

                Text(
                    text = "Edit Invoice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(24.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InvoiceInputField(
                    label = "Invoice Number",
                    value = invoiceNumber,
                    onValueChange = { },
                    enabled = false
                )

                // Client Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToSelectClient() }
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedClient?.name ?: "Select Client",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (selectedClient == null) Color.Gray else Color.Black
                    )
                }

                DatePickerField(
                    label = "Due Date",
                    value = dueDate,
                    onValueChange = { viewModel.setDueDate(it) }
                )

                Text(
                    text = "Items",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
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
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.quantity} x $${item.productService.pricePerUnit}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Total: $${String.format("%.2f", item.lineTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { viewModel.removeLineItem(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = Color.Red
                            )
                        }
                    }
                }

                // Add Item Button
                Button(
                    onClick = { onNavigateToSelectProduct() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9DEA6E),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(text = "Add Item", modifier = Modifier.padding(start = 8.dp))
                }

                InvoiceInputField(
                    label = "Tax (Optional)",
                    value = tax,
                    onValueChange = { viewModel.setTax(it) }
                )

                InvoiceInputField(
                    label = "Discount (Optional)",
                    value = discount,
                    onValueChange = { viewModel.setDiscount(it) }
                )

                InvoiceNotesField(
                    value = notes,
                    onValueChange = { viewModel.setNotes(it) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subtotal:", fontWeight = FontWeight.Normal)
                        Text(
                            text = "$${String.format("%.2f", subtotal)}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "$${String.format("%.2f", totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.deleteInvoice {
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(
                            width = 1.dp,
                            color = Color.Red,
                            shape = RoundedCornerShape(26.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.updateInvoice {
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9DEA6E),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp),
                    enabled = selectedClient != null && lineItems.isNotEmpty()
                ) {
                    Text(
                        text = "Update",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
