package com.example.superinvoice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Papel off-white, laranja terroso e tipografia leve.
 *
 * O guia define uma única paleta, clara. Não há esquema escuro nem cor
 * dinâmica: a marca não pode ser sobrescrita pelo wallpaper do aparelho.
 */
private val InvColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = OrangeWash,
    onPrimaryContainer = Orange,

    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = InertWash,
    onSecondaryContainer = Ink,

    tertiary = Green,
    onTertiary = Paper,
    tertiaryContainer = GreenWash,
    onTertiaryContainer = Green,

    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = InertWash,
    onSurfaceVariant = Neutral,

    error = Red,
    onError = Paper,
    errorContainer = RedWash,
    onErrorContainer = Red,

    outline = Hairline,
    outlineVariant = Divider,
    scrim = Ink
)

@Composable
fun SuperinvoiceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InvColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
