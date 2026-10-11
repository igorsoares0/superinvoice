package com.example.superinvoice

import com.example.superinvoice.util.formatDecimalInput
import com.example.superinvoice.util.formatQuantity
import com.example.superinvoice.util.parseDecimalInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * Em pt-BR e es o teclado decimal mostra vírgula; antes deste parser, "10,5" de imposto
 * virava 0 sem aviso. Os três idiomas do app precisam ler o que o próprio app escreve no campo.
 */
class DecimalInputTest {

    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val es = Locale.forLanguageTag("es")

    @Test
    fun `virgula e ponto valem como separador decimal`() {
        assertEquals(10.5, parseDecimalInput("10,5")!!, 0.0)
        assertEquals(10.5, parseDecimalInput("10.5")!!, 0.0)
        assertEquals(0.5, parseDecimalInput(",5")!!, 0.0)
        assertEquals(10.0, parseDecimalInput("10,")!!, 0.0)
    }

    @Test
    fun `com os dois separadores o ultimo e o decimal`() {
        assertEquals(1234.56, parseDecimalInput("1.234,56")!!, 0.0)
        assertEquals(1234.56, parseDecimalInput("1,234.56")!!, 0.0)
    }

    @Test
    fun `espacos nas pontas sao ignorados`() {
        assertEquals(7.0, parseDecimalInput(" 7 ")!!, 0.0)
    }

    @Test
    fun `entrada vazia ou invalida devolve null`() {
        listOf("", "  ", "abc", "1,2,3x", "1,2,3", ",", "1e5", "NaN").forEach {
            assertNull("\"$it\" deveria ser inválido", parseDecimalInput(it))
        }
    }

    @Test
    fun `valor formatado em cada idioma volta igual pelo parser`() {
        assertEquals("10,00", formatDecimalInput(10.0, ptBr))
        assertEquals("10,00", formatDecimalInput(10.0, es))
        assertEquals("10.00", formatDecimalInput(10.0, Locale.US))

        listOf(ptBr, es, Locale.US).forEach { locale ->
            listOf(10.0, 12.9, 12.345, 1234.5).forEach { value ->
                assertEquals("$value em $locale", value, parseDecimalInput(formatDecimalInput(value, locale))!!, 0.0)
            }
        }
    }

    @Test
    fun `zero vira campo vazio`() {
        assertEquals("", formatDecimalInput(0.0, ptBr))
    }

    @Test
    fun `quantidade inteira sem casas e decimal com o separador do idioma`() {
        assertEquals("2", formatQuantity(2.0, Locale.US))
        assertEquals("2", formatQuantity(2.0, ptBr))
        assertEquals("2.5", formatQuantity(2.5, Locale.US))
        assertEquals("2,5", formatQuantity(2.5, ptBr))
        assertEquals("0,333", formatQuantity(0.333, es))
        assertEquals("1,235", formatQuantity(1.2345, ptBr))
    }
}
