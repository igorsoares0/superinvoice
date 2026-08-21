package com.example.superinvoice.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.data.Client
import online.isdevapps.superinvoice.R

@Composable
fun ClientCard(
    client: Client,
    onMenuClick: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val actions = buildList {
        onEdit?.let { add(InvMenuAction(stringResource(R.string.edit), onClick = it)) }
        onDelete?.let {
            add(InvMenuAction(stringResource(R.string.delete), destructive = true, onClick = it))
        }
    }
    val menuDescription = stringResource(R.string.menu)

    InvListRow(
        modifier = modifier,
        title = client.name,
        meta = listOfNotNull(
            client.email.takeIf { it.isNotBlank() },
            client.phone.takeIf { it.isNotBlank() }
        ).joinToString(" · ").takeIf { it.isNotBlank() },
        leading = { InvAvatar(client.name) },
        onClick = onClick,
        trailing = { InvRowMenu(actions = actions, contentDescription = menuDescription) }
    )
}
