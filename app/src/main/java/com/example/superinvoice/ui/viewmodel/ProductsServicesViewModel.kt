package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsServicesViewModel @Inject constructor(
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    fun addProductService(name: String, pricePerUnit: Double) {
        viewModelScope.launch {
            val productService = ProductService(
                name = name,
                pricePerUnit = pricePerUnit
            )
            productServiceRepository.insertProductService(productService)
        }
    }

    fun updateProductService(productService: ProductService) {
        viewModelScope.launch {
            productServiceRepository.updateProductService(productService)
        }
    }

    fun deleteProductService(productService: ProductService) {
        viewModelScope.launch {
            productServiceRepository.deleteProductService(productService)
        }
    }
}
