package com.example.superinvoice

import com.example.superinvoice.util.InvoiceDates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/**
 * A migração 4 → 5 converte o `dueDate` em texto de cada fatura existente; um formato lido
 * errado muda o vencimento que o cliente da fatura recebeu. O resultado é sempre a meia-noite
 * UTC do dia, a mesma convenção do DatePicker.
 */
class InvoiceDatesTest {

    private val dec31 = 1735603200000L // 2024-12-31 00:00 UTC
    private val createdIn2024 = 1717200000000L // 2024-06-01 00:00 UTC
    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val es = Locale.forLanguageTag("es")

    private fun parse(text: String, userPattern: String? = null, locale: Locale = Locale.US) =
        InvoiceDates.parseLegacy(text, userPattern, locale, referenceDate = createdIn2024)

    @Test
    fun `cada formato numerico da lista e lido`() {
        listOf("12/31/2024", "31/12/2024", "2024-12-31", "31.12.2024", "31-12-2024").forEach {
            assertEquals(it, dec31, parse(it))
        }
    }

    @Test
    fun `formatos com nome de mes dependem da lingua do aparelho`() {
        assertEquals(dec31, parse("December 31, 2024"))
        assertEquals(dec31, parse("Dec 31, 2024"))
        assertEquals(dec31, parse("31 dezembro 2024", locale = ptBr))
        assertEquals(dec31, parse("31 diciembre 2024", locale = es))
    }

    @Test
    fun `formato do usuario vence a ambiguidade entre MM-dd e dd-MM`() {
        val march4 = 1709510400000L // 2024-03-04 UTC
        val april3 = 1712102400000L // 2024-04-03 UTC
        assertEquals(march4, parse("03/04/2024", userPattern = "MM/dd/yyyy"))
        assertEquals(april3, parse("03/04/2024", userPattern = "dd/MM/yyyy"))
    }

    @Test
    fun `formato antigo sem ano usa o ano da criacao da fatura`() {
        assertEquals(dec31, parse("Dec, 31"))
    }

    @Test
    fun `texto invalido, vazio ou com sobra devolve null`() {
        listOf("", "  ", "amanhã", "31/31/2024", "12/31/2024abc").forEach {
            assertNull("\"$it\" deveria ser ilegível", parse(it))
        }
    }

    @Test
    fun `hoje e o dia local do aparelho, gravado como meia-noite UTC`() {
        val lateNightInSaoPaulo = 1735696800000L // 2025-01-01 02:00 UTC = 2024-12-31 23:00 em São Paulo
        assertEquals(dec31, InvoiceDates.today(lateNightInSaoPaulo, TimeZone.getTimeZone("America/Sao_Paulo")))
        assertEquals(dec31 + 86_400_000L, InvoiceDates.today(lateNightInSaoPaulo, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun `formatar nao muda o dia em nenhum fuso`() {
        val original = TimeZone.getDefault()
        try {
            listOf("America/Sao_Paulo", "Asia/Tokyo", "UTC").forEach {
                TimeZone.setDefault(TimeZone.getTimeZone(it))
                assertEquals(it, "31/12/2024", InvoiceDates.format(dec31, "dd/MM/yyyy", Locale.US))
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
