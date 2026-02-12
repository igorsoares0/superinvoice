package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import com.example.superinvoice.ui.components.InvoiceFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(InvoiceFilter.PAID)
    val selectedFilter: StateFlow<InvoiceFilter> = _selectedFilter.asStateFlow()

    val dateFormat: StateFlow<String> = settingsRepository.dateFormat
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "MM/dd/yyyy"
        )

    private val allInvoices = invoiceRepository.getAllInvoices()

    val filteredInvoices: StateFlow<List<Invoice>> = combine(
        allInvoices,
        selectedFilter
    ) { invoices, filter ->
        when (filter) {
            InvoiceFilter.ALL -> invoices
            InvoiceFilter.PAID -> invoices.filter { it.status == InvoiceStatus.PAID }
            InvoiceFilter.UNPAID -> invoices.filter { it.status != InvoiceStatus.PAID }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: InvoiceFilter) {
        _selectedFilter.value = filter
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoice)
        }
    }

    fun downloadInvoicePdf(invoice: Invoice, onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                // Get client
                val client = clientRepository.getClientById(invoice.clientId)
                if (client == null) {
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

                // Get date format
                val dateFormat = settingsRepository.dateFormat.first()

                // Get logo path
                val logoPath = settingsRepository.logoPath.first()

                // Get signature path
                val signaturePath = settingsRepository.signaturePath.first()

                // Get payment QR code path
                val paymentQrCodePath = settingsRepository.paymentQrCodePath.first()

                // Get selected template
                val selectedTemplate = settingsRepository.selectedTemplate.first()
                android.util.Log.d("HomeViewModel", "Selected template from settings: $selectedTemplate")
                val template = when (selectedTemplate) {
                    "modern" -> com.example.superinvoice.data.pdf.InvoiceTemplate.MODERN
                    "professional" -> com.example.superinvoice.data.pdf.InvoiceTemplate.PROFESSIONAL
                    else -> com.example.superinvoice.data.pdf.InvoiceTemplate.CLASSIC
                }
                android.util.Log.d("HomeViewModel", "Template enum: $template")

                // Generate PDF
                val file = pdfGenerator.generateInvoicePdf(
                    invoice = invoice,
                    client = client,
                    items = items,
                    businessInfo = businessInfo,
                    paymentInfo = paymentInfo,
                    currency = currency,
                    dateFormat = dateFormat,
                    logoPath = if (logoPath.isNotEmpty()) logoPath else null,
                    signaturePath = if (signaturePath.isNotEmpty()) signaturePath else null,
                    paymentQrCodePath = if (paymentQrCodePath.isNotEmpty()) paymentQrCodePath else null,
                    template = template,
                    isPremium = billingManager.isPremium.value
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

    fun shareInvoicePdf(invoice: Invoice, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                // Get client
                val client = clientRepository.getClientById(invoice.clientId)
                if (client == null) {
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

                // Get date format
                val dateFormat = settingsRepository.dateFormat.first()

                // Get logo path
                val logoPath = settingsRepository.logoPath.first()

                // Get signature path
                val signaturePath = settingsRepository.signaturePath.first()

                // Get payment QR code path
                val paymentQrCodePath = settingsRepository.paymentQrCodePath.first()

                // Get selected template
                val selectedTemplate = settingsRepository.selectedTemplate.first()
                android.util.Log.d("HomeViewModel", "Selected template from settings: $selectedTemplate")
                val template = when (selectedTemplate) {
                    "modern" -> com.example.superinvoice.data.pdf.InvoiceTemplate.MODERN
                    "professional" -> com.example.superinvoice.data.pdf.InvoiceTemplate.PROFESSIONAL
                    else -> com.example.superinvoice.data.pdf.InvoiceTemplate.CLASSIC
                }
                android.util.Log.d("HomeViewModel", "Template enum: $template")

                // Generate PDF
                val file = pdfGenerator.generateInvoicePdf(
                    invoice = invoice,
                    client = client,
                    items = items,
                    businessInfo = businessInfo,
                    paymentInfo = paymentInfo,
                    currency = currency,
                    dateFormat = dateFormat,
                    logoPath = if (logoPath.isNotEmpty()) logoPath else null,
                    signaturePath = if (signaturePath.isNotEmpty()) signaturePath else null,
                    paymentQrCodePath = if (paymentQrCodePath.isNotEmpty()) paymentQrCodePath else null,
                    template = template,
                    isPremium = billingManager.isPremium.value
                )

                if (file != null) {
                    // Share the PDF using FileProvider
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.number}")
                        putExtra(Intent.EXTRA_TEXT, "Please find attached Invoice ${invoice.number}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooser = Intent.createChooser(shareIntent, "Share Invoice")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
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
