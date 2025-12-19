package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.superinvoice.R
import com.example.superinvoice.ui.components.DatePickerField
import com.example.superinvoice.ui.components.InvoiceInputField
import com.example.superinvoice.ui.components.InvoiceNotesField
import com.example.superinvoice.ui.components.InvoiceOptionItem
import com.example.superinvoice.ui.components.InvoiceTotalCard

@Composable
fun CreateInvoiceScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNavigateToClients: () -> Unit = {},
    onNavigateToProductsServices: () -> Unit = {}
) {
    var invoiceName by remember { mutableStateOf("#820") }
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }

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
                    text = "New Invoice",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InvoiceInputField(
                        label = "Invoice name #",
                        value = invoiceName,
                        onValueChange = { invoiceName = it },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "Due Date",
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                InvoiceOptionItem(
                    text = "Add Client",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToClients
                )

                InvoiceOptionItem(
                    text = "Add Product or Service",
                    icon = Icons.Default.Add,
                    onClick = onNavigateToProductsServices
                )

                InvoiceInputField(
                    label = "Tax (Optional)",
                    value = tax,
                    onValueChange = { tax = it }
                )

                InvoiceInputField(
                    label = "Discount (Optional)",
                    value = discount,
                    onValueChange = { discount = it }
                )

                InvoiceNotesField(
                    value = notes,
                    onValueChange = { notes = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                InvoiceTotalCard(total = 100.00)

                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9DEA6E),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
