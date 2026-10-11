package com.example.superinvoice.data.repository

import androidx.room.withTransaction
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.Payment
import com.example.superinvoice.data.calc.PaymentRules
import com.example.superinvoice.data.database.AppDatabase
import com.example.superinvoice.data.database.dao.InvoiceDao
import com.example.superinvoice.data.database.dao.InvoiceItemDao
import com.example.superinvoice.data.database.dao.PaymentDao
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.database.entities.InvoiceStatus
import com.example.superinvoice.data.database.entities.PaymentMethod
import com.example.superinvoice.util.InvoiceDates
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val database: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val paymentDao: PaymentDao,
    private val settingsRepository: SettingsRepository
) {
    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao.getAll()

    suspend fun getInvoiceById(id: Int): Invoice? = invoiceDao.getById(id)

    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>> =
        invoiceDao.getByStatus(status)

    fun getInvoicesByClientId(clientId: Int): Flow<List<Invoice>> =
        invoiceDao.getByClientId(clientId)

    fun getInvoiceItems(invoiceId: Int): Flow<List<InvoiceItem>> =
        invoiceItemDao.getByInvoiceId(invoiceId)

    suspend fun getInvoiceItemsSync(invoiceId: Int): List<InvoiceItem> =
        invoiceItemDao.getByInvoiceIdSync(invoiceId)

    /**
     * Fatura e itens numa transação só: sem ela, uma falha entre os dois inserts deixava uma
     * fatura sem itens. O contador do plano grátis fica no DataStore, fora do banco, e só sobe
     * depois que a fatura existe de fato.
     */
    suspend fun insertInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = database.withTransaction {
            val id = invoiceDao.insert(invoice)
            invoiceItemDao.insertAll(items.map { it.copy(invoiceId = id.toInt()) })
            id
        }
        settingsRepository.incrementTotalInvoicesCreated()
        return invoiceId
    }

    /**
     * Troca os itens inteiros; sem transação, uma falha no meio apagava os itens da fatura.
     * O total pode ter mudado, então o status de pagamento é recalculado na mesma transação
     * (e `amountPaid` relido dos pagamentos, não da cópia que a tela carregou).
     */
    suspend fun updateInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        database.withTransaction {
            invoiceDao.update(invoice)
            invoiceItemDao.deleteByInvoiceId(invoice.id)
            invoiceItemDao.insertAll(items)
            refreshPaymentState(invoice.id)
        }
    }

    fun getPayments(invoiceId: Int): Flow<List<Payment>> = paymentDao.getByInvoiceId(invoiceId)

    suspend fun getPaymentsSync(invoiceId: Int): List<Payment> = paymentDao.getByInvoiceIdSync(invoiceId)

    /**
     * Grava o pagamento com o próximo número de recibo e atualiza `amountPaid`, status e
     * `paymentDate` da fatura, tudo numa transação. Valor acima do saldo é aceito (a tela avisa).
     */
    suspend fun addPayment(
        invoiceId: Int,
        amount: Double,
        paidAt: Long,
        method: PaymentMethod,
        note: String
    ): Payment = database.withTransaction {
        val payment = Payment(
            invoiceId = invoiceId,
            amount = amount,
            paidAt = paidAt,
            method = method,
            note = note.trim(),
            receiptNumber = PaymentRules.nextReceiptNumber(paymentDao.getLastReceiptNumber())
        )
        val id = paymentDao.insert(payment)
        refreshPaymentState(invoiceId)
        payment.copy(id = id.toInt())
    }

    suspend fun deletePayment(payment: Payment) {
        database.withTransaction {
            paymentDao.delete(payment)
            refreshPaymentState(payment.invoiceId)
        }
    }

    /** Passo 3 da seção 5.2 da spec. Chamar sempre dentro de uma transação. */
    private suspend fun refreshPaymentState(invoiceId: Int) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        val payments = paymentDao.getByInvoiceIdSync(invoiceId)
        val amountPaid = PaymentRules.sum(payments.map { it.amount })
        val status = PaymentRules.status(invoice.status, invoice.totalAmount, amountPaid)
        val paymentDate = if (status == InvoiceStatus.PAID) payments.maxOfOrNull { it.paidAt } else null
        invoiceDao.updatePaymentState(invoiceId, amountPaid, status, paymentDate)
    }

    suspend fun insertInvoice(invoice: Invoice): Long = invoiceDao.insert(invoice)

    suspend fun updateInvoice(invoice: Invoice) = invoiceDao.update(invoice)

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.delete(invoice)
    }

    suspend fun getInvoiceCount(): Int = invoiceDao.getCount()

    suspend fun getInvoiceCountForClient(clientId: Int): Int =
        invoiceDao.getCountByClientId(clientId)

    suspend fun getPaidInvoiceCount(): Int = invoiceDao.getPaidCount()

    suspend fun getUnpaidInvoiceCount(): Int = invoiceDao.getUnpaidCount()

    suspend fun generateNextInvoiceNumber(): String = generateNextNumber(DocumentType.INVOICE)

    /** INV-001… para faturas, EST-001… para orçamentos; cada tipo com a sua sequência. */
    suspend fun generateNextNumber(type: DocumentType): String {
        val prefix = if (type == DocumentType.ESTIMATE) "EST" else "INV"
        val lastNumber = invoiceDao.getLastNumber(type)
        val numberPart = lastNumber?.substringAfterLast("-")?.toIntOrNull() ?: 0
        return "$prefix-${(numberPart + 1).toString().padStart(3, '0')}"
    }

    /** Aceito ou recusado; só faz sentido para orçamento ainda não convertido. */
    suspend fun setEstimateStatus(estimateId: Int, status: InvoiceStatus) {
        invoiceDao.updateStatus(estimateId, status)
    }

    suspend fun getInvoiceConvertedFrom(estimateId: Int): Invoice? = invoiceDao.getConvertedFrom(estimateId)

    /**
     * Converte um orçamento em fatura (spec, seção 5.1), numa transação: fatura `DRAFT` com
     * cliente, itens, ajustes, frete, notas e moeda do orçamento, número `INV-` novo, criada
     * agora e vencendo hoje; o orçamento vira `CONVERTED`.
     *
     * Não incrementa o contador do plano grátis (decisão D1): o orçamento já contou. Se a fatura
     * já existe, devolve a mesma em vez de criar outra; se ela foi apagada, converte de novo.
     *
     * @return id da fatura, ou `null` se o id não é de um orçamento
     */
    suspend fun convertEstimateToInvoice(estimateId: Int): Int? = database.withTransaction {
        val estimate = invoiceDao.getById(estimateId)
            ?.takeIf { it.type == DocumentType.ESTIMATE }
            ?: return@withTransaction null
        invoiceDao.getConvertedFrom(estimateId)?.let { return@withTransaction it.id }

        val now = System.currentTimeMillis()
        val invoiceId = invoiceDao.insert(
            estimate.copy(
                id = 0,
                type = DocumentType.INVOICE,
                number = generateNextNumber(DocumentType.INVOICE),
                createdDate = now,
                dueDate = InvoiceDates.today(now),
                paymentDate = null,
                amountPaid = 0.0,
                status = InvoiceStatus.DRAFT,
                convertedFromId = estimate.id
            )
        ).toInt()
        val items = invoiceItemDao.getByInvoiceIdSync(estimateId)
        invoiceItemDao.insertAll(items.map { it.copy(id = 0, invoiceId = invoiceId) })
        invoiceDao.updateStatus(estimateId, InvoiceStatus.CONVERTED)
        invoiceId
    }
}
