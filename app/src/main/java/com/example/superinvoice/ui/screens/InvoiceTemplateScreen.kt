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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InvoiceTemplateScreen(
    onClose: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFFFFFFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Template Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Invoice Template Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .background(Color.White)
                    .padding(32.dp)
            ) {
                Column {
                    // Title with line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color.Black
                        )
                        Text(
                            text = "INVOICE",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Issued To and Invoice Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Column - Issued To
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ISSUED TO:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "XYZ Client",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                            Text(
                                text = "XYZ Enterprises Ltd.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                            Text(
                                text = "123 Business St., Any City",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                        }

                        // Right Column - Invoice Info
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Row {
                                Text(
                                    text = "INVOICE NO:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "820",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = "DATE:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "12.19.2024",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = "DUE DATE:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "12.19.2024",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pay To Section
                    Column {
                        Text(
                            text = "PAY TO:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Business Bank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        Text(
                            text = "Account Name: SuperInvoice Business",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        Text(
                            text = "Account Number: 1234567890",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Table Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DESCRIPTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "UNIT PRICE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "QTY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.5f)
                        )
                        Text(
                            text = "TOTAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Items
                    InvoiceLineItem("Website Creation", "100", "1", "$100")
                    InvoiceLineItem("Brand Design", "100", "1", "$100")
                    InvoiceLineItem("Logo Design", "100", "1", "$100")
                    InvoiceLineItem("Social Media Templates", "100", "1", "$100")
                    InvoiceLineItem("Brand Photography", "100", "1", "$100")
                    InvoiceLineItem("Brand Guide", "100", "1", "$100")

                    Spacer(modifier = Modifier.height(24.dp))

                    // Totals
                    Column(
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SUBTOTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = "$600",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TAX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = "10%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TOTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = "$660",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Signature
                    Text(
                        text = "John Johnston",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InvoiceLineItem(
    description: String,
    unitPrice: String,
    quantity: String,
    total: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = description,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = unitPrice,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = quantity,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = total,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
