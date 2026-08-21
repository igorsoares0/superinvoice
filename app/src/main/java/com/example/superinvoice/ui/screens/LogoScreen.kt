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
import com.example.superinvoice.ui.viewmodel.LogoViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun LogoScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: LogoViewModel = hiltViewModel()
) {
    val logoPath by viewModel.logoPath.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveLogo(
                uri = it,
                onSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.logo_saved))
                    }
                },
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.logo_save_failed)
                        )
                    }
                }
            )
        }
    }

    InvImageSettingScreen(
        title = stringResource(R.string.title_business_logo),
        heading = stringResource(R.string.upload_your_logo),
        description = stringResource(R.string.logo_description),
        imagePath = logoPath,
        imageContentDescription = stringResource(R.string.business_logo),
        emptyLabel = stringResource(R.string.no_logo_uploaded),
        uploadLabel = if (logoPath != null) {
            stringResource(R.string.change_logo)
        } else {
            stringResource(R.string.upload_logo)
        },
        removeLabel = stringResource(R.string.remove_logo),
        footnote = stringResource(R.string.recommended_image_format),
        saveLabel = stringResource(R.string.save),
        closeContentDescription = stringResource(R.string.close),
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onPickImage = { imagePickerLauncher.launch("image/*") },
        onRemove = {
            viewModel.removeLogo {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.logo_removed))
                }
            }
        },
        onSave = onSave
    )
}
