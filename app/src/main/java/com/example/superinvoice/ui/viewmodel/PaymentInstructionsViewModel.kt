package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentInstructionsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _bankName = MutableStateFlow("")
    val bankName: StateFlow<String> = _bankName.asStateFlow()

    private val _accountHolderName = MutableStateFlow("")
    val accountHolderName: StateFlow<String> = _accountHolderName.asStateFlow()

    private val _accountNumber = MutableStateFlow("")
    val accountNumber: StateFlow<String> = _accountNumber.asStateFlow()

    private val _routingNumber = MutableStateFlow("")
    val routingNumber: StateFlow<String> = _routingNumber.asStateFlow()

    private val _iban = MutableStateFlow("")
    val iban: StateFlow<String> = _iban.asStateFlow()

    private val _swiftCode = MutableStateFlow("")
    val swiftCode: StateFlow<String> = _swiftCode.asStateFlow()

    private val _bankAddress = MutableStateFlow("")
    val bankAddress: StateFlow<String> = _bankAddress.asStateFlow()

    private val _paymentTerms = MutableStateFlow("")
    val paymentTerms: StateFlow<String> = _paymentTerms.asStateFlow()

    private val _additionalInstructions = MutableStateFlow("")
    val additionalInstructions: StateFlow<String> = _additionalInstructions.asStateFlow()

    init {
        loadPaymentInstructions()
    }

    private fun loadPaymentInstructions() {
        viewModelScope.launch {
            settingsRepository.bankName.collect { _bankName.value = it }
        }
        viewModelScope.launch {
            settingsRepository.accountHolderName.collect { _accountHolderName.value = it }
        }
        viewModelScope.launch {
            settingsRepository.accountNumber.collect { _accountNumber.value = it }
        }
        viewModelScope.launch {
            settingsRepository.routingNumber.collect { _routingNumber.value = it }
        }
        viewModelScope.launch {
            settingsRepository.iban.collect { _iban.value = it }
        }
        viewModelScope.launch {
            settingsRepository.swiftCode.collect { _swiftCode.value = it }
        }
        viewModelScope.launch {
            settingsRepository.bankAddress.collect { _bankAddress.value = it }
        }
        viewModelScope.launch {
            settingsRepository.paymentTerms.collect { _paymentTerms.value = it }
        }
        viewModelScope.launch {
            settingsRepository.additionalInstructions.collect { _additionalInstructions.value = it }
        }
    }

    fun setBankName(value: String) {
        _bankName.value = value
    }

    fun setAccountHolderName(value: String) {
        _accountHolderName.value = value
    }

    fun setAccountNumber(value: String) {
        _accountNumber.value = value
    }

    fun setRoutingNumber(value: String) {
        _routingNumber.value = value
    }

    fun setIban(value: String) {
        _iban.value = value
    }

    fun setSwiftCode(value: String) {
        _swiftCode.value = value
    }

    fun setBankAddress(value: String) {
        _bankAddress.value = value
    }

    fun setPaymentTerms(value: String) {
        _paymentTerms.value = value
    }

    fun setAdditionalInstructions(value: String) {
        _additionalInstructions.value = value
    }

    fun savePaymentInstructions(onSuccess: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.savePaymentInstructions(
                bankName = _bankName.value,
                accountHolderName = _accountHolderName.value,
                accountNumber = _accountNumber.value,
                routingNumber = _routingNumber.value,
                iban = _iban.value,
                swiftCode = _swiftCode.value,
                bankAddress = _bankAddress.value,
                paymentTerms = _paymentTerms.value,
                additionalInstructions = _additionalInstructions.value
            )
            onSuccess()
        }
    }
}
