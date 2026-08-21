package com.example.superinvoice.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de 4. Layouts sempre com flex/grid e `gap` — ou seja,
 * `Arrangement.spacedBy`, não padding manual entre irmãos.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Margem lateral da tela. */
    val screen = 24.dp

    /** Respiro entre linhas de lista. */
    val row = 16.dp

    /** Respiro entre seções. */
    val section = 22.dp
}

object Size {
    /** Altura de botão: 52–54px. */
    val button = 52.dp

    /** Traço dos filetes. */
    val hairline = 1.dp

    /** Sublinhado do filtro ativo. */
    val underline = 1.5.dp

    /** Ponto de status. */
    val dot = 7.dp

    /** Círculo de contorno do estado vazio. */
    val emptyBadge = 64.dp

    /** Avatar de cliente/produto. */
    val avatar = 40.dp
}

/** Tamanhos de ícone do guia. */
object IconSize {
    val sm = 16.dp
    val md = 17.dp
    val lg = 19.dp
    val xl = 30.dp

    /** Padrão de toque para ícones de ação. */
    val action = 24.dp
}
