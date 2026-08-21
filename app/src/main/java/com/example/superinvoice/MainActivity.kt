package com.example.superinvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.example.superinvoice.ui.navigation.AppNavigation
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.SuperinvoiceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O app é claro por definição, então as barras seguem o papel com
        // ícones escuros — nada de contraste automático herdado do sistema.
        val paper = Paper.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(paper, paper),
            navigationBarStyle = SystemBarStyle.light(paper, paper)
        )
        setContent {
            SuperinvoiceTheme {
                AppNavigation()
            }
        }
    }
}
