package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod
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
class EditInvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _invoice = MutableStateFlow<Invoice?>(null)
    val invoice: StateFlow<Invoice?> = _invoice.asStateFlow()

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

    private val _status = MutableStateFlow(InvoiceStatus.DRAFT)
    val status: StateFlow<InvoiceStatus> = _status.asStateFlow()

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

    fun loadInvoice(invoiceId: Int) {
        if (loadedInvoiceId == invoiceId) return

        viewModelScope.launch {
            val loadedInvoice = invoiceRepository.getInvoiceById(invoiceId)
            if (loadedInvoice != null) {
                _invoice.value = loadedInvoice
                _invoiceNumber.value = loadedInvoice.number

                _dueDate.value = loadedInvoice.dueDate

                _notes.value = loadedInvoice.notes
                _currency.value = loadedInvoice.currency
                _status.value = loadedInvoice.status

                val client = clientRepository.getClientById(loadedInvoice.clientId)
                _selectedClient.value = client

                form.load(loadedInvoice, invoiceRepository.getInvoiceItemsSync(invoiceId))
                loadedInvoiceId = invoiceId
            }
        }
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

    fun setStatus(status: InvoiceStatus) {
        _status.value = status
    }

    /**
     * Atalho da edição: registra um pagamento do saldo da fatura gravada, com a data de hoje e
     * método "outro". Itens alterados e ainda não salvos não entram: ao salvar, o repositório
     * recalcula o status com o total novo.
     */
    fun markAsPaid() {
        viewModelScope.launch {
            val invoice = _invoice.value ?: return@launch
            val balance = InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)
            if (balance <= 0.0) return@launch
            invoiceRepository.addPayment(invoice.id, balance, InvoiceDates.today(), PaymentMethod.OTHER, "")
            analyticsManager.logPaymentRecorded(PaymentMethod.OTHER, isPartial = false)
            invoiceRepository.getInvoiceById(invoice.id)?.let {
                _invoice.value = it
                _status.value = it.status
            }
        }
    }

    fun setSelectedClient(client: Client?) {
        _selectedClient.value = client
    }

    fun updateInvoice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val originalInvoice = _invoice.value ?: return@launch
            val client = _selectedClient.value ?: return@launch

            val updatedInvoice = form.applyTo(
                originalInvoice.copy(
                    number = _invoiceNumber.value,
                    clientId = client.id,
                    dueDate = _dueDate.value,
                    notes = _notes.value,
                    status = _status.value,
                    currency = _currency.value
                )
            )

            val items = form.toInvoiceItems(invoiceId = originalInvoice.id)

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
