package com.example.superinvoice.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.superinvoice.data.database.entities.AdjustmentType
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
import com.example.superinvoice.util.parseDecimalInput

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

/**
 * Item da fatura, com o passo de quantidade e o total da linha. O passo soma e subtrai 1; tocar
 * na quantidade abre [InvLineItemDialog] para digitar decimal ("2,5") e trocar a unidade.
 */
@Composable
fun InvLineItemRow(
    name: String,
    unitPrice: String,
    quantity: String,
    canDecrease: Boolean,
    lineTotal: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityClick: () -> Unit,
    quantityClickLabel: String,
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
                        enabled = canDecrease,
                        onClick = onDecrease
                    )
                    Text(
                        text = quantity,
                        style = InvType.amountRow.copy(textDecoration = TextDecoration.Underline),
                        color = Ink,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable(onClickLabel = quantityClickLabel, onClick = onQuantityClick)
                            .padding(vertical = Space.xs)
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

/**
 * Desconto ou imposto: o usuário escolhe entre percentual e valor fixo nas abas acima do campo.
 * O texto volta como foi digitado (vírgula ou ponto); quem lê é o `parseDecimalInput`.
 */
@Composable
fun InvAdjustmentDialog(
    title: String,
    initialType: AdjustmentType,
    initialValue: String,
    percentTabText: String,
    fixedTabText: String,
    percentLabel: String,
    fixedLabel: String,
    currencySymbol: String,
    onConfirm: (AdjustmentType, String) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String
) {
    var type by remember { mutableStateOf(initialType) }
    var value by remember { mutableStateOf(initialValue) }
    val isPercent = type == AdjustmentType.PERCENT

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = InvShape.card,
        title = {
            Text(text = title, style = InvType.sectionTitle, color = Ink)
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xl)) {
                    InvUnderlineTab(
                        text = percentTabText,
                        isSelected = isPercent,
                        onClick = { type = AdjustmentType.PERCENT }
                    )
                    InvUnderlineTab(
                        text = fixedTabText,
                        isSelected = !isPercent,
                        onClick = { type = AdjustmentType.FIXED }
                    )
                }
                Spacer(modifier = Modifier.height(Space.lg))
                InvField(
                    label = if (isPercent) percentLabel else fixedLabel,
                    value = value,
                    onValueChange = { value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    opensSection = true,
                    trailing = {
                        Text(
                            text = if (isPercent) "%" else currencySymbol,
                            style = InvType.label,
                            color = Neutral
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(type, value) }) {
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

/**
 * Quantidade decimal e unidade de um item. A unidade vem do produto e pode ser trocada só nesta
 * fatura; vazia, o PDF mostra só o número.
 */
@Composable
fun InvLineItemDialog(
    title: String,
    initialQuantity: String,
    initialUnit: String,
    quantityLabel: String,
    unitLabel: String,
    unitPlaceholder: String,
    onConfirm: (quantity: Double, unit: String) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String
) {
    var quantity by remember { mutableStateOf(initialQuantity) }
    var unit by remember { mutableStateOf(initialUnit) }
    val parsed = parseDecimalInput(quantity)?.takeIf { it > 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = InvShape.card,
        title = {
            Text(
                text = title,
                style = InvType.sectionTitle,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                InvField(
                    label = quantityLabel,
                    value = quantity,
                    onValueChange = { quantity = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    opensSection = true
                )
                InvField(
                    label = unitLabel,
                    value = unit,
                    onValueChange = { unit = it },
                    placeholder = unitPlaceholder
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it, unit) } },
                enabled = parsed != null
            ) {
                Text(
                    text = confirmText,
                    style = InvType.action,
                    color = if (parsed != null) Orange else Ghost
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, style = InvType.action, color = Ink)
            }
        }
    )
}

/** Linhas entre os itens e o total: subtotal, desconto, imposto e frete, rótulo à esquerda. */
@Composable
fun InvTotalsBreakdown(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                Text(
                    text = label,
                    style = InvType.support,
                    color = Neutral,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = InvType.support,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
