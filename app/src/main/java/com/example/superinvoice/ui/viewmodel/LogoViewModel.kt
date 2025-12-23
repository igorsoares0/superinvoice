package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.superinvoice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class LogoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _logoPath = MutableStateFlow<String?>(null)
    val logoPath: StateFlow<String?> = _logoPath.asStateFlow()

    init {
        loadLogoPath()
    }

    private fun loadLogoPath() {
        viewModelScope.launch {
            val path = settingsRepository.logoPath.first()
            _logoPath.value = if (path.isNotEmpty()) path else null
        }
    }

    fun saveLogo(uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                // Create logos directory
                val logosDir = File(context.filesDir, "logos")
                if (!logosDir.exists()) {
                    logosDir.mkdirs()
                }

                // Create file with timestamp to avoid conflicts
                val fileName = "logo_${System.currentTimeMillis()}.jpg"
                val logoFile = File(logosDir, fileName)

                // Copy the image from URI to internal storage
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(logoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Save path to preferences
                settingsRepository.saveLogoPath(logoFile.absolutePath)
                _logoPath.value = logoFile.absolutePath
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }

    fun removeLogo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // Delete old logo file if exists
                _logoPath.value?.let { path ->
                    File(path).delete()
                }

                // Clear from preferences
                settingsRepository.saveLogoPath("")
                _logoPath.value = null
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
