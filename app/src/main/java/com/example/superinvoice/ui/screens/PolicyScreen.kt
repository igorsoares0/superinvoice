package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.isdevapps.superinvoice.R

@Composable
fun PolicyScreen(
    onClose: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB))
                    .padding(top = 32.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
                Text(
                    text = stringResource(R.string.title_privacy_policy),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.policy_last_updated),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_1_title),
                body = stringResource(R.string.policy_section_1_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_2_title),
                body = stringResource(R.string.policy_section_2_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_3_title),
                body = stringResource(R.string.policy_section_3_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_4_title),
                body = stringResource(R.string.policy_section_4_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_5_title),
                body = stringResource(R.string.policy_section_5_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_6_title),
                body = stringResource(R.string.policy_section_6_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_7_title),
                body = stringResource(R.string.policy_section_7_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_8_title),
                body = stringResource(R.string.policy_section_8_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_9_title),
                body = stringResource(R.string.policy_section_9_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_10_title),
                body = stringResource(R.string.policy_section_10_body)
            )

            PolicySection(
                title = stringResource(R.string.policy_section_11_title),
                body = stringResource(R.string.policy_section_11_body)
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.DarkGray,
        lineHeight = 22.sp,
        modifier = Modifier.padding(bottom = 20.dp)
    )
}
