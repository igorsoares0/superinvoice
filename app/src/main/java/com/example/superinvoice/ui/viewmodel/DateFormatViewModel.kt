package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DateFormatViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedFormat = MutableStateFlow("MM/dd/yyyy")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    init {
        loadCurrentFormat()
    }

    private fun loadCurrentFormat() {
        viewModelScope.launch {
            val format = settingsRepository.dateFormat.first()
            _selectedFormat.value = format
        }
    }

    fun setSelectedFormat(format: String) {
        _selectedFormat.value = format
    }

    fun saveFormat() {
        viewModelScope.launch {
            settingsRepository.saveDateFormat(_selectedFormat.value)
        }
    }
}
