package com.example.superinvoice.util

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Datas "de calendário" da fatura (vencimento, validade do orçamento) são gravadas como a
 * meia-noite UTC do dia escolhido. É o que o `DatePicker` do Material devolve, e formatar
 * sempre em UTC garante que o dia não mude quando o aparelho troca de fuso.
 */
object InvoiceDates {

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * Formatos que a v4 pode ter gravado em `invoices.dueDate` (texto já formatado no padrão do
     * usuário). Antes ficavam duplicados em três classes como `reformatDateIfNeeded`.
     */
    val LEGACY_FORMATS = listOf(
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "dd.MM.yyyy",
        "dd-MM-yyyy",
        "MMMM dd, yyyy",
        "dd MMMM yyyy",
        "MMM dd, yyyy",
        "MMM, dd" // formato das primeiras versões, sem ano
    )

    /** Hoje no fuso do aparelho, como meia-noite UTC. */
    fun today(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = now }
        return Calendar.getInstance(UTC).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun format(date: Long, pattern: String, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(pattern, locale).apply { timeZone = UTC }.format(date)

    /**
     * Lê um `dueDate` da v4. Tenta primeiro o formato atual do usuário (resolve "03/04/2025"
     * entre MM/dd e dd/MM), depois a lista [LEGACY_FORMATS]. Formatos com nome de mês dependem
     * da língua, por isso [locale] deve ser o do aparelho.
     *
     * Formato sem ano pega o ano de [referenceDate] (a data de criação da fatura).
     *
     * @return meia-noite UTC do dia lido, ou `null` se nenhum formato servir
     */
    fun parseLegacy(text: String, userPattern: String?, locale: Locale, referenceDate: Long): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val patterns = listOfNotNull(userPattern?.takeIf { it.isNotBlank() }) + LEGACY_FORMATS
        for (pattern in patterns.distinct()) {
            val parsed = parseStrict(trimmed, pattern, locale) ?: continue
            if ('y' in pattern) return parsed
            val referenceYear = Calendar.getInstance(UTC).apply { timeInMillis = referenceDate }.get(Calendar.YEAR)
            return Calendar.getInstance(UTC).apply {
                timeInMillis = parsed
                set(Calendar.YEAR, referenceYear)
            }.timeInMillis
        }
        return null
    }

    // parse(String) aceita sobra no fim ("12/31/2024abc"); com ParsePosition exigimos o texto todo.
    private fun parseStrict(text: String, pattern: String, locale: Locale): Long? {
        val parser = try {
            SimpleDateFormat(pattern, locale).apply {
                timeZone = UTC
                isLenient = false
            }
        } catch (e: IllegalArgumentException) {
            return null
        }
        val position = ParsePosition(0)
        val date = parser.parse(text, position) ?: return null
        return if (position.index == text.length) date.time else null
    }
}
