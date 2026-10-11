package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.Payment
import com.example.superinvoice.data.analytics.AnalyticsManager
import com.example.superinvoice.data.analytics.CrashReporter
import com.example.superinvoice.data.billing.BillingManager
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.calc.PaymentRules
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod
import com.example.superinvoice.data.pdf.InvoicePdfGenerator
import com.example.superinvoice.data.repository.ClientRepository
import com.example.superinvoice.data.repository.InvoiceRepository
import com.example.superinvoice.data.repository.SettingsRepository
import com.example.superinvoice.util.InvoiceDates
import com.example.superinvoice.util.shareTextsFor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.isdevapps.superinvoice.R
import java.io.File
import javax.inject.Inject

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfGenerator: InvoicePdfGenerator,
    private val billingManager: BillingManager,
    private val analyticsManager: AnalyticsManager,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _invoice = MutableStateFlow<Invoice?>(null)
    val invoice: StateFlow<Invoice?> = _invoice.asStateFlow()

    private val _client = MutableStateFlow<Client?>(null)
    val client: StateFlow<Client?> = _client.asStateFlow()

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

    /** Em orçamento convertido: a fatura que nasceu dele, para a tela oferecer o link. */
    private val _convertedInvoice = MutableStateFlow<Invoice?>(null)
    val convertedInvoice: StateFlow<Invoice?> = _convertedInvoice.asStateFlow()

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    private var paymentsJob: Job? = null

    fun loadInvoice(invoiceId: Int) {
        if (paymentsJob == null || _invoice.value?.id != invoiceId) {
            paymentsJob?.cancel()
            paymentsJob = viewModelScope.launch {
                invoiceRepository.getPayments(invoiceId).collect { _payments.value = it }
            }
        }
        viewModelScope.launch {
            val loadedInvoice = invoiceRepository.getInvoiceById(invoiceId)
            if (loadedInvoice != null) {
                _invoice.value = loadedInvoice
                _convertedInvoice.value = if (loadedInvoice.type == DocumentType.ESTIMATE) {
                    invoiceRepository.getInvoiceConvertedFrom(loadedInvoice.id)
                } else {
                    null
                }

                val client = clientRepository.getClientById(loadedInvoice.clientId)
                _client.value = client

                val items = invoiceRepository.getInvoiceItemsSync(invoiceId)

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
                _businessInfo.value = loadBusinessInfo()

                // Load payment info
                _paymentInfo.value = loadPaymentInfo()

                // Load date format
                _dateFormat.value = settingsRepository.dateFormat.first()

                // Load selected template
                _selectedTemplate.value = settingsRepository.selectedTemplate.first()

                _formattedDueDate.value = InvoiceDates.format(loadedInvoice.dueDate, _dateFormat.value)

                // Generate preview bitmap in background
                if (client != null && _businessInfo.value != null && _paymentInfo.value != null) {
                    withContext(Dispatchers.IO) {
                        val template = com.example.superinvoice.data.pdf.invoiceTemplateOf(_selectedTemplate.value)

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
                            isPremium = billingManager.awaitPremiumStatus(),
                            style = settingsRepository.invoiceStyle()
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
                val businessInfo = loadBusinessInfo()

                // Get payment info
                val paymentInfo = loadPaymentInfo()

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

                val template = com.example.superinvoice.data.pdf.invoiceTemplateOf(selectedTemplate)


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
                    isPremium = billingManager.awaitPremiumStatus(),
                    style = settingsRepository.invoiceStyle()
                )

                if (file != null) {
                    analyticsManager.logInvoicePdfDownloaded(
                        template = selectedTemplate,
                        hasLogo = logoPath.isNotEmpty(),
                        hasSignature = signaturePath.isNotEmpty()
                    )
                    onSuccess(file.absolutePath)
                } else {
                    // Gerador devolveu null sem lançar: não há exceção para reportar,
                    // mas a falha precisa aparecer em algum lugar.
                    analyticsManager.logInvoicePdfFailed("download_null_file")
                    crashReporter.log("generateInvoicePdf devolveu null (download, template=$selectedTemplate)")
                    onError()
                }
            } catch (e: Exception) {
                analyticsManager.logInvoicePdfFailed("download_exception")
                crashReporter.recordException(e, "downloadInvoicePdf falhou")
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
                val businessInfo = loadBusinessInfo()

                // Get payment info
                val paymentInfo = loadPaymentInfo()

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

                val template = com.example.superinvoice.data.pdf.invoiceTemplateOf(selectedTemplate)


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
                    isPremium = billingManager.awaitPremiumStatus(),
                    style = settingsRepository.invoiceStyle()
                )

                if (file != null) {
                    // Share the PDF using FileProvider
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val shareTexts = shareTextsFor(context, invoice)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, shareTexts.subject)
                        putExtra(Intent.EXTRA_TEXT, shareTexts.text)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooser = Intent.createChooser(shareIntent, shareTexts.chooserTitle)
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)

                    // Registrado ao abrir o seletor, não ao entregar: o app não fica
                    // sabendo se a pessoa concluiu o envio no outro aplicativo.
                    analyticsManager.logInvoicePdfShared(
                        template = selectedTemplate,
                        hasLogo = logoPath.isNotEmpty(),
                        hasSignature = signaturePath.isNotEmpty()
                    )
                } else {
                    analyticsManager.logInvoicePdfFailed("share_null_file")
                    crashReporter.log("generateInvoicePdf devolveu null (share, template=$selectedTemplate)")
                    onError()
                }
            } catch (e: Exception) {
                analyticsManager.logInvoicePdfFailed("share_exception")
                crashReporter.recordException(e, "shareInvoicePdf falhou")
                onError()
            }
        }
    }

    /**
     * Registra o pagamento e redesenha a pré-visualização, que passa a mostrar "Pago" e o saldo.
     * [onRecorded] recebe o pagamento gravado, para a tela oferecer o recibo.
     */
    fun recordPayment(
        amount: Double,
        paidAt: Long,
        method: PaymentMethod,
        note: String,
        onRecorded: (Payment) -> Unit
    ) {
        viewModelScope.launch {
            val invoice = _invoice.value ?: return@launch
            val balance = InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)
            val payment = invoiceRepository.addPayment(invoice.id, amount, paidAt, method, note)
            analyticsManager.logPaymentRecorded(
                method = method,
                isPartial = InvoiceCalculator.balanceDue(balance, amount) > 0.0
            )
            loadInvoice(invoice.id)
            onRecorded(payment)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            invoiceRepository.deletePayment(payment)
            loadInvoice(payment.invoiceId)
        }
    }

    /** Gera o recibo do pagamento e abre o seletor de compartilhamento. */
    fun shareReceipt(payment: Payment, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val invoice = invoiceRepository.getInvoiceById(payment.invoiceId)
                val client = invoice?.let { clientRepository.getClientById(it.clientId) }
                if (invoice == null || client == null) {
                    onError()
                    return@launch
                }

                // Saldo do dia do pagamento: soma dele com os anteriores, na ordem da lista.
                val payments = invoiceRepository.getPaymentsSync(invoice.id)
                val upToThis = payments.takeWhile { it.id != payment.id } + payment
                val paidToDate = PaymentRules.sum(upToThis.map { it.amount })

                val logoPath = settingsRepository.logoPath.first()
                val file = withContext(Dispatchers.IO) {
                    pdfGenerator.generateReceiptPdf(
                        invoice = invoice,
                        client = client,
                        payment = payment,
                        paidToDate = paidToDate,
                        businessInfo = loadBusinessInfo(),
                        dateFormat = settingsRepository.dateFormat.first(),
                        logoPath = logoPath.ifEmpty { null },
                        isPremium = billingManager.awaitPremiumStatus(),
                        style = settingsRepository.invoiceStyle()
                    )
                }

                if (file == null) {
                    crashReporter.log("generateReceiptPdf devolveu null")
                    onError()
                    return@launch
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val subject = context.getString(R.string.receipt_share_subject, payment.receiptNumber)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_receipt))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                analyticsManager.logReceiptShared()
            } catch (e: Exception) {
                crashReporter.recordException(e, "shareReceipt falhou")
                onError()
            }
        }
    }

    private suspend fun loadBusinessInfo() = InvoicePdfGenerator.BusinessInfo(
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

    private suspend fun loadPaymentInfo() = InvoicePdfGenerator.PaymentInfo(
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

    /** Resposta do cliente ao orçamento: aceito ou recusado. */
    fun setEstimateStatus(status: InvoiceStatus) {
        viewModelScope.launch {
            val estimate = _invoice.value ?: return@launch
            invoiceRepository.setEstimateStatus(estimate.id, status)
            loadInvoice(estimate.id)
        }
    }

    /** Converte o orçamento em fatura e devolve o id dela para a tela abrir em edição. */
    fun convertToInvoice(onConverted: (Int) -> Unit) {
        viewModelScope.launch {
            val estimate = _invoice.value ?: return@launch
            val alreadyConverted = _convertedInvoice.value != null
            val invoiceId = invoiceRepository.convertEstimateToInvoice(estimate.id) ?: return@launch
            if (!alreadyConverted) analyticsManager.logEstimateConverted()
            onConverted(invoiceId)
        }
    }
}
