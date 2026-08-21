package com.example.superinvoice.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.data.pdf.InvoiceAccent
import com.example.superinvoice.data.pdf.InvoiceFontChoice
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.components.InvSelectableRow
import com.example.superinvoice.ui.components.InvSettingsSubScreen
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Space
import com.example.superinvoice.ui.viewmodel.InvoiceStyleViewModel
import online.isdevapps.superinvoice.R

@Composable
fun InvoiceStyleScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: InvoiceStyleViewModel = hiltViewModel()
) {
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val font by viewModel.font.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    InvSettingsSubScreen(
        title = stringResource(R.string.title_invoice_style),
        onClose = onClose,
        closeContentDescription = stringResource(R.string.close),
        saveText = stringResource(R.string.save),
        onSave = { viewModel.save(onSave) },
        snackbarHostState = snackbarHostState
    ) {
        Text(
            text = stringResource(R.string.invoice_style_description),
            style = InvType.body,
            color = Neutral,
            modifier = Modifier.padding(bottom = Space.xl)
        )

        // A fatura de verdade, redesenhada a cada escolha.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .border(1.dp, Hairline, InvShape.card),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Space.md),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Orange)
            }
        }

        Spacer(modifier = Modifier.height(Space.xxl))

        Text(
            text = stringResource(R.string.invoice_style_accent).uppercase(),
            style = InvType.label,
            color = Neutral,
            modifier = Modifier.padding(bottom = Space.md)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            InvoiceAccent.entries.forEach { swatch ->
                Swatch(
                    accent = swatch,
                    selected = swatch == accent,
                    onClick = { viewModel.setAccent(swatch) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(Space.xxl))

        Text(
            text = stringResource(R.string.invoice_style_typeface).uppercase(),
            style = InvType.label,
            color = Neutral,
            modifier = Modifier.padding(bottom = Space.md)
        )
        InvSectionRule()
        InvoiceFontChoice.entries.forEach { choice ->
            InvSelectableRow(
                title = stringResource(choice.labelRes()),
                subtitle = stringResource(choice.noteRes()),
                isSelected = choice == font,
                onClick = { viewModel.setFont(choice) }
            )
        }

        Spacer(modifier = Modifier.height(Space.xl))
    }
}

@Composable
private fun Swatch(
    accent: InvoiceAccent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(accent.labelRes())
    Box(
        modifier = modifier
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(accent.argb), CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Ink else Hairline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = InvIcons.Check,
                    contentDescription = label,
                    tint = Color(accent.onSwatch()),
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
    }
}

/** O mesmo critério de maior contraste usado no documento. */
private fun InvoiceAccent.onSwatch(): Int {
    val r = (argb shr 16 and 0xFF) / 255.0
    val g = (argb shr 8 and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    fun lin(c: Double) = if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    val luminance = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
    val onWhite = 1.05 / (luminance + 0.05)
    val onBlack = (luminance + 0.05) / 0.05
    return if (onBlack > onWhite) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
}

private fun InvoiceAccent.labelRes(): Int = when (this) {
    InvoiceAccent.Black -> R.string.accent_black
    InvoiceAccent.Orange -> R.string.accent_orange
    InvoiceAccent.Terracotta -> R.string.accent_terracotta
    InvoiceAccent.Green -> R.string.accent_green
    InvoiceAccent.Teal -> R.string.accent_teal
    InvoiceAccent.Navy -> R.string.accent_navy
    InvoiceAccent.Purple -> R.string.accent_purple
    InvoiceAccent.Wine -> R.string.accent_wine
    InvoiceAccent.Bronze -> R.string.accent_bronze
}

private fun InvoiceFontChoice.labelRes(): Int = when (this) {
    InvoiceFontChoice.System -> R.string.font_system
    InvoiceFontChoice.GroteskSans -> R.string.font_grotesk_sans
    InvoiceFontChoice.Sans -> R.string.font_sans
    InvoiceFontChoice.Serif -> R.string.font_serif
}

private fun InvoiceFontChoice.noteRes(): Int = when (this) {
    InvoiceFontChoice.System -> R.string.font_system_note
    InvoiceFontChoice.GroteskSans -> R.string.font_grotesk_sans_note
    InvoiceFontChoice.Sans -> R.string.font_sans_note
    InvoiceFontChoice.Serif -> R.string.font_serif_note
}
