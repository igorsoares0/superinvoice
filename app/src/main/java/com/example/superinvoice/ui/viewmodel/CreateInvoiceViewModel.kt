package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import com.example.superinvoice.util.InvoiceDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateInvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    /** Fatura ou orçamento: a mesma tela cria os dois (spec, seção 5.1). */
    private val _documentType = MutableStateFlow(DocumentType.INVOICE)
    val documentType: StateFlow<DocumentType> = _documentType.asStateFlow()

    private val _invoiceNumber = MutableStateFlow("")
    val invoiceNumber: StateFlow<String> = _invoiceNumber.asStateFlow()

    private val _dueDate = MutableStateFlow(InvoiceDates.today())
    val dueDate: StateFlow<Long> = _dueDate.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    /** Itens, desconto, imposto, frete e totais; a tela fala direto com ele. */
    val form = InvoiceForm()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _dateFormat = MutableStateFlow("MM/dd/yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    val clients: StateFlow<List<Client>> = clientRepository.getActiveClients()
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

    init {
        generateInvoiceNumber()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val currency = settingsRepository.currency.first()
            _currency.value = currency

            val dateFormatPattern = settingsRepository.dateFormat.first()
            _dateFormat.value = dateFormatPattern

            setCurrentDateAsDueDate()
        }
    }

    private fun generateInvoiceNumber() {
        viewModelScope.launch {
            // O init já pede um número de fatura; se a tela trocar para orçamento antes de ele
            // chegar, a resposta atrasada não pode sobrescrever o EST-.
            val type = _documentType.value
            val number = invoiceRepository.generateNextNumber(type)
            if (_documentType.value == type) _invoiceNumber.value = number
        }
    }

    private fun setCurrentDateAsDueDate() {
        _dueDate.value = InvoiceDates.today()
    }

    fun setInvoiceNumber(value: String) {
        _invoiceNumber.value = value
    }

    fun setDueDate(value: Long) {
        _dueDate.value = value
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun setCurrency(value: String) {
        _currency.value = value
    }

    fun setSelectedClient(client: Client?) {
        _selectedClient.value = client
    }

    fun saveInvoice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val client = _selectedClient.value ?: return@launch

            val invoice = form.applyTo(
                Invoice(
                    type = _documentType.value,
                    number = _invoiceNumber.value,
                    clientId = client.id,
                    dueDate = _dueDate.value,
                    notes = _notes.value,
                    status = InvoiceStatus.DRAFT,
                    currency = _currency.value
                )
            )

            val items = form.toInvoiceItems(invoiceId = 0)

            invoiceRepository.insertInvoiceWithItems(invoice, items)

            // Registrado antes do resetForm, que zera justamente os campos medidos.
            // Nenhum valor monetário e nenhum dado do cliente entram no evento — só a
            // forma da fatura, que é o que fecha o funil com invoice_started.
            if (invoice.type == DocumentType.ESTIMATE) {
                analyticsManager.logEstimateCreated(itemCount = items.size)
            } else {
                analyticsManager.logInvoiceSaved(
                    itemCount = items.size,
                    hasTax = invoice.taxAmount > 0.0,
                    hasDiscount = invoice.discountAmount > 0.0,
                    currency = invoice.currency,
                    discountType = invoice.discountType.takeIf { invoice.discountAmount > 0.0 },
                    taxType = invoice.taxType.takeIf { invoice.taxAmount > 0.0 },
                    hasShipping = invoice.shipping > 0.0,
                    hasDecimalQuantity = form.hasDecimalQuantity
                )
            }
            if (settingsRepository.totalInvoicesCreated.first() <= 1) {
                analyticsManager.logFirstInvoiceCompleted()
            }

            resetForm()
            onSuccess()
        }
    }

    /** Começa um documento novo do tipo pedido; o número vem da sequência desse tipo. */
    fun resetForm(type: DocumentType = _documentType.value) {
        _documentType.value = type
        _notes.value = ""
        _selectedClient.value = null
        form.reset()
        generateInvoiceNumber()
        loadSettings()
    }
}
