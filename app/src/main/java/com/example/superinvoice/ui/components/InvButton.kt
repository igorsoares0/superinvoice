package com.example.superinvoice.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Inert
import com.example.superinvoice.ui.theme.InertWash
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.OnOrange
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Red
import com.example.superinvoice.ui.theme.Rule
import com.example.superinvoice.ui.theme.Size
import com.example.superinvoice.ui.theme.Space

enum class InvButtonVariant { Primary, Secondary, Destructive }

/**
 * Botão pílula de 52dp. Primário laranja, secundário só contorno.
 *
 * O desabilitado sempre vem com uma linha explicando o que falta — passe
 * [hint] quando houver um texto que sirva.
 */
@Composable
fun InvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: InvButtonVariant = InvButtonVariant.Primary,
    enabled: Boolean = true,
    hint: String? = null,
    leadingIcon: ImageVector? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(Size.button),
            shape = InvShape.pill,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            border = when {
                !enabled -> null
                variant == InvButtonVariant.Secondary -> BorderStroke(Size.hairline, Rule)
                variant == InvButtonVariant.Destructive -> BorderStroke(Size.hairline, Red)
                else -> null
            },
            colors = when (variant) {
                InvButtonVariant.Primary -> ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = OnOrange,
                    disabledContainerColor = InertWash,
                    disabledContentColor = Inert
                )

                InvButtonVariant.Secondary -> ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Ink,
                    disabledContainerColor = InertWash,
                    disabledContentColor = Inert
                )

                InvButtonVariant.Destructive -> ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Red,
                    disabledContainerColor = InertWash,
                    disabledContentColor = Inert
                )
            },
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.lg)
                )
                Spacer(modifier = Modifier.width(Space.sm))
            }
            Text(text = text, style = InvType.action, maxLines = 1)
        }
        if (!enabled && hint != null) {
            Text(
                text = hint,
                style = InvType.support,
                color = Neutral,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.sm)
            )
        }
    }
}

/**
 * A única sombra do sistema, colorida e exclusiva do botão flutuante.
 * Cartões usam borda, não sombra.
 */
@Composable
fun InvFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(Size.button)
            .shadow(
                elevation = 18.dp,
                shape = InvShape.pill,
                spotColor = Orange,
                ambientColor = Orange
            ),
        shape = InvShape.pill,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = OnOrange
        ),
        contentPadding = PaddingValues(
            horizontal = 22.dp,
            vertical = 0.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.lg)
                )
            }
            Text(text = text, style = InvType.action, maxLines = 1)
        }
    }
}
