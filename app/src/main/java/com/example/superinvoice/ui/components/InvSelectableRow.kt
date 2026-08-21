package com.example.superinvoice.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Space

/**
 * Linha de escolha única — moeda, formato de data. O selecionado é laranja
 * com o tique à direita; sem fundo tingido nem contorno.
 */
@Composable
fun InvSelectableRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingLabel: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = title,
                    style = InvType.name,
                    color = if (isSelected) Orange else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = InvType.support,
                        color = Neutral,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = InvType.amountRow,
                    color = if (isSelected) Orange else Neutral,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = InvIcons.Check,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
        InvDivider()
    }
}
