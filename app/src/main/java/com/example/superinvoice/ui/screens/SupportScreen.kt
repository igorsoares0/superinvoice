package com.example.superinvoice.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.components.InvSectionRule
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

@Composable
fun SupportScreen(
    onClose: () -> Unit
) {
    InvScaffold {
        InvScreenHeader(
            title = stringResource(R.string.title_support),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
        ) {
            Text(
                text = stringResource(R.string.support_need_help),
                style = InvType.sectionTitle,
                color = Ink
            )
            Text(
                text = stringResource(R.string.support_description),
                style = InvType.body,
                color = Neutral,
                modifier = Modifier.padding(top = Space.md, bottom = Space.xxl)
            )

            InvSectionRule()
            Column(modifier = Modifier.padding(vertical = Space.lg)) {
                Text(
                    text = stringResource(R.string.support_contact_email_label).uppercase(),
                    style = InvType.label,
                    color = Neutral
                )
                Spacer(modifier = Modifier.height(Space.sm))
                Text(
                    text = stringResource(R.string.support_contact_email),
                    style = InvType.fieldValue,
                    color = Ink
                )
            }
            InvSectionRule()
        }
    }
}
