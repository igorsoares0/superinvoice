package com.example.superinvoice.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Base ----------------------------------------------------------------
/** Fundo de todas as telas. */
val Paper = Color(0xFFFCFBF8)

/** Texto principal e réguas. */
val Ink = Color(0xFF211E1A)

/** Rótulos e texto de apoio. */
val Neutral = Color(0xFF8D8477)

/** Campo vazio e valor zero. */
val Ghost = Color(0xFFB3AA9B)

// ---- Marca e status ------------------------------------------------------
// Laranja é ação e "a pagar" ao mesmo tempo. Verde só confirma pagamento,
// vermelho só sinaliza atraso. Nunca laranja em texto corrido, nunca verde
// em botão.

/** Ação, marca e "a pagar". */
val Orange = Color(0xFFD2591F)
val OrangeWash = Color(0xFFFBEDE4)

/** Fatura paga. */
val Green = Color(0xFF3F7A4B)
val GreenWash = Color(0xFFEAF0E8)

/** Fatura vencida. */
val Red = Color(0xFFB5341F)
val RedWash = Color(0xFFF9E3DE)

/** Botão desabilitado. */
val Inert = Color(0xFFA79E90)
val InertWash = Color(0xFFF1EDE4)

/** Texto sobre laranja. */
val OnOrange = Color(0xFFFFFDF9)

// ---- Filetes -------------------------------------------------------------
/** Régua — abre e fecha seções. */
val Rule = Ink.copy(alpha = 0.90f)

/** Divisor — entre linhas de lista. */
val Divider = Ink.copy(alpha = 0.10f)

/** Contorno de cartão. */
val Hairline = Ink.copy(alpha = 0.14f)

/** Contorno de chip. */
val ChipLine = Ink.copy(alpha = 0.18f)
