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
class BusinessInformationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _ownerName = MutableStateFlow("")
    val ownerName: StateFlow<String> = _ownerName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _website = MutableStateFlow("")
    val website: StateFlow<String> = _website.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state.asStateFlow()

    private val _zipCode = MutableStateFlow("")
    val zipCode: StateFlow<String> = _zipCode.asStateFlow()

    private val _taxId = MutableStateFlow("")
    val taxId: StateFlow<String> = _taxId.asStateFlow()

    init {
        loadBusinessInformation()
    }

    private fun loadBusinessInformation() {
        viewModelScope.launch {
            settingsRepository.businessName.collect { _businessName.value = it }
        }
        viewModelScope.launch {
            settingsRepository.ownerName.collect { _ownerName.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessEmail.collect { _email.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessPhone.collect { _phone.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessWebsite.collect { _website.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessAddress.collect { _address.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessCity.collect { _city.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessState.collect { _state.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessZipCode.collect { _zipCode.value = it }
        }
        viewModelScope.launch {
            settingsRepository.businessTaxId.collect { _taxId.value = it }
        }
    }

    fun setBusinessName(value: String) {
        _businessName.value = value
    }

    fun setOwnerName(value: String) {
        _ownerName.value = value
    }

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPhone(value: String) {
        _phone.value = value
    }

    fun setWebsite(value: String) {
        _website.value = value
    }

    fun setAddress(value: String) {
        _address.value = value
    }

    fun setCity(value: String) {
        _city.value = value
    }

    fun setState(value: String) {
        _state.value = value
    }

    fun setZipCode(value: String) {
        _zipCode.value = value
    }

    fun setTaxId(value: String) {
        _taxId.value = value
    }

    fun saveBusinessInformation(onSuccess: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.saveBusinessInformation(
                businessName = _businessName.value,
                ownerName = _ownerName.value,
                email = _email.value,
                phone = _phone.value,
                website = _website.value,
                address = _address.value,
                city = _city.value,
                state = _state.value,
                zipCode = _zipCode.value,
                taxId = _taxId.value
            )
            onSuccess()
        }
    }
}
