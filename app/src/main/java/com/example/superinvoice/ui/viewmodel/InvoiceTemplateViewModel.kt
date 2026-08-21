package com.example.superinvoice.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.pdf.InvoiceTemplate
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InvoiceTemplateViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator
) : ViewModel() {

    companion object {
        // Cache para armazenar os previews gerados
        private var cachedClassicPreview: Bitmap? = null
        private var cachedModernPreview: Bitmap? = null
        private var cachedProfessionalPreview: Bitmap? = null

        /**
         * O cache vive no companion, então sobrevive à destruição do
         * ViewModel — sem isto, mudar a cor ou a fonte da fatura deixaria
         * os três previews desta tela mostrando a aparência antiga.
         */
        fun invalidatePreviewCache() {
            cachedClassicPreview = null
            cachedModernPreview = null
            cachedProfessionalPreview = null
        }
    }

    val selectedTemplate: StateFlow<String> = settingsRepository.selectedTemplate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "classic"
        )

    private val _classicPreview = MutableStateFlow<Bitmap?>(cachedClassicPreview)
    val classicPreview: StateFlow<Bitmap?> = _classicPreview.asStateFlow()

    private val _modernPreview = MutableStateFlow<Bitmap?>(cachedModernPreview)
    val modernPreview: StateFlow<Bitmap?> = _modernPreview.asStateFlow()

    private val _professionalPreview = MutableStateFlow<Bitmap?>(cachedProfessionalPreview)
    val professionalPreview: StateFlow<Bitmap?> = _professionalPreview.asStateFlow()

    init {
        // Só gera previews se ainda não existem no cache
        if (cachedClassicPreview == null || cachedModernPreview == null || cachedProfessionalPreview == null) {
            generateTemplatePreviews()
        }
    }

    private fun generateTemplatePreviews() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val mockInvoice = createMockInvoice()
                val mockClient = createMockClient()
                val mockItems = createMockItems()
                val mockBusinessInfo = createMockBusinessInfo()
                val mockPaymentInfo = createMockPaymentInfo()

                // Generate previews with lower resolution (2x) for faster loading
                val previewScale = 2
                val style = settingsRepository.invoiceStyle()

                // Generate Classic preview (se ainda não existe)
                if (cachedClassicPreview == null) {
                    val classicBitmap = pdfGenerator.generateInvoicePreviewBitmap(
                        invoice = mockInvoice,
                        client = mockClient,
                        items = mockItems,
                        businessInfo = mockBusinessInfo,
                        paymentInfo = mockPaymentInfo,
                        currency = "USD",
                        dateFormat = "MM/dd/yyyy",
                        logoPath = null,
                        signaturePath = null,
                        paymentQrCodePath = null,
                        template = InvoiceTemplate.CLASSIC,
                        scale = previewScale,
                        style = style
                    )
                    cachedClassicPreview = classicBitmap
                    _classicPreview.value = classicBitmap
                }

                // Generate Modern preview (se ainda não existe)
                if (cachedModernPreview == null) {
                    val modernBitmap = pdfGenerator.generateInvoicePreviewBitmap(
                        invoice = mockInvoice,
                        client = mockClient,
                        items = mockItems,
                        businessInfo = mockBusinessInfo,
                        paymentInfo = mockPaymentInfo,
                        currency = "USD",
                        dateFormat = "MM/dd/yyyy",
                        logoPath = null,
                        signaturePath = null,
                        paymentQrCodePath = null,
                        template = InvoiceTemplate.MODERN,
                        scale = previewScale,
                        style = style
                    )
                    cachedModernPreview = modernBitmap
                    _modernPreview.value = modernBitmap
                }

                // Generate Professional preview (se ainda não existe)
                if (cachedProfessionalPreview == null) {
                    val professionalBitmap = pdfGenerator.generateInvoicePreviewBitmap(
                        invoice = mockInvoice,
                        client = mockClient,
                        items = mockItems,
                        businessInfo = mockBusinessInfo,
                        paymentInfo = mockPaymentInfo,
                        currency = "USD",
                        dateFormat = "MM/dd/yyyy",
                        logoPath = null,
                        signaturePath = null,
                        paymentQrCodePath = null,
                        template = InvoiceTemplate.PROFESSIONAL,
                        scale = previewScale,
                        style = style
                    )
                    cachedProfessionalPreview = professionalBitmap
                    _professionalPreview.value = professionalBitmap
                }
            }
        }
    }

    private fun createMockInvoice() = Invoice(
        id = 1,
        number = "001",
        clientId = 1,
        createdDate = System.currentTimeMillis(),
        dueDate = "12/31/2024",
        subtotal = 1000.0,
        tax = 100.0,
        totalAmount = 1100.0,
        status = InvoiceStatus.DRAFT,
        currency = "USD"
    )

    private fun createMockClient() = Client(
        id = 1,
        name = "John Doe",
        email = "john@example.com",
        phone = "+1 234 567 8900"
    )

    private fun createMockItems() = listOf(
        InvoiceItem(
            id = 1,
            invoiceId = 1,
            productServiceId = 1,
            productServiceName = "Design Services",
            productServiceDescription = "Professional design and branding",
            pricePerUnit = 500.0,
            quantity = 2,
            lineTotal = 1000.0
        )
    )

    private fun createMockBusinessInfo() = InvoicePdfGenerator.BusinessInfo(
        businessName = "Your Business",
        ownerName = "Owner Name",
        email = "business@example.com",
        phone = "+1 234 567 8900",
        website = "www.business.com",
        address = "123 Business St",
        city = "City",
        state = "State",
        zipCode = "12345",
        taxId = "123456789"
    )

    private fun createMockPaymentInfo() = InvoicePdfGenerator.PaymentInfo(
        bankName = "Bank Name",
        accountHolderName = "Account Holder",
        accountNumber = "****1234",
        routingNumber = "123456789",
        iban = "GB00XXXX00001234567890",
        swiftCode = "SWIFT123",
        bankAddress = "Bank Address",
        paymentTerms = "Net 30",
        additionalInstructions = "Payment instructions"
    )

    suspend fun saveSelectedTemplate(template: String) {
        settingsRepository.saveSelectedTemplate(template)
    }
}
