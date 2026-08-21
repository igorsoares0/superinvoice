package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.pdf.InvoiceAccent
import com.example.superinvoice.data.pdf.InvoiceFontChoice
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.pdf.InvoiceStyle
import com.example.superinvoice.data.pdf.invoiceTemplateOf
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InvoiceStyleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator
) : ViewModel() {

    private val _accent = MutableStateFlow(InvoiceAccent.Default)
    val accent: StateFlow<InvoiceAccent> = _accent.asStateFlow()

    private val _font = MutableStateFlow(InvoiceFontChoice.Default)
    val font: StateFlow<InvoiceFontChoice> = _font.asStateFlow()

    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()

    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            _accent.value = settingsRepository.invoiceAccent.first()
            _font.value = settingsRepository.invoiceFont.first()
            refreshPreview()
        }
    }

    fun setAccent(accent: InvoiceAccent) {
        if (_accent.value == accent) return
        _accent.value = accent
        refreshPreview()
    }

    fun setFont(font: InvoiceFontChoice) {
        if (_font.value == font) return
        _font.value = font
        refreshPreview()
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.saveInvoiceStyle(_accent.value, _font.value)
            InvoiceTemplateViewModel.invalidatePreviewCache()
            onSaved()
        }
    }

    /**
     * Regera o preview com um respiro, para tocar em várias amostras
     * seguidas não enfileirar renderizações.
     */
    private fun refreshPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            val style = InvoiceStyle.of(_accent.value, _font.value.fonts(context))
            val templateId = settingsRepository.selectedTemplate.first()
            val currency = settingsRepository.currency.first()
            val dateFormat = settingsRepository.dateFormat.first()

            val bitmap = withContext(Dispatchers.IO) {
                pdfGenerator.generateInvoicePreviewBitmap(
                    invoice = SAMPLE_INVOICE,
                    client = SAMPLE_CLIENT,
                    items = SAMPLE_ITEMS,
                    businessInfo = sampleBusinessInfo(),
                    paymentInfo = samplePaymentInfo(),
                    currency = currency,
                    dateFormat = dateFormat,
                    logoPath = null,
                    signaturePath = null,
                    paymentQrCodePath = null,
                    template = invoiceTemplateOf(templateId),
                    // O padrão é 5, o que dá um bitmap de ~50MB. Aqui a
                    // imagem é pequena na tela e regenera a cada toque.
                    scale = PREVIEW_SCALE,
                    isPremium = true,
                    style = style
                )
            }
            // Sem `recycle()` no anterior de propósito: a Image ainda pode
            // estar desenhando com ele quando a troca acontece, e desenhar
            // um bitmap reciclado derruba o app. A ~2MB por troca, o GC dá
            // conta.
            _preview.value = bitmap
        }
    }

    private fun sampleBusinessInfo() = InvoicePdfGenerator.BusinessInfo(
        businessName = "Studio",
        ownerName = "",
        email = "hello@studio.com",
        phone = "",
        website = "studio.com",
        address = "12 Main Street",
        city = "Lisbon",
        state = "",
        zipCode = "1000-001",
        taxId = ""
    )

    private fun samplePaymentInfo() = InvoicePdfGenerator.PaymentInfo(
        bankName = "Bank",
        accountHolderName = "Studio",
        accountNumber = "0001",
        paymentTerms = "Net 15"
    )

    companion object {
        private const val PREVIEW_DEBOUNCE_MS = 150L
        private const val PREVIEW_SCALE = 1

        private val SAMPLE_INVOICE = Invoice(
            id = 1,
            number = "001",
            clientId = 1,
            createdDate = System.currentTimeMillis(),
            dueDate = "12/31/2025",
            subtotal = 1200.0,
            tax = 120.0,
            totalAmount = 1320.0,
            status = InvoiceStatus.DRAFT,
            currency = "USD"
        )

        private val SAMPLE_CLIENT = Client(
            id = 1,
            name = "Acme Studio",
            email = "hello@acme.com",
            phone = "",
            address = "40 Park Avenue",
            city = "Porto",
            zipCode = "4000-001"
        )

        private val SAMPLE_ITEMS = listOf(
            InvoiceItem(
                id = 1,
                invoiceId = 1,
                productServiceId = 1,
                productServiceName = "Design work",
                pricePerUnit = 600.0,
                quantity = 2,
                lineTotal = 1200.0
            )
        )
    }
}
