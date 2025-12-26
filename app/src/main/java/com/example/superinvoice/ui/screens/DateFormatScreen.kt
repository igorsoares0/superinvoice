package com.example.superinvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.superinvoice.ui.viewmodel.DateFormatViewModel

data class DateFormat(
    val format: String,
    val example: String,
    val description: String
)

@Composable
fun DateFormatScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    viewModel: DateFormatViewModel = hiltViewModel()
) {
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()

    val dateFormats = listOf(
        DateFormat("MM/dd/yyyy", "12/31/2024", "US Format"),
        DateFormat("dd/MM/yyyy", "31/12/2024", "European Format"),
        DateFormat("yyyy-MM-dd", "2024-12-31", "ISO Format"),
        DateFormat("dd.MM.yyyy", "31.12.2024", "German Format"),
        DateFormat("dd-MM-yyyy", "31-12-2024", "Alternative Format"),
        DateFormat("MMMM dd, yyyy", "December 31, 2024", "Long Format"),
        DateFormat("dd MMMM yyyy", "31 December 2024", "British Format"),
        DateFormat("MMM dd, yyyy", "Dec 31, 2024", "Short Month Format")
    )

    Scaffold(
        containerColor = Color(0xFFFFFFFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Date Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                dateFormats.forEach { dateFormat ->
                    DateFormatOption(
                        dateFormat = dateFormat,
                        isSelected = selectedFormat == dateFormat.format,
                        onClick = { viewModel.setSelectedFormat(dateFormat.format) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.saveFormat()
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9DEA6E),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DateFormatOption(
    dateFormat: DateFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF9DEA6E) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) Color(0xFF9DEA6E).copy(alpha = 0.1f) else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Format info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dateFormat.format,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.example,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFormat.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Check indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9DEA6E))
                    .padding(4.dp),
                tint = Color.Black
            )
        }
    }
}
