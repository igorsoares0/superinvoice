package com.example.superinvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.superinvoice.ui.navigation.AppNavigation
import com.example.superinvoice.ui.theme.SuperinvoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperinvoiceTheme {
                AppNavigation()
            }
        }
    }
}