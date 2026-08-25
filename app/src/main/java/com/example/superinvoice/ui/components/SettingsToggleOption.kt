package com.example.superinvoice.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.superinvoice.ui.theme.Green
import com.example.superinvoice.ui.theme.Inert
import com.example.superinvoice.ui.theme.Ink
import com.example.superinvoice.ui.theme.InvType
import com.example.superinvoice.ui.theme.Neutral
import com.example.superinvoice.ui.theme.Paper
import com.example.superinvoice.ui.theme.Space

/**
 * Linha de configuração com interruptor, no lugar da seta de [SettingsOption].
 *
 * Aceita [description] porque um interruptor sozinho não diz o que ele controla — e no
 * caso do consentimento de telemetria, dizer exatamente o que é e o que não é coletado
 * é o ponto principal da linha, não um detalhe.
 */
@Composable
fun SettingsToggleOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = InvType.name,
                    color = Ink
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = InvType.body,
                        color = Neutral,
                        modifier = Modifier.padding(top = Space.xs)
                    )
                }
            }
            // Mesmas cores do Switch de status de pagamento em EditInvoiceScreen, para
            // os dois interruptores do app não parecerem de origens diferentes.
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Paper,
                    checkedTrackColor = Green,
                    checkedBorderColor = Green,
                    uncheckedThumbColor = Paper,
                    uncheckedTrackColor = Inert,
                    uncheckedBorderColor = Inert
                )
            )
        }
        InvDivider()
    }
}
