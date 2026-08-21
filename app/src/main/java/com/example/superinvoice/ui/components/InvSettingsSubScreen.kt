package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.superinvoice.ui.theme.Space

/**
 * O esqueleto das subtelas de ajuste: cabeçalho, conteúdo rolável e um
 * botão de salvar preso embaixo. Antes disso as sete telas repetiam as
 * mesmas ~90 linhas cada uma.
 */
@Composable
fun InvSettingsSubScreen(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String? = null,
    saveText: String? = null,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    saveHint: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    InvScaffold(
        modifier = modifier.imePadding(),
        snackbarHostState = snackbarHostState
    ) {
        InvScreenHeader(
            title = title,
            onClose = onClose,
            closeContentDescription = closeContentDescription
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen),
            content = content
        )
        if (saveText != null && onSave != null) {
            InvButton(
                text = saveText,
                onClick = onSave,
                enabled = saveEnabled,
                hint = saveHint,
                modifier = Modifier.padding(
                    start = Space.screen,
                    end = Space.screen,
                    top = Space.lg,
                    bottom = Space.xl
                )
            )
        } else {
            Column(modifier = Modifier.height(Space.xl)) {}
        }
    }
}
