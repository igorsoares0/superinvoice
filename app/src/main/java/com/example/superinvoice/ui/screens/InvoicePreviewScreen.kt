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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun InvoicePreviewScreen(
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSaveAsPdf: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFFFFFFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    text = "Invoice preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Invoice Template Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0))
                        .background(Color.White)
                        .padding(24.dp)
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
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Normal,
                                fontSize = 20.sp,
                                letterSpacing = 3.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

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
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "XYZ Client",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                Text(
                                    text = "XYZ Enterprises Ltd.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                Text(
                                    text = "123 Business St., Any City",
                                    fontSize = 10.sp,
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
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "820",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row {
                                    Text(
                                        text = "DATE:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "12.19.2024",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row {
                                    Text(
                                        text = "DUE DATE:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "12.19.2024",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pay To Section
                        Column {
                            Text(
                                text = "PAY TO:",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Business Bank",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                            Text(
                                text = "Account Name: SuperInvoice Business",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                            Text(
                                text = "Account Number: 1234567890",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DESCRIPTION",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "UNIT PRICE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "QTY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = "TOTAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Items
                        PreviewInvoiceLineItem("Brand consultation", "100", "1", "$100")
                        PreviewInvoiceLineItem("Logo design", "100", "1", "$100")
                        PreviewInvoiceLineItem("Website design", "100", "1", "$100")
                        PreviewInvoiceLineItem("Social media templates", "100", "1", "$100")
                        PreviewInvoiceLineItem("Brand photography", "100", "1", "$100")
                        PreviewInvoiceLineItem("Brand guide", "100", "1", "$100")

                        Spacer(modifier = Modifier.height(20.dp))

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
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(
                                    text = "$600",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "TAX",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(
                                    text = "10%",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "TOTAL",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(
                                    text = "$660",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Signature
                        Text(
                            text = "John Johnston",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Share",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onSaveAsPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9DEA6E),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Save as PDF",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewInvoiceLineItem(
    description: String,
    unitPrice: String,
    quantity: String,
    total: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = description,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = unitPrice,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = quantity,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = total,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
