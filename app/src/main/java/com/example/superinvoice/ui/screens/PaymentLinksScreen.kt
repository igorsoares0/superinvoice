package com.example.superinvoice.ui.screens

import android.content.ClipData
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.payment.PaymentQr
import com.example.superinvoice.data.payment.PixKeyType
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvButtonVariant
import com.example.superinvoice.ui.components.InvField
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.components.InvUnderlineTab
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.PaymentLinksViewModel
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

/** Pix, PayPal.me e um link avulso (`docs/spec-links-pagamento.md`, seção 5.3). */
@Composable
fun PaymentLinksScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    isPremium: Boolean,
    onUnlockQr: () -> Unit,
    viewModel: PaymentLinksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val input = state.input
    val errors = state.errors
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    InvSettingsSubScreen(
        title = stringResource(R.string.title_payment_links),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = { viewModel.save(onSave) },
        snackbarHostState = snackbarHostState
    ) {
        Text(
            text = stringResource(R.string.payment_links_description),
            style = InvType.body,
            color = Neutral,
            modifier = Modifier.padding(bottom = Space.xl)
        )

        if (state.showPix) {
            SectionLabel(stringResource(R.string.section_pix))
            // Cinco abas não cabem lado a lado num celular de 360dp.
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.xl)
            ) {
                PixKeyType.entries.forEach { type ->
                    InvUnderlineTab(
                        text = stringResource(type.labelRes()),
                        isSelected = type == input.pixKeyType,
                        onClick = { viewModel.setPixKeyType(type) }
                    )
                }
            }
            InvField(
                label = stringResource(R.string.pix_key),
                value = input.pixKey,
                onValueChange = viewModel::setPixKey,
                placeholder = stringResource(input.pixKeyType.placeholderRes()),
                keyboardOptions = input.pixKeyType.keyboardOptions()
            )
            if (errors.pixKey) {
                FieldError(
                    stringResource(
                        if (input.pixKey.isBlank()) R.string.error_pix_key_missing
                        else input.pixKeyType.errorRes()
                    )
                )
            }
            InvField(
                label = stringResource(R.string.pix_merchant_name),
                value = input.pixMerchantName,
                onValueChange = viewModel::setPixMerchantName,
                placeholder = state.businessName
                    .ifBlank { stringResource(R.string.pix_merchant_name_placeholder) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            if (errors.pixMerchantName) FieldError(stringResource(R.string.error_pix_merchant_name))
            InvField(
                label = stringResource(R.string.pix_merchant_city),
                value = input.pixMerchantCity,
                onValueChange = viewModel::setPixMerchantCity,
                placeholder = state.businessCity
                    .ifBlank { stringResource(R.string.pix_merchant_city_placeholder) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            if (errors.pixMerchantCity) FieldError(stringResource(R.string.error_pix_merchant_city))
            Hint(stringResource(R.string.pix_brl_only_hint))
            InvButton(
                text = stringResource(R.string.pix_test_button),
                variant = InvButtonVariant.Secondary,
                onClick = {
                    val payload = viewModel.testPixPayload() ?: return@InvButton
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Pix", payload)))
                        snackbarHostState.showSnackbar(context.getString(R.string.pix_test_copied))
                    }
                },
                modifier = Modifier.padding(top = Space.lg)
            )
            Spacer(modifier = Modifier.height(Space.xxl))
        }

        SectionLabel(stringResource(R.string.section_paypal))
        InvField(
            label = stringResource(R.string.paypal_username),
            value = input.payPalUsername,
            onValueChange = viewModel::setPayPalUsername,
            placeholder = stringResource(R.string.paypal_username_placeholder),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii
            )
        )
        if (errors.payPalUsername) FieldError(stringResource(R.string.error_paypal_username))

        Spacer(modifier = Modifier.height(Space.xxl))

        SectionLabel(stringResource(R.string.section_other_payment_link))
        InvField(
            label = stringResource(R.string.payment_link_url),
            value = input.paymentLinkUrl,
            onValueChange = viewModel::setPaymentLinkUrl,
            placeholder = stringResource(R.string.payment_link_placeholder),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri
            )
        )
        if (errors.paymentLinkUrl) FieldError(stringResource(R.string.error_payment_link))
        Hint(stringResource(R.string.payment_link_hint))

        val qrPreview = state.qrPreview
        if (qrPreview != null || state.uploadedQrWins) {
            Spacer(modifier = Modifier.height(Space.xxl))
            SectionLabel(stringResource(R.string.section_payment_qr_preview))
        }
        if (qrPreview != null) {
            QrPreview(content = qrPreview, locked = !isPremium)
            if (isPremium) {
                Hint(stringResource(R.string.payment_links_qr_note))
            } else {
                Hint(stringResource(R.string.payment_links_qr_locked))
                InvButton(
                    text = stringResource(R.string.payment_links_qr_unlock),
                    variant = InvButtonVariant.Secondary,
                    leadingIcon = InvIcons.Lock,
                    onClick = onUnlockQr,
                    modifier = Modifier.padding(top = Space.lg)
                )
            }
        } else if (state.uploadedQrWins) {
            Hint(stringResource(R.string.payment_links_uploaded_qr_note))
        }

        Spacer(modifier = Modifier.height(Space.xl))
    }
}

/**
 * O mesmo QR que o PDF desenha. Para quem não é premium ele aparece apagado, com cadeado
 * (decisão L1): dá para ver o que se ganha sem entregar o recurso.
 */
@Composable
private fun QrPreview(content: String, locked: Boolean) {
    val matrix = remember(content) { PaymentQr.encode(content) } ?: return
    Box(contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(Size.qrPreview)
                .background(Paper)
                .alpha(if (locked) 0.15f else 1f)
        ) {
            val module = size.width / matrix.width
            PaymentQr.forEachDarkRun(matrix) { row, start, end ->
                drawRect(
                    color = Ink,
                    topLeft = Offset(start * module, row * module),
                    size = androidx.compose.ui.geometry.Size((end - start) * module, module)
                )
            }
        }
        if (locked) {
            Icon(
                imageVector = InvIcons.Lock,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(IconSize.xl)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = InvType.label,
        color = Neutral,
        modifier = Modifier.padding(bottom = Space.md)
    )
}

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        style = InvType.support,
        color = Red,
        modifier = Modifier.padding(top = Space.sm)
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = InvType.support,
        color = Neutral,
        modifier = Modifier.padding(top = Space.md)
    )
}

private fun PixKeyType.labelRes(): Int = when (this) {
    PixKeyType.CPF -> R.string.pix_key_type_cpf
    PixKeyType.CNPJ -> R.string.pix_key_type_cnpj
    PixKeyType.EMAIL -> R.string.pix_key_type_email
    PixKeyType.PHONE -> R.string.pix_key_type_phone
    PixKeyType.EVP -> R.string.pix_key_type_evp
}

private fun PixKeyType.placeholderRes(): Int = when (this) {
    PixKeyType.CPF -> R.string.pix_key_placeholder_cpf
    PixKeyType.CNPJ -> R.string.pix_key_placeholder_cnpj
    PixKeyType.EMAIL -> R.string.pix_key_placeholder_email
    PixKeyType.PHONE -> R.string.pix_key_placeholder_phone
    PixKeyType.EVP -> R.string.pix_key_placeholder_evp
}

private fun PixKeyType.errorRes(): Int = when (this) {
    PixKeyType.CPF -> R.string.error_pix_key_cpf
    PixKeyType.CNPJ -> R.string.error_pix_key_cnpj
    PixKeyType.EMAIL -> R.string.error_pix_key_email
    PixKeyType.PHONE -> R.string.error_pix_key_phone
    PixKeyType.EVP -> R.string.error_pix_key_evp
}

private fun PixKeyType.keyboardOptions(): KeyboardOptions = when (this) {
    PixKeyType.CPF, PixKeyType.CNPJ -> KeyboardOptions(keyboardType = KeyboardType.Number)
    PixKeyType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
    PixKeyType.EMAIL -> KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Email
    )
    PixKeyType.EVP -> KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Ascii
    )
}
