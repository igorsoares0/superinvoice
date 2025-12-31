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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditInvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _invoice = MutableStateFlow<Invoice?>(null)
    val invoice: StateFlow<Invoice?> = _invoice.asStateFlow()

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

    private val _dateFormat = MutableStateFlow("MM/dd/yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _status = MutableStateFlow(InvoiceStatus.DRAFT)
    val status: StateFlow<InvoiceStatus> = _status.asStateFlow()

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

    private var loadedInvoiceId: Int? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val currency = settingsRepository.currency.first()
            _currency.value = currency

            val dateFormatPattern = settingsRepository.dateFormat.first()
            _dateFormat.value = dateFormatPattern
        }
    }

    private fun reformatDateIfNeeded(dateString: String): String {
        if (dateString.isEmpty()) return dateString

        // Lista de formatos conhecidos para tentar parsear
        val knownFormats = listOf(
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd-MM-yyyy",
            "MMMM dd, yyyy",
            "dd MMMM yyyy",
            "MMM dd, yyyy",
            "MMM, dd"  // Formato antigo
        )

        // Tentar parsear com cada formato conhecido
        for (format in knownFormats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                parser.isLenient = false
                val date = parser.parse(dateString)

                // Se conseguiu parsear, reformatar com o formato atual
                if (date != null) {
                    val formatter = SimpleDateFormat(_dateFormat.value, Locale.getDefault())
                    return formatter.format(date)
                }
            } catch (e: Exception) {
                // Continuar tentando outros formatos
                continue
            }
        }

        // Se não conseguiu parsear com nenhum formato, retornar a string original
        return dateString
    }

    fun loadInvoice(invoiceId: Int) {
        if (loadedInvoiceId == invoiceId) return

        viewModelScope.launch {
            val loadedInvoice = invoiceRepository.getInvoiceById(invoiceId)
            if (loadedInvoice != null) {
                _invoice.value = loadedInvoice
                _invoiceNumber.value = loadedInvoice.number

                // Reformatar due date para o formato atual
                _dueDate.value = reformatDateIfNeeded(loadedInvoice.dueDate)

                _notes.value = loadedInvoice.notes
                _tax.value = loadedInvoice.tax.toString()
                _discount.value = loadedInvoice.discount.toString()
                _currency.value = loadedInvoice.currency
                _status.value = loadedInvoice.status

                val client = clientRepository.getClientById(loadedInvoice.clientId)
                _selectedClient.value = client

                val items = invoiceRepository.getInvoiceItemsSync(invoiceId)
                _lineItems.value = items.map { item ->
                    productServiceRepository.getProductServiceById(item.productServiceId)?.let { ps ->
                        LineItem(
                            productService = ps,
                            quantity = item.quantity,
                            lineTotal = item.lineTotal
                        )
                    }
                }.filterNotNull()

                calculateSubtotal()
                loadedInvoiceId = invoiceId
            }
        }
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

    fun setStatus(status: InvoiceStatus) {
        _status.value = status
    }

    fun togglePaidStatus() {
        val newStatus = if (_status.value == InvoiceStatus.PAID) {
            InvoiceStatus.DRAFT
        } else {
            InvoiceStatus.PAID
        }
        _status.value = newStatus

        // Also update the invoice object
        _invoice.value?.let { currentInvoice ->
            _invoice.value = currentInvoice.copy(status = newStatus)
        }
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

    fun updateInvoice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val originalInvoice = _invoice.value ?: return@launch
            val client = _selectedClient.value ?: return@launch

            val updatedInvoice = originalInvoice.copy(
                number = _invoiceNumber.value,
                clientId = client.id,
                dueDate = _dueDate.value,
                notes = _notes.value,
                tax = _tax.value.toDoubleOrNull() ?: 0.0,
                discount = _discount.value.toDoubleOrNull() ?: 0.0,
                subtotal = _subtotal.value,
                totalAmount = _totalAmount.value,
                status = _status.value,
                currency = _currency.value
            )

            val items = _lineItems.value.map { lineItem ->
                InvoiceItem(
                    invoiceId = originalInvoice.id,
                    productServiceId = lineItem.productService.id,
                    productServiceName = lineItem.productService.name,
                    productServiceDescription = lineItem.productService.description,
                    pricePerUnit = lineItem.productService.pricePerUnit,
                    quantity = lineItem.quantity,
                    lineTotal = lineItem.lineTotal
                )
            }

            invoiceRepository.updateInvoiceWithItems(updatedInvoice, items)
            onSuccess()
        }
    }

    fun deleteInvoice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val invoice = _invoice.value ?: return@launch
            invoiceRepository.deleteInvoice(invoice)
            onSuccess()
        }
    }
}
