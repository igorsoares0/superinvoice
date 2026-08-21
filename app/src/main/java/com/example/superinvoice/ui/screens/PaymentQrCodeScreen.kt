package com.example.superinvoice.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvImageSettingScreen
import com.example.superinvoice.ui.viewmodel.PaymentQrCodeViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun PaymentQrCodeScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: PaymentQrCodeViewModel = hiltViewModel()
) {
    val qrCodePath by viewModel.qrCodePath.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveQrCode(
                uri = it,
                onSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.qr_code_saved))
                    }
                },
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.qr_code_save_failed)
                        )
                    }
                }
            )
        }
    }

    InvImageSettingScreen(
        title = stringResource(R.string.title_payment_qr_code),
        heading = stringResource(R.string.upload_your_qr_code),
        description = stringResource(R.string.qr_code_description),
        imagePath = qrCodePath,
        imageContentDescription = stringResource(R.string.payment_qr_code),
        emptyLabel = stringResource(R.string.no_qr_code_uploaded),
        uploadLabel = if (qrCodePath != null) {
            stringResource(R.string.change_qr_code)
        } else {
            stringResource(R.string.upload_qr_code)
        },
        removeLabel = stringResource(R.string.remove_qr_code),
        footnote = stringResource(R.string.recommended_image_format),
        saveLabel = stringResource(R.string.save),
        closeContentDescription = stringResource(R.string.close),
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onPickImage = { imagePickerLauncher.launch("image/*") },
        onRemove = {
            viewModel.removeQrCode {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.qr_code_removed))
                }
            }
        },
        onSave = onSave
    )
}
