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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                        contentDescription = "Close"
                    )
                }
                Text(
                    text = "Terms of Service",
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
                text = "Last updated: February 2026",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TermsSection(
                title = "1. Acceptance of Terms",
                body = "By downloading, installing, or using SuperInvoice, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the application."
            )

            TermsSection(
                title = "2. Description of Service",
                body = "SuperInvoice is a mobile application that allows users to create, manage, and share invoices. The app provides tools for managing clients, products and services, and generating PDF invoices."
            )

            TermsSection(
                title = "3. User Accounts and Responsibilities",
                body = "You are responsible for:\n\n" +
                        "\u2022 The accuracy of all business information, client data, and invoice details you enter into the app.\n\n" +
                        "\u2022 Ensuring your invoices comply with applicable local laws and tax regulations.\n\n" +
                        "\u2022 Maintaining the security of your device and any data stored within the app.\n\n" +
                        "\u2022 Any invoices generated and shared using the application."
            )

            TermsSection(
                title = "4. Free and Premium Plans",
                body = "SuperInvoice offers both free and premium plans:\n\n" +
                        "\u2022 Free Plan: Allows creation of a limited number of invoices with a watermark applied to generated PDFs.\n\n" +
                        "\u2022 Premium Plan: Unlocks unlimited invoice creation, watermark-free PDFs, and access to additional features such as custom logos and signatures.\n\n" +
                        "Premium subscriptions are billed through Google Play and are subject to the app store's terms and conditions."
            )

            TermsSection(
                title = "5. Subscriptions and Payments",
                body = "Premium subscriptions are processed through Google Play. By purchasing a subscription:\n\n" +
                        "\u2022 Payment will be charged to your Google Play account upon confirmation of purchase.\n\n" +
                        "\u2022 Subscriptions automatically renew unless canceled at least 24 hours before the end of the current billing period.\n\n" +
                        "\u2022 You can manage or cancel your subscription through Google Play settings.\n\n" +
                        "\u2022 Refunds are subject to Google Play's refund policy."
            )

            TermsSection(
                title = "6. Intellectual Property",
                body = "All content, design, graphics, and software within SuperInvoice are the intellectual property of SuperInvoice and are protected by applicable copyright and trademark laws. You may not copy, modify, distribute, or reverse-engineer any part of the application."
            )

            TermsSection(
                title = "7. Permitted Use",
                body = "You agree to use SuperInvoice only for lawful purposes. You may not:\n\n" +
                        "\u2022 Use the app to generate fraudulent or misleading invoices.\n\n" +
                        "\u2022 Attempt to circumvent subscription restrictions or tamper with the app's functionality.\n\n" +
                        "\u2022 Use the app in any way that violates applicable laws or regulations.\n\n" +
                        "\u2022 Distribute or resell the application or any content generated by it for commercial purposes beyond normal invoicing."
            )

            TermsSection(
                title = "8. Disclaimer of Warranties",
                body = "SuperInvoice is provided \"as is\" and \"as available\" without warranties of any kind, either express or implied. We do not guarantee that:\n\n" +
                        "\u2022 The app will be error-free or uninterrupted.\n\n" +
                        "\u2022 Invoices generated will meet all legal or regulatory requirements in your jurisdiction.\n\n" +
                        "\u2022 Data stored on your device will be preserved in all circumstances.\n\n" +
                        "You are solely responsible for verifying that your invoices are accurate and legally compliant."
            )

            TermsSection(
                title = "9. Limitation of Liability",
                body = "To the fullest extent permitted by law, SuperInvoice and its developers shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising from your use of the application, including but not limited to loss of data, revenue, or business opportunities."
            )

            TermsSection(
                title = "10. Data and Privacy",
                body = "Your use of SuperInvoice is also governed by our Privacy Policy. By using the app, you consent to the collection and use of information as described in our Privacy Policy."
            )

            TermsSection(
                title = "11. Termination",
                body = "We reserve the right to terminate or suspend access to the application at any time, without prior notice, for conduct that we believe violates these Terms of Service or is harmful to other users or the service."
            )

            TermsSection(
                title = "12. Changes to Terms",
                body = "We may update these Terms of Service from time to time. Continued use of the app after changes are posted constitutes your acceptance of the revised terms. We encourage you to review these terms periodically."
            )

            TermsSection(
                title = "13. Governing Law",
                body = "These Terms of Service shall be governed by and construed in accordance with applicable laws, without regard to conflict of law principles."
            )

            TermsSection(
                title = "14. Contact Us",
                body = "If you have any questions about these Terms of Service, please contact us at devforge4@gmail.com"
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
