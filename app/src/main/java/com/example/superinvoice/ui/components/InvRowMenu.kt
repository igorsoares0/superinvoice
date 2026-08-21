package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.superinvoice.ui.icons.InvIcons
import com.example.superinvoice.ui.theme.IconSize
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Red

data class InvMenuAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/** O menu de overflow das linhas de lista. */
@Composable
fun InvRowMenu(
    actions: List<InvMenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(IconSize.action)
        ) {
            Icon(
                imageVector = InvIcons.More,
                contentDescription = contentDescription,
                tint = Neutral,
                modifier = Modifier.size(IconSize.lg)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = InvShape.avatar,
            containerColor = Paper
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            style = InvType.filter,
                            color = if (action.destructive) Red else Ink
                        )
                    },
                    onClick = {
                        expanded = false
                        action.onClick()
                    }
                )
            }
        }
    }
}
