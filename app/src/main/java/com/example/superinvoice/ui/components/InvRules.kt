package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.superinvoice.ui.theme.Divider
import com.example.superinvoice.ui.theme.Rule
import com.example.superinvoice.ui.theme.Size

/** Régua · 1px @ 90% — abre e fecha seções. */
@Composable
fun InvSectionRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Size.hairline)
            .background(Rule)
    )
}

/** Divisor · 1px @ 10% — entre linhas de lista. */
@Composable
fun InvDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Size.hairline)
            .background(Divider)
    )
}
