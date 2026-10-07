package com.example.superinvoice.ui.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsServicesViewModel @Inject constructor(
    private val productServiceRepository: ProductServiceRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    /**
     * Produto que o usuário tentou excluir mas está em uso em faturas.
     *
     * A FK `invoice_items.productServiceId` é `ON DELETE RESTRICT`: o banco recusa a
     * exclusão e lança `SQLiteConstraintException`, que antes escapava do `launch` e
     * derrubava o app. Até a Onda 1 trocar para `SET NULL` (docs/spec-onda-1.md), a
     * exclusão é bloqueada com uma explicação.
     */
    data class ProductInUse(val productService: ProductService, val invoiceCount: Int)

    private val _productInUse = MutableStateFlow<ProductInUse?>(null)
    val productInUse: StateFlow<ProductInUse?> = _productInUse.asStateFlow()

    val productsServices: StateFlow<List<ProductService>> =
        productServiceRepository.getAllProductsServices()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val currency: StateFlow<String> =
        settingsRepository.currency
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "USD"
            )

    fun addProductService(name: String, pricePerUnit: Double, description: String = "") {
        viewModelScope.launch {
            val productService = ProductService(
                name = name,
                description = description,
                pricePerUnit = pricePerUnit
            )
            productServiceRepository.insertProductService(productService)
            // Sem nome nem preço: catálogo é dado do negócio do usuário.
            analyticsManager.logProductCreated()
        }
    }

    fun updateProductService(productService: ProductService) {
        viewModelScope.launch {
            productServiceRepository.updateProductService(productService)
        }
    }

    fun deleteProductService(productService: ProductService) {
        viewModelScope.launch {
            val invoiceCount =
                invoiceRepository.getInvoiceCountForProductService(productService.id)
            if (invoiceCount > 0) {
                _productInUse.value = ProductInUse(productService, invoiceCount)
                return@launch
            }
            try {
                productServiceRepository.deleteProductService(productService)
            } catch (e: SQLiteConstraintException) {
                // Corrida rara: o produto entrou numa fatura entre a contagem e o delete.
                _productInUse.value = ProductInUse(
                    productService,
                    invoiceRepository.getInvoiceCountForProductService(productService.id)
                        .coerceAtLeast(1)
                )
            }
        }
    }

    fun dismissProductInUse() {
        _productInUse.value = null
    }
}
