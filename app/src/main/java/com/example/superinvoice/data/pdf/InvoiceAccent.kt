package com.example.superinvoice.data.pdf

/**
 * A paleta de destaque da fatura.
 *
 * Curada de propósito, sem roda de cor: todas são escuras o bastante para
 * receber texto branco por cima e continuam distinguíveis impressas em
 * preto e branco. [Black] é o padrão e reproduz a fatura atual.
 *
 * O [id] é o que vai para o DataStore — os nomes das constantes podem
 * mudar, o id não.
 */
enum class InvoiceAccent(val id: String, val argb: Int) {
    Black("black", 0xFF000000.toInt()),
    Orange("orange", 0xFFD2591F.toInt()),
    Terracotta("terracotta", 0xFFB5341F.toInt()),
    Green("green", 0xFF3F7A4B.toInt()),
    Teal("teal", 0xFF1F5673.toInt()),
    Navy("navy", 0xFF23366B.toInt()),
    Purple("purple", 0xFF5B3A78.toInt()),
    Wine("wine", 0xFF7A2E43.toInt()),
    Bronze("bronze", 0xFF8A6A1F.toInt());

    companion object {
        val Default = Black

        fun from(id: String?): InvoiceAccent =
            entries.firstOrNull { it.id == id } ?: Default
    }
}
