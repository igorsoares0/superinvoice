package com.example.superinvoice.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Pílula em botões e chips, 18 em cartões, 12 em avatares. */
object InvShape {
    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(18.dp)
    val avatar = RoundedCornerShape(12.dp)
}

val Shapes = Shapes(
    extraSmall = InvShape.avatar,
    small = InvShape.avatar,
    medium = InvShape.card,
    large = InvShape.card,
    extraLarge = InvShape.card
)
