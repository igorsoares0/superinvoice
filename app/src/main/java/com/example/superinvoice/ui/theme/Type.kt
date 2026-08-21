package com.example.superinvoice.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import online.isdevapps.superinvoice.R

/**
 * Títulos de tela, nomes em destaque e todo valor monetário.
 * Tracking sempre negativo.
 */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium)
)

/** Corpo, rótulos, listas e botões. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium)
)

/**
 * A escala do guia de estilo. A hierarquia vem de tamanho, tracking e caixa
 * alta — nunca de peso pesado. Nenhum peso passa de 500.
 */
object InvType {

    /** Grotesk 400 · 30 · −.9 — "Faturas" */
    val screenTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.9).sp
    )

    /** Grotesk 400 · 33 · −1.2 — o número mais importante da tela */
    val amountLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 33.sp,
        lineHeight = 33.sp,
        letterSpacing = (-1.2).sp
    )

    /** Grotesk 400 · 23 · −.7 — "Nova fatura" */
    val sectionTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.7).sp
    )

    /** Grotesk 400 · 21 · −.6 — título de estado vazio */
    val emptyTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.6).sp
    )

    /** Grotesk 400 · 20 · −.5 — valor dentro de um campo de formulário */
    val fieldValue = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp
    )

    /** Grotesk 400 · 17 · −.35 — valor em linha de lista */
    val amountRow = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.35).sp
    )

    /** DM Sans 500 · 16 · −.2 — nome, item */
    val name = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    )

    /** DM Sans 500 · 15 — rótulo de botão */
    val action = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 19.sp
    )

    /** DM Sans 400 · 14 / 1.5 — corpo */
    val body = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )

    /** DM Sans 500 · 13 — filtro, chip */
    val filter = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )

    /** DM Sans 400 · 11.5 — texto de apoio */
    val support = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
    )

    /** DM Sans 500 · 10.5 · +1.5 · CAIXA ALTA — rótulo */
    val label = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp
    )

    /** DM Sans 500 · 10.5 · +.6 · CAIXA ALTA — etiqueta de status */
    val status = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
}

/**
 * Os papéis M3 remapeados nas duas famílias, para que qualquer componente
 * Material que não receba estilo explícito já caia na tipografia certa.
 * Nenhum peso passa de Medium.
 */
val Typography = Typography(
    displayLarge = InvType.amountLarge.copy(fontSize = 45.sp, lineHeight = 48.sp),
    displayMedium = InvType.amountLarge.copy(fontSize = 38.sp, lineHeight = 42.sp),
    displaySmall = InvType.amountLarge,
    headlineLarge = InvType.screenTitle.copy(fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = InvType.screenTitle,
    headlineSmall = InvType.sectionTitle,
    titleLarge = InvType.sectionTitle.copy(fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = InvType.name,
    titleSmall = InvType.name.copy(fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = InvType.body.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = InvType.body,
    bodySmall = InvType.support,
    labelLarge = InvType.action.copy(fontSize = 14.sp),
    labelMedium = InvType.filter.copy(fontSize = 12.sp),
    labelSmall = InvType.label
)
