package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoicePreviewLineItem(
    val productService: ProductService,
    val quantity: Int,
    val lineTotal: Double
)

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator
) : ViewModel() {

    private val _invoice = MutableStateFlow<Invoice?>(null)
    val invoice: StateFlow<Invoice?> = _invoice.asStateFlow()

    private val _client = MutableStateFlow<Client?>(null)
    val client: StateFlow<Client?> = _client.asStateFlow()

    private val _lineItems = MutableStateFlow<List<InvoicePreviewLineItem>>(emptyList())
    val lineItems: StateFlow<List<InvoicePreviewLineItem>> = _lineItems.asStateFlow()

    private val _logoPath = MutableStateFlow<String?>(null)
    val logoPath: StateFlow<String?> = _logoPath.asStateFlow()

    private val _signaturePath = MutableStateFlow<String?>(null)
    val signaturePath: StateFlow<String?> = _signaturePath.asStateFlow()

    private val _businessInfo = MutableStateFlow<InvoicePdfGenerator.BusinessInfo?>(null)
    val businessInfo: StateFlow<InvoicePdfGenerator.BusinessInfo?> = _businessInfo.asStateFlow()

    private val _paymentInfo = MutableStateFlow<InvoicePdfGenerator.PaymentInfo?>(null)
    val paymentInfo: StateFlow<InvoicePdfGenerator.PaymentInfo?> = _paymentInfo.asStateFlow()

    fun loadInvoice(invoiceId: Int) {
        viewModelScope.launch {
            val loadedInvoice = invoiceRepository.getInvoiceById(invoiceId)
            if (loadedInvoice != null) {
                _invoice.value = loadedInvoice

                val client = clientRepository.getClientById(loadedInvoice.clientId)
                _client.value = client

                val items = invoiceRepository.getInvoiceItemsSync(invoiceId)
                _lineItems.value = items.map { item ->
                    productServiceRepository.getProductServiceById(item.productServiceId)?.let { ps ->
                        InvoicePreviewLineItem(
                            productService = ps,
                            quantity = item.quantity,
                            lineTotal = item.lineTotal
                        )
                    }
                }.filterNotNull()

                // Load logo path
                val logoPath = settingsRepository.logoPath.first()
                _logoPath.value = if (logoPath.isNotEmpty()) logoPath else null

                // Load signature path
                val signaturePath = settingsRepository.signaturePath.first()
                _signaturePath.value = if (signaturePath.isNotEmpty()) signaturePath else null

                // Load business info
                _businessInfo.value = InvoicePdfGenerator.BusinessInfo(
                    businessName = settingsRepository.businessName.first(),
                    ownerName = settingsRepository.ownerName.first(),
                    email = settingsRepository.businessEmail.first(),
                    phone = settingsRepository.businessPhone.first(),
                    website = settingsRepository.businessWebsite.first(),
                    address = settingsRepository.businessAddress.first(),
                    city = settingsRepository.businessCity.first(),
                    state = settingsRepository.businessState.first(),
                    zipCode = settingsRepository.businessZipCode.first(),
                    taxId = settingsRepository.businessTaxId.first()
                )

                // Load payment info
                _paymentInfo.value = InvoicePdfGenerator.PaymentInfo(
                    bankName = settingsRepository.bankName.first(),
                    accountHolderName = settingsRepository.accountHolderName.first(),
                    accountNumber = settingsRepository.accountNumber.first(),
                    routingNumber = settingsRepository.routingNumber.first(),
                    iban = settingsRepository.iban.first(),
                    swiftCode = settingsRepository.swiftCode.first(),
                    bankAddress = settingsRepository.bankAddress.first(),
                    paymentTerms = settingsRepository.paymentTerms.first(),
                    additionalInstructions = settingsRepository.additionalInstructions.first()
                )
            }
        }
    }

    fun downloadInvoicePdf(onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val invoice = _invoice.value
                val client = _client.value

                if (invoice == null || client == null) {
                    onError()
                    return@launch
                }

                // Get invoice items
                val items = invoiceRepository.getInvoiceItemsSync(invoice.id)

                // Get business info
                val businessInfo = InvoicePdfGenerator.BusinessInfo(
                    businessName = settingsRepository.businessName.first(),
                    ownerName = settingsRepository.ownerName.first(),
                    email = settingsRepository.businessEmail.first(),
                    phone = settingsRepository.businessPhone.first(),
                    website = settingsRepository.businessWebsite.first(),
                    address = settingsRepository.businessAddress.first(),
                    city = settingsRepository.businessCity.first(),
                    state = settingsRepository.businessState.first(),
                    zipCode = settingsRepository.businessZipCode.first(),
                    taxId = settingsRepository.businessTaxId.first()
                )

                // Get payment info
                val paymentInfo = InvoicePdfGenerator.PaymentInfo(
                    bankName = settingsRepository.bankName.first(),
                    accountHolderName = settingsRepository.accountHolderName.first(),
                    accountNumber = settingsRepository.accountNumber.first(),
                    routingNumber = settingsRepository.routingNumber.first(),
                    iban = settingsRepository.iban.first(),
                    swiftCode = settingsRepository.swiftCode.first(),
                    bankAddress = settingsRepository.bankAddress.first(),
                    paymentTerms = settingsRepository.paymentTerms.first(),
                    additionalInstructions = settingsRepository.additionalInstructions.first()
                )

                // Get currency
                val currency = settingsRepository.currency.first()

                // Get logo path
                val logoPath = settingsRepository.logoPath.first()

                // Get signature path
                val signaturePath = settingsRepository.signaturePath.first()

                // Generate PDF
                val file = pdfGenerator.generateInvoicePdf(
                    invoice = invoice,
                    client = client,
                    items = items,
                    businessInfo = businessInfo,
                    paymentInfo = paymentInfo,
                    currency = currency,
                    logoPath = if (logoPath.isNotEmpty()) logoPath else null,
                    signaturePath = if (signaturePath.isNotEmpty()) signaturePath else null
                )

                if (file != null) {
                    onSuccess(file.absolutePath)
                } else {
                    onError()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }
}
