package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LineItem(
    val productService: ProductService,
    val quantity: Int,
    val lineTotal: Double
)

@HiltViewModel
class CreateInvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _invoiceNumber = MutableStateFlow("")
    val invoiceNumber: StateFlow<String> = _invoiceNumber.asStateFlow()

    private val _dueDate = MutableStateFlow("")
    val dueDate: StateFlow<String> = _dueDate.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _tax = MutableStateFlow("")
    val tax: StateFlow<String> = _tax.asStateFlow()

    private val _discount = MutableStateFlow("")
    val discount: StateFlow<String> = _discount.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    private val _lineItems = MutableStateFlow<List<LineItem>>(emptyList())
    val lineItems: StateFlow<List<LineItem>> = _lineItems.asStateFlow()

    val clients: StateFlow<List<Client>> = clientRepository.getAllClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productsServices: StateFlow<List<ProductService>> =
        productServiceRepository.getAllProductsServices()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    init {
        generateInvoiceNumber()
        setCurrentDateAsDueDate()
        loadCurrency()
    }

    private fun loadCurrency() {
        viewModelScope.launch {
            val currency = settingsRepository.currency.first()
            _currency.value = currency
        }
    }

    private fun generateInvoiceNumber() {
        viewModelScope.launch {
            val number = invoiceRepository.generateNextInvoiceNumber()
            _invoiceNumber.value = number
        }
    }

    private fun setCurrentDateAsDueDate() {
        val dateFormat = SimpleDateFormat("MMM, dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date()).lowercase()
        _dueDate.value = currentDate
    }

    fun setInvoiceNumber(value: String) {
        _invoiceNumber.value = value
    }

    fun setDueDate(value: String) {
        _dueDate.value = value
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun setTax(value: String) {
        _tax.value = value
        calculateTotal()
    }

    fun setDiscount(value: String) {
        _discount.value = value
        calculateTotal()
    }

    fun setCurrency(value: String) {
        _currency.value = value
    }

    fun setSelectedClient(client: Client?) {
        _selectedClient.value = client
    }

    fun addLineItem(productService: ProductService, quantity: Int) {
        val lineTotal = productService.pricePerUnit * quantity
        val lineItem = LineItem(
            productService = productService,
            quantity = quantity,
            lineTotal = lineTotal
        )
        _lineItems.update { it + lineItem }
        calculateSubtotal()
    }

    fun updateLineItemQuantity(index: Int, quantity: Int) {
        _lineItems.update { items ->
            items.mapIndexed { i, item ->
                if (i == index) {
                    item.copy(
                        quantity = quantity,
                        lineTotal = item.productService.pricePerUnit * quantity
                    )
                } else {
                    item
                }
            }
        }
        calculateSubtotal()
    }

    fun removeLineItem(index: Int) {
        _lineItems.update { items ->
            items.filterIndexed { i, _ -> i != index }
        }
        calculateSubtotal()
    }

    private fun calculateSubtotal() {
        _subtotal.value = _lineItems.value.sumOf { it.lineTotal }
        calculateTotal()
    }

    private fun calculateTotal() {
        val taxValue = _tax.value.toDoubleOrNull() ?: 0.0
        val discountValue = _discount.value.toDoubleOrNull() ?: 0.0
        _totalAmount.value = _subtotal.value + taxValue - discountValue
    }

    fun saveInvoice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val client = _selectedClient.value ?: return@launch

            val invoice = Invoice(
                number = _invoiceNumber.value,
                clientId = client.id,
                dueDate = _dueDate.value,
                notes = _notes.value,
                tax = _tax.value.toDoubleOrNull() ?: 0.0,
                discount = _discount.value.toDoubleOrNull() ?: 0.0,
                subtotal = _subtotal.value,
                totalAmount = _totalAmount.value,
                status = InvoiceStatus.DRAFT,
                currency = _currency.value
            )

            val items = _lineItems.value.map { lineItem ->
                InvoiceItem(
                    invoiceId = 0,
                    productServiceId = lineItem.productService.id,
                    productServiceName = lineItem.productService.name,
                    pricePerUnit = lineItem.productService.pricePerUnit,
                    quantity = lineItem.quantity,
                    lineTotal = lineItem.lineTotal
                )
            }

            invoiceRepository.insertInvoiceWithItems(invoice, items)
            resetForm()
            onSuccess()
        }
    }

    fun resetForm() {
        _notes.value = ""
        _tax.value = ""
        _discount.value = ""
        _selectedClient.value = null
        _lineItems.value = emptyList()
        _subtotal.value = 0.0
        _totalAmount.value = 0.0
        generateInvoiceNumber()
        setCurrentDateAsDueDate()
        loadCurrency()
    }
}
