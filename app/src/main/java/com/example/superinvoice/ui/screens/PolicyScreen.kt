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
                        contentDescription = "Close"
                    )
                }
                Text(
                    text = "Privacy Policy",
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

            PolicySection(
                title = "1. Introduction",
                body = "Welcome to SuperInvoice. We respect your privacy and are committed to protecting your personal data. This Privacy Policy explains how we collect, use, and safeguard your information when you use our mobile application."
            )

            PolicySection(
                title = "2. Information We Collect",
                body = "We may collect the following types of information:\n\n" +
                        "\u2022 Business Information: Business name, owner name, address, email, phone number, website, and tax ID that you provide in Settings.\n\n" +
                        "\u2022 Client Information: Names, email addresses, phone numbers, and addresses of your clients that you add to the app.\n\n" +
                        "\u2022 Invoice Data: Invoice numbers, amounts, items, dates, and payment details you create within the app.\n\n" +
                        "\u2022 Media Files: Logo images, signature images, and QR code images that you upload.\n\n" +
                        "\u2022 Purchase Information: Subscription status and transaction details processed through the app store."
            )

            PolicySection(
                title = "3. How We Use Your Information",
                body = "Your information is used to:\n\n" +
                        "\u2022 Generate invoices and PDF documents as requested by you.\n\n" +
                        "\u2022 Store your business and client details locally on your device for convenience.\n\n" +
                        "\u2022 Process and manage your subscription status.\n\n" +
                        "\u2022 Improve app functionality and user experience."
            )

            PolicySection(
                title = "4. Data Storage",
                body = "All your business data, client information, and invoices are stored locally on your device. We do not upload or store your invoices, client details, or business information on external servers. Subscription and purchase data is managed by the respective app store (Google Play)."
            )

            PolicySection(
                title = "5. Third-Party Services",
                body = "We use the following third-party services:\n\n" +
                        "\u2022 RevenueCat: To manage in-app subscriptions and purchases. RevenueCat may collect anonymized usage data. Please refer to RevenueCat's privacy policy for more details.\n\n" +
                        "\u2022 Google Play Billing: For processing payments and subscriptions."
            )

            PolicySection(
                title = "6. Data Sharing",
                body = "We do not sell, trade, or rent your personal information to third parties. Your data may only be shared when:\n\n" +
                        "\u2022 You explicitly choose to share an invoice via email, messaging apps, or other sharing methods.\n\n" +
                        "\u2022 Required by law or legal proceedings."
            )

            PolicySection(
                title = "7. Data Security",
                body = "We take reasonable measures to protect your information. Since data is stored locally on your device, its security is also dependent on your device's security settings, such as screen lock and encryption."
            )

            PolicySection(
                title = "8. Your Rights",
                body = "You have the right to:\n\n" +
                        "\u2022 Access, update, or delete your business and client information at any time through the app settings.\n\n" +
                        "\u2022 Delete all app data by uninstalling the application.\n\n" +
                        "\u2022 Cancel your subscription at any time through the app store."
            )

            PolicySection(
                title = "9. Children's Privacy",
                body = "SuperInvoice is not intended for use by children under the age of 13. We do not knowingly collect personal information from children."
            )

            PolicySection(
                title = "10. Changes to This Policy",
                body = "We may update this Privacy Policy from time to time. Any changes will be reflected in the app with an updated revision date. We encourage you to review this policy periodically."
            )

            PolicySection(
                title = "11. Contact Us",
                body = "If you have any questions or concerns about this Privacy Policy, please contact us at devforge4@gmail.com"
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
