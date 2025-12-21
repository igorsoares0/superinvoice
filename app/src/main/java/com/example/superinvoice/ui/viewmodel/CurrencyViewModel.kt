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
class CurrencyViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    init {
        loadCurrency()
    }

    private fun loadCurrency() {
        viewModelScope.launch {
            settingsRepository.currency.collect { currency ->
                _selectedCurrency.value = currency
            }
        }
    }

    fun setSelectedCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun saveCurrency(onSuccess: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.saveCurrency(_selectedCurrency.value)
            onSuccess()
        }
    }
}
