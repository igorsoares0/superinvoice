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
fun TermsScreen(
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
                    text = stringResource(R.string.title_terms_of_service),
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
                text = stringResource(R.string.terms_last_updated),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_1_title),
                body = stringResource(R.string.terms_section_1_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_2_title),
                body = stringResource(R.string.terms_section_2_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_3_title),
                body = stringResource(R.string.terms_section_3_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_4_title),
                body = stringResource(R.string.terms_section_4_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_5_title),
                body = stringResource(R.string.terms_section_5_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_6_title),
                body = stringResource(R.string.terms_section_6_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_7_title),
                body = stringResource(R.string.terms_section_7_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_8_title),
                body = stringResource(R.string.terms_section_8_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_9_title),
                body = stringResource(R.string.terms_section_9_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_10_title),
                body = stringResource(R.string.terms_section_10_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_11_title),
                body = stringResource(R.string.terms_section_11_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_12_title),
                body = stringResource(R.string.terms_section_12_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_13_title),
                body = stringResource(R.string.terms_section_13_body)
            )

            TermsSection(
                title = stringResource(R.string.terms_section_14_title),
                body = stringResource(R.string.terms_section_14_body)
            )
        }
    }
}

@Composable
private fun TermsSection(
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
