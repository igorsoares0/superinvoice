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

    val selectedTemplate: StateFlow<String> = settingsRepository.selectedTemplate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "classic"
        )

    private val _classicPreview = MutableStateFlow<Bitmap?>(null)
    val classicPreview: StateFlow<Bitmap?> = _classicPreview.asStateFlow()

    private val _modernPreview = MutableStateFlow<Bitmap?>(null)
    val modernPreview: StateFlow<Bitmap?> = _modernPreview.asStateFlow()

    private val _professionalPreview = MutableStateFlow<Bitmap?>(null)
    val professionalPreview: StateFlow<Bitmap?> = _professionalPreview.asStateFlow()

    init {
        generateTemplatePreviews()
    }

    private fun generateTemplatePreviews() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val mockInvoice = createMockInvoice()
                val mockClient = createMockClient()
                val mockItems = createMockItems()
                val mockBusinessInfo = createMockBusinessInfo()
                val mockPaymentInfo = createMockPaymentInfo()

                // Generate Classic preview
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
                    template = InvoiceTemplate.CLASSIC
                )
                _classicPreview.value = classicBitmap

                // Generate Modern preview
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
                    template = InvoiceTemplate.MODERN
                )
                _modernPreview.value = modernBitmap

                // Generate Professional preview
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
                    template = InvoiceTemplate.PROFESSIONAL
                )
                _professionalPreview.value = professionalBitmap
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
        android.util.Log.d("TemplateViewModel", "Saving template: $template")
        settingsRepository.saveSelectedTemplate(template)
        android.util.Log.d("TemplateViewModel", "Template saved successfully")
    }
}
