package com.example.superinvoice.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Red

/**
 * Diálogo de confirmação no visual do app (o mesmo do "excluir fatura").
 *
 * Com [dismissText] nulo vira um aviso de um botão só: serve para explicar por que uma
 * ação não pode ser feita, sem oferecer uma escolha que não existe.
 */
@Composable
fun InvConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = InvShape.card,
        title = {
            Text(text = title, style = InvType.sectionTitle, color = Ink)
        },
        text = {
            Text(text = message, style = InvType.body, color = Neutral)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    style = InvType.action,
                    color = if (destructive) Red else Orange
                )
            }
        },
        dismissButton = dismissText?.let { text ->
            {
                TextButton(onClick = onDismiss) {
                    Text(text = text, style = InvType.action, color = Ink)
                }
            }
        }
    )
}
