package com.example.superinvoice.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.ChipLine
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space

/**
 * Ícone em círculo de contorno, título e uma frase — nunca emoji.
 */
@Composable
fun InvEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = InvIcons.Document
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        Box(
            modifier = Modifier
                .size(Size.emptyBadge)
                .border(Size.hairline, ChipLine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = InvType.emptyTitle,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = InvType.body,
            color = Neutral,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp)
        )
    }
}
