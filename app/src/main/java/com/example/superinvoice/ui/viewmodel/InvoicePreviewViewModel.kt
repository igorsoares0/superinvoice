package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.ProductService
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.ProductServiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class InvoicePreviewLineItem(
    val productService: ProductService,
    val quantity: Int,
    val lineTotal: Double
)

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator,
    private val billingManager: BillingManager
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

    private val _paymentQrCodePath = MutableStateFlow<String?>(null)
    val paymentQrCodePath: StateFlow<String?> = _paymentQrCodePath.asStateFlow()

    private val _businessInfo = MutableStateFlow<InvoicePdfGenerator.BusinessInfo?>(null)
    val businessInfo: StateFlow<InvoicePdfGenerator.BusinessInfo?> = _businessInfo.asStateFlow()

    private val _paymentInfo = MutableStateFlow<InvoicePdfGenerator.PaymentInfo?>(null)
    val paymentInfo: StateFlow<InvoicePdfGenerator.PaymentInfo?> = _paymentInfo.asStateFlow()

    private val _dateFormat = MutableStateFlow("MM/dd/yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _formattedDueDate = MutableStateFlow("")
    val formattedDueDate: StateFlow<String> = _formattedDueDate.asStateFlow()

    private val _selectedTemplate = MutableStateFlow("classic")
    val selectedTemplate: StateFlow<String> = _selectedTemplate.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

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

                // Load payment QR code path
                val paymentQrCodePath = settingsRepository.paymentQrCodePath.first()
                _paymentQrCodePath.value = if (paymentQrCodePath.isNotEmpty()) paymentQrCodePath else null

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

                // Load date format
                _dateFormat.value = settingsRepository.dateFormat.first()

                // Load selected template
                _selectedTemplate.value = settingsRepository.selectedTemplate.first()

                // Reformatar due date para o formato atual
                _formattedDueDate.value = reformatDateIfNeeded(loadedInvoice.dueDate)

                // Generate preview bitmap in background
                if (client != null && _businessInfo.value != null && _paymentInfo.value != null) {
                    withContext(Dispatchers.IO) {
                        val template = when (_selectedTemplate.value) {
                            "modern" -> com.example.superinvoice.data.pdf.InvoiceTemplate.MODERN
                            "professional" -> com.example.superinvoice.data.pdf.InvoiceTemplate.PROFESSIONAL
                            else -> com.example.superinvoice.data.pdf.InvoiceTemplate.CLASSIC
                        }

                        val bitmap = pdfGenerator.generateInvoicePreviewBitmap(
                            invoice = loadedInvoice,
                            client = client,
                            items = items,
                            businessInfo = _businessInfo.value!!,
                            paymentInfo = _paymentInfo.value!!,
                            currency = loadedInvoice.currency,
                            dateFormat = _dateFormat.value,
                            logoPath = _logoPath.value,
                            signaturePath = _signaturePath.value,
                            paymentQrCodePath = _paymentQrCodePath.value,
                            template = template,
                            isPremium = billingManager.isPremium.value
                        )

                        withContext(Dispatchers.Main) {
                            _previewBitmap.value = bitmap
                        }
                    }
                }
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

                val template = when (selectedTemplate) {
                    "modern" -> com.example.superinvoice.data.pdf.InvoiceTemplate.MODERN
                    "professional" -> com.example.superinvoice.data.pdf.InvoiceTemplate.PROFESSIONAL
                    else -> com.example.superinvoice.data.pdf.InvoiceTemplate.CLASSIC
                }


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
                // Error handled silently
                onError()
            }
        }
    }

    fun shareInvoicePdf(onError: () -> Unit) {
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

                val template = when (selectedTemplate) {
                    "modern" -> com.example.superinvoice.data.pdf.InvoiceTemplate.MODERN
                    "professional" -> com.example.superinvoice.data.pdf.InvoiceTemplate.PROFESSIONAL
                    else -> com.example.superinvoice.data.pdf.InvoiceTemplate.CLASSIC
                }


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
                // Error handled silently
                onError()
            }
        }
    }
}
