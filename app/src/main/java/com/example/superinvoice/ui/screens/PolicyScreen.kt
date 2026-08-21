package com.example.superinvoice.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.superinvoice.ui.components.InvButton
import com.example.superinvoice.ui.components.InvScaffold
import com.example.superinvoice.ui.components.InvScreenHeader
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Space
import online.isdevapps.superinvoice.R

@Composable
fun PolicyScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current

    InvScaffold {
        InvScreenHeader(
            title = stringResource(R.string.title_privacy_policy),
            onClose = onClose,
            closeContentDescription = stringResource(R.string.close)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.screen),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.title_privacy_policy),
                style = InvType.sectionTitle,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.policy_web_description),
                style = InvType.body,
                color = Neutral,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.md, bottom = Space.xxl)
            )
            InvButton(
                text = stringResource(R.string.open_privacy_policy),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://isdevapps.online/privacy-policy"))
                    )
                }
            )
        }
    }
}
