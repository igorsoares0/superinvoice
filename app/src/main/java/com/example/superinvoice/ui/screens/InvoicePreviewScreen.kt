package com.example.superinvoice.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.superinvoice.ui.viewmodel.InvoicePreviewViewModel
import com.example.superinvoice.util.getCurrencySymbol
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicePreviewScreen(
    invoiceId: Int,
    previewVersion: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSaveAsPdf: () -> Unit,
    viewModel: InvoicePreviewViewModel = hiltViewModel()
) {
    LaunchedEffect(invoiceId, previewVersion) {
        viewModel.loadInvoice(invoiceId)
    }

    val invoice by viewModel.invoice.collectAsStateWithLifecycle()
    val client by viewModel.client.collectAsStateWithLifecycle()
    val lineItems by viewModel.lineItems.collectAsStateWithLifecycle()
    val logoPath by viewModel.logoPath.collectAsStateWithLifecycle()
    val signaturePath by viewModel.signaturePath.collectAsStateWithLifecycle()
    val businessInfo by viewModel.businessInfo.collectAsStateWithLifecycle()
    val paymentInfo by viewModel.paymentInfo.collectAsStateWithLifecycle()

    val dateFormat = SimpleDateFormat("MM.dd.yyyy", Locale.getDefault())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currencySymbol = getCurrencySymbol(invoice?.currency ?: "USD")

    Scaffold(
        containerColor = Color(0xFFFFFFFF),
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        // Logo (if exists)
                        logoPath?.let { path ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(path)),
                                    contentDescription = "Business Logo",
                                    modifier = Modifier.height(40.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

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

                        // FROM (Business Information)
                        if (businessInfo?.businessName?.isNotEmpty() == true) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "FROM:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = businessInfo?.businessName ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                if (businessInfo?.ownerName?.isNotEmpty() == true) {
                                    Text(
                                        text = businessInfo?.ownerName ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (businessInfo?.email?.isNotEmpty() == true) {
                                    Text(
                                        text = businessInfo?.email ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (businessInfo?.phone?.isNotEmpty() == true) {
                                    Text(
                                        text = businessInfo?.phone ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (businessInfo?.website?.isNotEmpty() == true) {
                                    Text(
                                        text = businessInfo?.website ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (businessInfo?.address?.isNotEmpty() == true) {
                                    Text(
                                        text = businessInfo?.address ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                val cityStateZip = buildString {
                                    if (businessInfo?.city?.isNotEmpty() == true) append(businessInfo?.city)
                                    if (businessInfo?.state?.isNotEmpty() == true) {
                                        if (isNotEmpty()) append(", ")
                                        append(businessInfo?.state)
                                    }
                                    if (businessInfo?.zipCode?.isNotEmpty() == true) {
                                        if (isNotEmpty()) append(" ")
                                        append(businessInfo?.zipCode)
                                    }
                                }
                                if (cityStateZip.isNotEmpty()) {
                                    Text(
                                        text = cityStateZip,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (businessInfo?.taxId?.isNotEmpty() == true) {
                                    Text(
                                        text = "Tax ID: ${businessInfo?.taxId}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

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
                                    text = client?.name ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                Text(
                                    text = client?.email ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                Text(
                                    text = client?.phone ?: "",
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
                                        text = invoice?.number ?: "",
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
                                        text = invoice?.let { dateFormat.format(Date(it.createdDate)) } ?: "",
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
                                        text = invoice?.dueDate ?: "",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pay To Section
                        if (paymentInfo?.bankName?.isNotEmpty() == true) {
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
                                    text = paymentInfo?.bankName ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                if (paymentInfo?.accountHolderName?.isNotEmpty() == true) {
                                    Text(
                                        text = "Account Name: ${paymentInfo?.accountHolderName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                                if (paymentInfo?.accountNumber?.isNotEmpty() == true) {
                                    Text(
                                        text = "Account Number: ${paymentInfo?.accountNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }
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
                        lineItems.forEach { item ->
                            PreviewInvoiceLineItem(
                                description = item.productService.name,
                                unitPrice = String.format("%.2f", item.productService.pricePerUnit),
                                quantity = item.quantity.toString(),
                                total = "$currencySymbol${String.format("%.2f", item.lineTotal)}",
                                currencySymbol = currencySymbol
                            )
                        }

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
                                    text = "$currencySymbol${String.format("%.2f", invoice?.subtotal ?: 0.0)}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                            if ((invoice?.tax ?: 0.0) > 0) {
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
                                        text = "$currencySymbol${String.format("%.2f", invoice?.tax ?: 0.0)}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }
                            if ((invoice?.discount ?: 0.0) > 0) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "DISCOUNT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Text(
                                        text = "-$currencySymbol${String.format("%.2f", invoice?.discount ?: 0.0)}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
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
                                    text = "$currencySymbol${String.format("%.2f", invoice?.totalAmount ?: 0.0)}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Signature (if exists)
                        signaturePath?.let { path ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(path)),
                                    contentDescription = "Signature",
                                    modifier = Modifier.height(30.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
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
                    onClick = {
                        viewModel.downloadInvoicePdf(
                            onSuccess = { path ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("PDF saved to: $path")
                                }
                            },
                            onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to save PDF")
                                }
                            }
                        )
                    },
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
    total: String,
    currencySymbol: String
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
            text = "$currencySymbol$unitPrice",
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
