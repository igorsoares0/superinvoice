package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Space

/**
 * A raiz de toda tela. Fundo papel, e os insets de barra de status e de
 * navegação resolvidos num lugar só — antes disso metade das telas montava
 * um `Box` cru e ficava por baixo da barra de status.
 */
@Composable
fun InvScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = Paper,
        contentColor = Ink,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            content = content
        )
    }
}

/**
 * O cabeçalho único das telas. O ícone de saída vive numa linha própria e o
 * título vem abaixo, alinhado à esquerda — a hierarquia vem do tamanho, não
 * de centralizar texto.
 */
@Composable
fun InvScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    closeIcon: ImageVector = InvIcons.Close,
    closeContentDescription: String? = null,
    titleStyle: TextStyle = InvType.sectionTitle,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.screen)
            .padding(top = Space.sm, bottom = Space.section)
    ) {
        if (onClose != null || trailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(IconSize.action)
                    ) {
                        Icon(
                            imageVector = closeIcon,
                            contentDescription = closeContentDescription,
                            tint = Ink,
                            modifier = Modifier.size(IconSize.lg)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(Space.lg))
        }
        Text(
            text = title,
            style = titleStyle,
            color = Ink
        )
    }
}
