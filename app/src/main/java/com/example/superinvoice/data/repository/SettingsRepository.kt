package com.example.superinvoice.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.superinvoice.data.pdf.InvoiceAccent
import com.example.superinvoice.data.pdf.InvoiceFontChoice
import com.example.superinvoice.data.pdf.InvoiceStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Business Information Keys
    private val BUSINESS_NAME = stringPreferencesKey("business_name")
    private val OWNER_NAME = stringPreferencesKey("owner_name")
    private val BUSINESS_EMAIL = stringPreferencesKey("business_email")
    private val BUSINESS_PHONE = stringPreferencesKey("business_phone")
    private val BUSINESS_WEBSITE = stringPreferencesKey("business_website")
    private val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
    private val BUSINESS_CITY = stringPreferencesKey("business_city")
    private val BUSINESS_STATE = stringPreferencesKey("business_state")
    private val BUSINESS_ZIP_CODE = stringPreferencesKey("business_zip_code")
    private val BUSINESS_TAX_ID = stringPreferencesKey("business_tax_id")

    // Payment Instructions Keys
    private val BANK_NAME = stringPreferencesKey("bank_name")
    private val ACCOUNT_HOLDER_NAME = stringPreferencesKey("account_holder_name")
    private val ACCOUNT_NUMBER = stringPreferencesKey("account_number")
    private val ROUTING_NUMBER = stringPreferencesKey("routing_number")
    private val IBAN = stringPreferencesKey("iban")
    private val SWIFT_CODE = stringPreferencesKey("swift_code")
    private val BANK_ADDRESS = stringPreferencesKey("bank_address")
    private val PAYMENT_TERMS = stringPreferencesKey("payment_terms")
    private val ADDITIONAL_INSTRUCTIONS = stringPreferencesKey("additional_instructions")

    // General Settings Keys
    private val CURRENCY = stringPreferencesKey("currency")
    private val DATE_FORMAT = stringPreferencesKey("date_format")
    private val SELECTED_TEMPLATE = stringPreferencesKey("selected_template")
    private val INVOICE_ACCENT = stringPreferencesKey("invoice_accent")
    private val INVOICE_FONT = stringPreferencesKey("invoice_font")
    private val LOGO_PATH = stringPreferencesKey("logo_path")
    private val SIGNATURE_PATH = stringPreferencesKey("signature_path")
    private val PAYMENT_QR_CODE_PATH = stringPreferencesKey("payment_qr_code_path")
    private val TOTAL_INVOICES_CREATED = intPreferencesKey("total_invoices_created")

    // Business Information Flows
    val businessName: Flow<String> = context.dataStore.data.map { it[BUSINESS_NAME] ?: "" }
    val ownerName: Flow<String> = context.dataStore.data.map { it[OWNER_NAME] ?: "" }
    val businessEmail: Flow<String> = context.dataStore.data.map { it[BUSINESS_EMAIL] ?: "" }
    val businessPhone: Flow<String> = context.dataStore.data.map { it[BUSINESS_PHONE] ?: "" }
    val businessWebsite: Flow<String> = context.dataStore.data.map { it[BUSINESS_WEBSITE] ?: "" }
    val businessAddress: Flow<String> = context.dataStore.data.map { it[BUSINESS_ADDRESS] ?: "" }
    val businessCity: Flow<String> = context.dataStore.data.map { it[BUSINESS_CITY] ?: "" }
    val businessState: Flow<String> = context.dataStore.data.map { it[BUSINESS_STATE] ?: "" }
    val businessZipCode: Flow<String> = context.dataStore.data.map { it[BUSINESS_ZIP_CODE] ?: "" }
    val businessTaxId: Flow<String> = context.dataStore.data.map { it[BUSINESS_TAX_ID] ?: "" }

    // Payment Instructions Flows
    val bankName: Flow<String> = context.dataStore.data.map { it[BANK_NAME] ?: "" }
    val accountHolderName: Flow<String> = context.dataStore.data.map { it[ACCOUNT_HOLDER_NAME] ?: "" }
    val accountNumber: Flow<String> = context.dataStore.data.map { it[ACCOUNT_NUMBER] ?: "" }
    val routingNumber: Flow<String> = context.dataStore.data.map { it[ROUTING_NUMBER] ?: "" }
    val iban: Flow<String> = context.dataStore.data.map { it[IBAN] ?: "" }
    val swiftCode: Flow<String> = context.dataStore.data.map { it[SWIFT_CODE] ?: "" }
    val bankAddress: Flow<String> = context.dataStore.data.map { it[BANK_ADDRESS] ?: "" }
    val paymentTerms: Flow<String> = context.dataStore.data.map { it[PAYMENT_TERMS] ?: "" }
    val additionalInstructions: Flow<String> = context.dataStore.data.map { it[ADDITIONAL_INSTRUCTIONS] ?: "" }

    // General Settings Flows
    val currency: Flow<String> = context.dataStore.data.map { it[CURRENCY] ?: "USD" }
    val dateFormat: Flow<String> = context.dataStore.data.map { it[DATE_FORMAT] ?: "MM/dd/yyyy" }
    val selectedTemplate: Flow<String> = context.dataStore.data.map { it[SELECTED_TEMPLATE] ?: "classic" }
    val invoiceAccent: Flow<InvoiceAccent> =
        context.dataStore.data.map { InvoiceAccent.from(it[INVOICE_ACCENT]) }
    val invoiceFont: Flow<InvoiceFontChoice> =
        context.dataStore.data.map { InvoiceFontChoice.from(it[INVOICE_FONT]) }
    val logoPath: Flow<String> = context.dataStore.data.map { it[LOGO_PATH] ?: "" }
    val signaturePath: Flow<String> = context.dataStore.data.map { it[SIGNATURE_PATH] ?: "" }
    val paymentQrCodePath: Flow<String> = context.dataStore.data.map { it[PAYMENT_QR_CODE_PATH] ?: "" }
    val totalInvoicesCreated: Flow<Int> = context.dataStore.data.map { it[TOTAL_INVOICES_CREATED] ?: 0 }

    // Save Business Information
    suspend fun saveBusinessInformation(
        businessName: String,
        ownerName: String,
        email: String,
        phone: String,
        website: String,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        taxId: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[BUSINESS_NAME] = businessName
            preferences[OWNER_NAME] = ownerName
            preferences[BUSINESS_EMAIL] = email
            preferences[BUSINESS_PHONE] = phone
            preferences[BUSINESS_WEBSITE] = website
            preferences[BUSINESS_ADDRESS] = address
            preferences[BUSINESS_CITY] = city
            preferences[BUSINESS_STATE] = state
            preferences[BUSINESS_ZIP_CODE] = zipCode
            preferences[BUSINESS_TAX_ID] = taxId
        }
    }

    // Save Payment Instructions
    suspend fun savePaymentInstructions(
        bankName: String,
        accountHolderName: String,
        accountNumber: String,
        routingNumber: String,
        iban: String,
        swiftCode: String,
        bankAddress: String,
        paymentTerms: String,
        additionalInstructions: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[BANK_NAME] = bankName
            preferences[ACCOUNT_HOLDER_NAME] = accountHolderName
            preferences[ACCOUNT_NUMBER] = accountNumber
            preferences[ROUTING_NUMBER] = routingNumber
            preferences[IBAN] = iban
            preferences[SWIFT_CODE] = swiftCode
            preferences[BANK_ADDRESS] = bankAddress
            preferences[PAYMENT_TERMS] = paymentTerms
            preferences[ADDITIONAL_INSTRUCTIONS] = additionalInstructions
        }
    }

    // Save Currency
    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY] = currency
        }
    }

    // Save Date Format
    suspend fun saveDateFormat(dateFormat: String) {
        context.dataStore.edit { preferences ->
            preferences[DATE_FORMAT] = dateFormat
        }
    }

    // Save Selected Template
    suspend fun saveSelectedTemplate(template: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_TEMPLATE] = template
        }
    }

    /**
     * A aparência escolhida, pronta para o gerador de PDF. Se o usuário
     * nunca abriu o editor, isto devolve exatamente o documento de sempre.
     */
    suspend fun invoiceStyle(): InvoiceStyle = InvoiceStyle.of(
        accent = invoiceAccent.first(),
        fonts = invoiceFont.first().fonts(context)
    )

    // Save Invoice Appearance
    suspend fun saveInvoiceStyle(accent: InvoiceAccent, font: InvoiceFontChoice) {
        context.dataStore.edit { preferences ->
            preferences[INVOICE_ACCENT] = accent.id
            preferences[INVOICE_FONT] = font.id
        }
    }

    // Save Logo Path
    suspend fun saveLogoPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[LOGO_PATH] = path
        }
    }

    // Save Signature Path
    suspend fun saveSignaturePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[SIGNATURE_PATH] = path
        }
    }

    // Save Payment QR Code Path
    suspend fun savePaymentQrCodePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PAYMENT_QR_CODE_PATH] = path
        }
    }

    suspend fun incrementTotalInvoicesCreated() {
        context.dataStore.edit { preferences ->
            val current = preferences[TOTAL_INVOICES_CREATED] ?: 0
            preferences[TOTAL_INVOICES_CREATED] = current + 1
        }
    }
}
