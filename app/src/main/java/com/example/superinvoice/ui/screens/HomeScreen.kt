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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.BottomNavigationBar
import com.example.superinvoice.ui.components.EmptyState
import com.example.superinvoice.ui.components.InvoiceCard
import com.example.superinvoice.ui.components.InvoiceFilterTabs
import com.example.superinvoice.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToCreateInvoice: () -> Unit = {},
    onNavigateToEditInvoice: (Int) -> Unit = {},
    onNavigateToAddClient: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {},
    selectedBottomNavItem: Int = 0,
    onBottomNavItemSelected: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val filteredInvoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFFFFFFF),
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
                    contentDescription = "Add Invoice"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Invoices",
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

            Text(
                text = "Invoice Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Here you will have access to all your invoices and be able to manage them in the best way.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            if (filteredInvoices.isEmpty()) {
                EmptyState(
                    title = "No invoices yet",
                    message = "Tap the + button to create your first invoice and start managing your business finances."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onClick = { onNavigateToEditInvoice(invoice.id) },
                            onEdit = { onNavigateToEditInvoice(invoice.id) },
                            onDelete = { viewModel.deleteInvoice(invoice) },
                            onDownloadPdf = {
                                viewModel.downloadInvoicePdf(
                                    invoice = invoice,
                                    onSuccess = { path ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("PDF saved to Downloads: Invoice_${invoice.number}.pdf")
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error generating PDF")
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
