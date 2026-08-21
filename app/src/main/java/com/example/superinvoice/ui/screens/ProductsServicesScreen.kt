package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.ui.components.ClientSearchBar
import com.example.superinvoice.ui.components.InvEmptyState
import com.example.superinvoice.ui.components.InvFab
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.ProductServiceCard
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.ProductsServicesViewModel
import online.isdevapps.superinvoice.R

@Composable
fun ProductsServicesScreen(
    onClose: () -> Unit,
    onNavigateToAddProductService: () -> Unit = {},
    onNavigateToEditProduct: ((Int) -> Unit)? = null,
    onProductSelected: ((ProductService) -> Unit)? = null,
    viewModel: ProductsServicesViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val productsServices by viewModel.productsServices.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    val filteredProducts = productsServices.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    val isSelectionMode = onProductSelected != null

    InvScaffold(
        floatingActionButton = {
            InvFab(
                text = stringResource(R.string.add_product_or_service_cd),
                icon = InvIcons.Plus,
                onClick = onNavigateToAddProductService
            )
        }
    ) {
        InvScreenHeader(
            title = stringResource(R.string.title_products_services),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )

        Column(modifier = Modifier.padding(horizontal = Space.screen)) {
            ClientSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_products_services)
            )

            Spacer(modifier = Modifier.height(Space.sm))

            if (filteredProducts.isEmpty()) {
                InvEmptyState(
                    title = if (searchQuery.isEmpty()) {
                        stringResource(R.string.no_products_yet)
                    } else {
                        stringResource(R.string.no_products_found)
                    },
                    message = if (searchQuery.isEmpty()) {
                        stringResource(R.string.no_products_message)
                    } else {
                        stringResource(R.string.no_products_search_message)
                    },
                    icon = InvIcons.Currency
                )
            } else {
                InvSectionRule()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = Space.xxl)
                ) {
                    items(filteredProducts) { product ->
                        ProductServiceCard(
                            productService = product,
                            currency = currency,
                            onClick = if (isSelectionMode) {
                                { onProductSelected?.invoke(product) }
                            } else {
                                null
                            },
                            onEdit = if (!isSelectionMode) {
                                { onNavigateToEditProduct?.invoke(product.id) }
                            } else {
                                null
                            },
                            onDelete = if (!isSelectionMode) {
                                { viewModel.deleteProductService(product) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
