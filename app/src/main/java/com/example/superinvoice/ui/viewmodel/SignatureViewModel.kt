package com.example.superinvoice.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
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
class SignatureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _signaturePath = MutableStateFlow<String?>(null)
    val signaturePath: StateFlow<String?> = _signaturePath.asStateFlow()

    init {
        loadSignaturePath()
    }

    private fun loadSignaturePath() {
        viewModelScope.launch {
            val path = settingsRepository.signaturePath.first()
            _signaturePath.value = if (path.isNotEmpty()) path else null
        }
    }

    fun saveSignatureFromUri(uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val signaturesDir = File(context.filesDir, "signatures")
                if (!signaturesDir.exists()) {
                    signaturesDir.mkdirs()
                }

                val fileName = "signature_${System.currentTimeMillis()}.png"
                val signatureFile = File(signaturesDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(signatureFile).use { output ->
                        input.copyTo(output)
                    }
                }

                settingsRepository.saveSignaturePath(signatureFile.absolutePath)
                _signaturePath.value = signatureFile.absolutePath
                onSuccess()
            } catch (e: Exception) {
                // Error handled silently
                onError()
            }
        }
    }

    fun saveSignatureFromBitmap(bitmap: Bitmap, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val signaturesDir = File(context.filesDir, "signatures")
                if (!signaturesDir.exists()) {
                    signaturesDir.mkdirs()
                }

                val fileName = "signature_${System.currentTimeMillis()}.png"
                val signatureFile = File(signaturesDir, fileName)

                FileOutputStream(signatureFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                settingsRepository.saveSignaturePath(signatureFile.absolutePath)
                _signaturePath.value = signatureFile.absolutePath
                onSuccess()
            } catch (e: Exception) {
                // Error handled silently
                onError()
            }
        }
    }

    fun removeSignature(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _signaturePath.value?.let { path ->
                    File(path).delete()
                }

                settingsRepository.saveSignaturePath("")
                _signaturePath.value = null
                onSuccess()
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }
}
