package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.theme.ChipLine
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.OrangeWash
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space

/**
 * Linha de lista. Nenhum cartão — só divisor.
 *
 * O valor é sempre `nowrap`; o nome trunca com reticências.
 */
@Composable
fun InvListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    amount: String? = null,
    statusLabel: String? = null,
    statusColor: Color = Orange,
    showStatusDot: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            if (showStatusDot) InvStatusDot(statusColor)
            leading?.invoke()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = title,
                    style = InvType.name,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta != null) {
                    Text(
                        text = meta,
                        style = InvType.support,
                        color = Neutral,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (amount != null || statusLabel != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    if (amount != null) {
                        Text(
                            text = amount,
                            style = InvType.amountRow,
                            color = Ink,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (statusLabel != null) {
                        Text(
                            text = statusLabel.uppercase(),
                            style = InvType.status,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            trailing?.invoke()
        }
        InvDivider()
    }
}

/** Status é ponto + caixa alta, sem fundo. */
@Composable
fun InvStatusLabel(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        InvStatusDot(color)
        Text(
            text = label.uppercase(),
            style = InvType.status,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun InvStatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Size.dot)
            .background(color, CircleShape)
    )
}

/** Avatar de canto 12, com a inicial em laranja sobre o lavado. */
@Composable
fun InvAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Size.avatar)
            .background(OrangeWash, InvShape.avatar),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = InvType.name,
            color = Orange
        )
    }
}

/** Chip de contorno — "+ Imposto", "+ Desconto". */
@Composable
fun InvChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = InvType.filter,
        color = Ink,
        maxLines = 1,
        modifier = modifier
            .border(Size.hairline, ChipLine, InvShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = Space.sm)
    )
}

