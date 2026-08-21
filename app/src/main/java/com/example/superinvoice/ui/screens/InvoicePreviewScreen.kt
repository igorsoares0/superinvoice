package com.example.superinvoice.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvButtonVariant
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.InvoicePreviewViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

/**
 * A fatura em si é renderizada pelo [com.example.superinvoice.data.pdf.InvoicePdfGenerator]
 * e chega aqui como bitmap — esta tela é só a moldura em volta dela.
 */
@Composable
fun InvoicePreviewScreen(
    invoiceId: Int,
    previewVersion: Int,
    onClose: () -> Unit,
    viewModel: InvoicePreviewViewModel = hiltViewModel()
) {
    LaunchedEffect(invoiceId, previewVersion) {
        viewModel.loadInvoice(invoiceId)
    }

    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        val maxX = (scale - 1f) * 1000f
        val maxY = (scale - 1f) * 1000f

        offsetX = (offsetX + offsetChange.x).coerceIn(-maxX, maxX)
        offsetY = (offsetY + offsetChange.y).coerceIn(-maxY, maxY)
    }

    InvScaffold(snackbarHostState = snackbarHostState) {
        InvScreenHeader(
            title = stringResource(R.string.title_invoice_preview),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Space.screen),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(InvShape.card)
                    .background(Color.White)
                    .transformable(state = transformableState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.invoice_preview_cd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.FillWidth,
                        filterQuality = FilterQuality.High
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(48.dp),
                        color = Orange
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen, vertical = Space.xl),
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            InvButton(
                text = stringResource(R.string.share),
                variant = InvButtonVariant.Secondary,
                onClick = {
                    viewModel.shareInvoicePdf(
                        onError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.error_sharing_pdf)
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
            InvButton(
                text = stringResource(R.string.save_as_pdf),
                onClick = {
                    viewModel.downloadInvoicePdf(
                        onSuccess = { path ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.pdf_saved_to, path)
                                )
                            }
                        },
                        onError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.failed_to_save_pdf)
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

