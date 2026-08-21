package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.ui.components.BottomNavigationBar
import com.example.superinvoice.ui.components.InvEmptyState
import com.example.superinvoice.ui.components.InvFab
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvoiceCard
import com.example.superinvoice.ui.components.InvoiceFilterTabs
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun HomeScreen(
    onNavigateToCreateInvoice: () -> Unit = {},
    onNavigateToEditInvoice: (Int) -> Unit = {},
    onNavigateToPreview: (Int) -> Unit = {},
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

    InvScaffold(
        snackbarHostState = snackbarHostState,
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = onBottomNavItemSelected
            )
        },
        floatingActionButton = {
            InvFab(
                text = stringResource(R.string.add_invoice),
                icon = InvIcons.Plus,
                onClick = onNavigateToCreateInvoice
            )
        }
    ) {
        InvScreenHeader(
            title = stringResource(R.string.title_invoices),
            titleStyle = InvType.screenTitle
        )

        Column(modifier = Modifier.padding(horizontal = Space.screen)) {
            InvoiceFilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            if (!isPremium) {
                Text(
                    text = stringResource(
                        R.string.invoices_used,
                        invoiceCount,
                        BillingManager.FREE_INVOICE_LIMIT
                    ),
                    style = InvType.support,
                    color = if (invoiceCount >= BillingManager.FREE_INVOICE_LIMIT) Red else Neutral,
                    modifier = Modifier.padding(top = Space.lg)
                )
            }

            Spacer(modifier = Modifier.height(Space.section))

            Text(
                text = stringResource(R.string.invoice_record).uppercase(),
                style = InvType.label,
                color = Neutral
            )
            Text(
                text = stringResource(R.string.invoice_record_description),
                style = InvType.body,
                color = Neutral,
                modifier = Modifier.padding(top = Space.sm, bottom = Space.lg)
            )

            if (filteredInvoices.isEmpty()) {
                InvEmptyState(
                    title = stringResource(R.string.no_invoices_yet),
                    message = stringResource(R.string.no_invoices_message)
                )
            } else {
                InvSectionRule()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = Space.xxl)
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
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.error_sharing_pdf)
                                            )
                                        }
                                    }
                                )
                            },
                            onDownloadPdf = {
                                viewModel.downloadInvoicePdf(
                                    invoice = invoice,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(
                                                    R.string.pdf_saved_to_downloads,
                                                    invoice.number
                                                )
                                            )
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.error_generating_pdf)
                                            )
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
