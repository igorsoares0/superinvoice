package com.example.superinvoice.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.Ghost
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Space

/**
 * Campo de formulário do guia: rótulo fixo em caixa alta acima do valor,
 * nunca placeholder dentro de caixa. Sem contorno de input — o campo é
 * delimitado por uma régua acima (quando abre a seção) e um divisor abaixo.
 */
@Composable
fun InvField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    opensSection: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (opensSection) InvSectionRule()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.lg),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InvFieldLabel(label)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = InvType.fieldValue.copy(color = if (enabled) Ink else Ghost),
                    cursorBrush = SolidColor(Orange),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = InvType.fieldValue,
                                color = Ghost,
                                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                )
                trailing?.invoke()
            }
        }
        InvDivider()
    }
}

/**
 * A variante de escolher, não de digitar: mesmo desenho, com o chevron à
 * direita. O valor vazio fica em cinza fantasma.
 */
@Composable
fun InvSelectField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    opensSection: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (opensSection) InvSectionRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InvFieldLabel(label)
                Text(
                    text = value.ifEmpty { placeholder },
                    style = InvType.fieldValue,
                    color = if (value.isEmpty()) Ghost else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = InvIcons.ChevronRight,
                contentDescription = null,
                tint = Ghost,
                modifier = Modifier.size(IconSize.md)
            )
        }
        InvDivider()
    }
}

/** Rótulo fixo em caixa alta acima do valor. */
@Composable
fun InvFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = InvType.label,
        color = Neutral,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

