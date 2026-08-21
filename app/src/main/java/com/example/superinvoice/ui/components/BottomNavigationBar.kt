package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Orange
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

sealed class BottomNavItem(
    val titleResId: Int,
    val icon: ImageVector
) {
    data object Invoices : BottomNavItem(R.string.nav_invoices, InvIcons.Document)
    data object Settings : BottomNavItem(R.string.nav_settings, InvIcons.Gear)
}

/**
 * Um ícone nunca aparece sem rótulo na navegação, e o traço fino acompanha
 * o peso leve do texto. Sem cápsula de seleção — o ativo é laranja.
 */
@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(BottomNavItem.Invoices, BottomNavItem.Settings)

    Column(modifier = Modifier.background(Paper)) {
        InvDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = selectedItem == index
                val tint = if (selected) Orange else Neutral
                val label = stringResource(item.titleResId)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(IconSize.lg)
                    )
                    Text(
                        text = label.uppercase(),
                        style = InvType.label,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
