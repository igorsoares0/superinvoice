package com.example.superinvoice.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvButtonVariant
import com.example.superinvoice.ui.components.InvImageSettingScreen
import com.example.superinvoice.ui.components.PathState
import com.example.superinvoice.ui.components.SignatureCanvas
import com.example.superinvoice.ui.components.toBitmap
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.SignatureViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun SignatureScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: SignatureViewModel = hiltViewModel()
) {
    val signaturePath by viewModel.signaturePath.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDrawDialog by remember { mutableStateOf(false) }
    var signaturePaths by remember { mutableStateOf<List<PathState>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveSignatureFromUri(
                uri = it,
                onSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.signature_saved)
                        )
                    }
                },
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.signature_save_failed)
                        )
                    }
                }
            )
        }
    }

    if (showDrawDialog) {
        AlertDialog(
            onDismissRequest = { showDrawDialog = false },
            containerColor = Paper,
            shape = InvShape.card,
            title = {
                Text(
                    text = stringResource(R.string.draw_your_signature),
                    style = InvType.sectionTitle,
                    color = Ink
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(Size.hairline, Hairline, InvShape.card)
                ) {
                    SignatureCanvas(onPathsChanged = { paths -> signaturePaths = paths })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (signaturePaths.isNotEmpty()) {
                            val bitmap = signaturePaths.toBitmap(1600, 600)
                            viewModel.saveSignatureFromBitmap(
                                bitmap = bitmap,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.signature_saved)
                                        )
                                    }
                                    showDrawDialog = false
                                    signaturePaths = emptyList()
                                },
                                onError = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.signature_save_failed)
                                        )
                                    }
                                }
                            )
                        }
                    },
                    enabled = signaturePaths.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = InvType.action,
                        color = Orange
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDrawDialog = false
                        signaturePaths = emptyList()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = InvType.action,
                        color = Ink
                    )
                }
            }
        )
    }

    InvImageSettingScreen(
        title = stringResource(R.string.title_signature),
        heading = stringResource(R.string.upload_your_signature),
        description = stringResource(R.string.signature_description),
        imagePath = signaturePath,
        imageContentDescription = stringResource(R.string.signature_cd),
        emptyLabel = stringResource(R.string.no_signature),
        uploadLabel = stringResource(R.string.upload_signature),
        removeLabel = stringResource(R.string.remove_signature),
        footnote = stringResource(R.string.draw_signature_or_upload),
        saveLabel = stringResource(R.string.save),
        closeContentDescription = stringResource(R.string.close),
        snackbarHostState = snackbarHostState,
        previewHeight = 150.dp,
        previewFillsWidth = true,
        onClose = onClose,
        onPickImage = { imagePickerLauncher.launch("image/*") },
        onRemove = {
            viewModel.removeSignature {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.signature_removed)
                    )
                }
            }
        },
        onSave = onSave,
        extraActions = {
            InvButton(
                text = stringResource(R.string.draw_signature),
                onClick = { showDrawDialog = true },
                variant = InvButtonVariant.Secondary,
                leadingIcon = InvIcons.Pencil,
                modifier = Modifier.padding(top = Space.md)
            )
        }
    )
}
