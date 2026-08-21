package com.example.superinvoice.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Ghost
import com.example.superinvoice.ui.theme.Hairline
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space

/**
 * O número mais importante da tela vive entre duas réguas, sem fundo
 * colorido.
 */
@Composable
fun InvTotalSummary(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    meta: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InvSectionRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.lg),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(text = label.uppercase(), style = InvType.label, color = Neutral)
                Text(
                    text = amount,
                    style = InvType.amountLarge,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (meta != null) {
                Text(
                    text = meta,
                    style = InvType.support,
                    color = Neutral,
                    maxLines = 2
                )
            }
        }
        InvSectionRule()
    }
}

/** Item da fatura, com o passo de quantidade e o total da linha. */
@Composable
fun InvLineItemRow(
    name: String,
    unitPrice: String,
    quantity: Int,
    lineTotal: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
    removeContentDescription: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Text(
                    text = name,
                    style = InvType.name,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = unitPrice, style = InvType.support, color = Neutral, maxLines = 1)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    StepperButton(
                        icon = InvIcons.Minus,
                        enabled = quantity > 1,
                        onClick = onDecrease
                    )
                    Text(
                        text = "$quantity",
                        style = InvType.amountRow,
                        color = Ink
                    )
                    StepperButton(
                        icon = InvIcons.Plus,
                        enabled = true,
                        onClick = onIncrease
                    )
                }
            }

            Text(
                text = lineTotal,
                style = InvType.amountRow,
                color = Ink,
                maxLines = 1,
                softWrap = false
            )

            IconButton(onClick = onRemove, modifier = Modifier.size(IconSize.action)) {
                Icon(
                    imageVector = InvIcons.Trash,
                    contentDescription = removeContentDescription,
                    tint = Red,
                    modifier = Modifier.size(IconSize.lg)
                )
            }
        }
        InvDivider()
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = if (enabled) Ink else Ghost
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(Size.hairline, if (enabled) Hairline else Ghost, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSize.sm)
        )
    }
}

/** O diálogo de imposto e desconto — um valor, ok e cancelar. */
@Composable
fun InvAmountDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = InvShape.card,
        title = {
            Text(text = title, style = InvType.sectionTitle, color = Ink)
        },
        text = {
            InvField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                opensSection = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, style = InvType.action, color = Orange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, style = InvType.action, color = Ink)
            }
        }
    )
}
