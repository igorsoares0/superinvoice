package com.example.superinvoice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InvoiceTemplateViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val selectedTemplate: StateFlow<String> = settingsRepository.selectedTemplate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "classic"
        )

    suspend fun saveSelectedTemplate(template: String) {
        android.util.Log.d("TemplateViewModel", "Saving template: $template")
        settingsRepository.saveSelectedTemplate(template)
        android.util.Log.d("TemplateViewModel", "Template saved successfully")
    }
}
