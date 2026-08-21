package com.example.superinvoice.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.viewmodel.InvoiceTemplateViewModel
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space
import kotlinx.coroutines.launch
import online.isdevapps.superinvoice.R

@Composable
fun InvoiceTemplateScreen(
    onClose: () -> Unit,
    isPremium: Boolean = false,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: InvoiceTemplateViewModel = hiltViewModel()
) {
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val classicPreview by viewModel.classicPreview.collectAsStateWithLifecycle()
    val modernPreview by viewModel.modernPreview.collectAsStateWithLifecycle()
    val professionalPreview by viewModel.professionalPreview.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    InvScaffold {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InvScreenHeader(
                title = stringResource(R.string.title_invoice_templates),
                onClose = onClose,
                closeContentDescription = stringResource(R.string.close)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screen),
                verticalArrangement = Arrangement.spacedBy(Space.lg)
            ) {
                Text(
                    text = stringResource(R.string.select_preferred_template),
                    style = InvType.body,
                    color = Neutral
                )

                TemplateCard(
                    title = stringResource(R.string.template_classic),
                    previewBitmap = classicPreview,
                    isSelected = selectedTemplate == "classic",
                    isLocked = false,
                    onClick = { scope.launch { viewModel.saveSelectedTemplate("classic") } }
                )

                TemplateCard(
                    title = stringResource(R.string.template_modern),
                    previewBitmap = modernPreview,
                    isSelected = selectedTemplate == "modern",
                    isLocked = !isPremium,
                    onClick = {
                        if (isPremium) {
                            scope.launch { viewModel.saveSelectedTemplate("modern") }
                        } else {
                            onNavigateToPaywall()
                        }
                    }
                )

                TemplateCard(
                    title = stringResource(R.string.template_professional),
                    previewBitmap = professionalPreview,
                    isSelected = selectedTemplate == "professional",
                    isLocked = !isPremium,
                    onClick = {
                        if (isPremium) {
                            scope.launch { viewModel.saveSelectedTemplate("professional") }
                        } else {
                            onNavigateToPaywall()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(Space.lg))

                InvButton(
                    text = stringResource(R.string.done),
                    onClick = onClose
                )

                Spacer(modifier = Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun TemplateCard(
    title: String,
    previewBitmap: Bitmap?,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) Size.underline else Size.hairline,
                color = if (isSelected) Orange else Hairline,
                shape = InvShape.card
            )
            .clickable(onClick = onClick)
            .padding(Space.md)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Orange)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                text = title,
                style = InvType.name,
                color = if (isSelected) Orange else Ink,
                modifier = Modifier.weight(1f)
            )
            if (isLocked) {
                Icon(
                    imageVector = InvIcons.Lock,
                    contentDescription = null,
                    tint = Neutral,
                    modifier = Modifier.size(IconSize.md)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = InvIcons.Check,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
    }
}
