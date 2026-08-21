package com.example.superinvoice.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Ghost
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space
import java.io.File

/**
 * A subtela de "envie uma imagem": logo, QR code de pagamento e assinatura
 * compartilham exatamente este desenho.
 */
@Composable
fun InvImageSettingScreen(
    title: String,
    heading: String,
    description: String,
    imagePath: String?,
    imageContentDescription: String,
    emptyLabel: String,
    uploadLabel: String,
    removeLabel: String,
    footnote: String,
    saveLabel: String,
    onClose: () -> Unit,
    onPickImage: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    closeContentDescription: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    previewHeight: Dp = 200.dp,
    previewFillsWidth: Boolean = false,
    extraActions: @Composable ColumnScope.() -> Unit = {}
) {
    InvSettingsSubScreen(
        title = title,
        onClose = onClose,
        closeContentDescription = closeContentDescription,
        saveText = saveLabel,
        onSave = onSave,
        snackbarHostState = snackbarHostState
    ) {
        Spacer(modifier = Modifier.height(Space.sm))

        Text(text = heading, style = InvType.sectionTitle, color = Ink)
        Text(
            text = description,
            style = InvType.body,
            color = Neutral,
            modifier = Modifier.padding(top = Space.sm, bottom = Space.xl)
        )

        Box(
            modifier = Modifier
                .then(
                    if (previewFillsWidth) {
                        Modifier.fillMaxWidth().height(previewHeight)
                    } else {
                        Modifier.size(previewHeight)
                    }
                )
                .align(Alignment.CenterHorizontally)
                .border(Size.hairline, Hairline, InvShape.card),
            contentAlignment = Alignment.Center
        ) {
            if (imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(imagePath)),
                    contentDescription = imageContentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Space.md),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    Icon(
                        imageVector = InvIcons.Plus,
                        contentDescription = null,
                        tint = Ghost,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(text = emptyLabel, style = InvType.support, color = Ghost)
                }
            }
        }

        Spacer(modifier = Modifier.height(Space.xl))

        InvButton(
            text = uploadLabel,
            onClick = onPickImage,
            variant = InvButtonVariant.Secondary,
            leadingIcon = InvIcons.Plus
        )

        extraActions()

        if (imagePath != null) {
            InvButton(
                text = removeLabel,
                onClick = onRemove,
                variant = InvButtonVariant.Destructive,
                leadingIcon = InvIcons.Trash,
                modifier = Modifier.padding(top = Space.md)
            )
        }

        Text(
            text = footnote,
            style = InvType.support,
            color = Neutral,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.lg, bottom = Space.xl)
        )
    }
}
