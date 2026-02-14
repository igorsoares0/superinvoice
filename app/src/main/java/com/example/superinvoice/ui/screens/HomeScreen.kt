package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.isdevapps.superinvoice.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.BottomNavigationBar
import com.example.superinvoice.ui.components.EmptyState
import com.example.superinvoice.ui.components.InvoiceCard
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.ui.components.InvoiceFilterTabs
import com.example.superinvoice.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToCreateInvoice: () -> Unit = {},
    onNavigateToEditInvoice: (Int) -> Unit = {},
    onNavigateToPreview: (Int) -> Unit = {},
    onNavigateToAddClient: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {},
    selectedBottomNavItem: Int = 0,
    onBottomNavItemSelected: (Int) -> Unit = {},
    isPremium: Boolean = false,
    invoiceCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val filteredInvoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF9FAFB),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = onBottomNavItemSelected,
                onAddClient = onNavigateToAddClient,
                onAddProduct = onNavigateToAddProduct
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateInvoice,
                containerColor = Color(0xFF9DEA6E),
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_invoice)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.title_invoices),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 32.dp)
            )

            InvoiceFilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 40.dp)
            )

            if (!isPremium) {
                Text(
                    text = stringResource(R.string.invoices_used, invoiceCount, BillingManager.FREE_INVOICE_LIMIT),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = if (invoiceCount >= BillingManager.FREE_INVOICE_LIMIT) Color.Red else Color.Gray,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                )
            }

            Text(
                text = stringResource(R.string.invoice_record),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.invoice_record_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            if (filteredInvoices.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_invoices_yet),
                    message = stringResource(R.string.no_invoices_message)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            dateFormatPattern = dateFormat,
                            onClick = { onNavigateToEditInvoice(invoice.id) },
                            onPreview = { onNavigateToPreview(invoice.id) },
                            onEdit = { onNavigateToEditInvoice(invoice.id) },
                            onShare = {
                                viewModel.shareInvoicePdf(
                                    invoice = invoice,
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.error_sharing_pdf))
                                        }
                                    }
                                )
                            },
                            onDownloadPdf = {
                                viewModel.downloadInvoicePdf(
                                    invoice = invoice,
                                    onSuccess = { path ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.pdf_saved_to_downloads, invoice.number))
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.error_generating_pdf))
                                        }
                                    }
                                )
                            },
                            onDelete = { viewModel.deleteInvoice(invoice) }
                        )
                    }
                }
            }
        }
    }
}
