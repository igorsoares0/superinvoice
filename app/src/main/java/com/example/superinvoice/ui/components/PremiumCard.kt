package com.example.superinvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvShape
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.OrangeWash
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

@Composable
fun PremiumCard(
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OrangeWash, InvShape.card)
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        Text(
            text = stringResource(R.string.go_to_premium),
            style = InvType.sectionTitle,
            color = Ink
        )
        Text(
            text = stringResource(R.string.premium_description),
            style = InvType.body,
            color = Neutral
        )
        InvButton(
            text = stringResource(R.string.unlock),
            onClick = onUnlockClick,
            modifier = Modifier.padding(top = Space.xs)
        )
    }
}
