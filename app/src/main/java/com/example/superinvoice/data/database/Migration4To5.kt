package com.example.superinvoice.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.superinvoice.util.InvoiceDates
import java.util.Locale

/**
 * Banco 4 → 5 (Onda 1, `docs/spec-onda-1.md` seção 6).
 *
 * SQLite não muda tipo de coluna nem ação de FK com `ALTER TABLE`, então `invoices` e
 * `invoice_items` são recriadas: tabela nova, cópia, `DROP` da antiga, `RENAME`. O Room roda a
 * migração com as foreign keys desligadas (o `PRAGMA foreign_keys = ON` só vem no `onOpen`),
 * por isso o `DROP` do pai não leva os filhos junto; o [checkForeignKeys] no fim confere que
 * nada ficou órfão.
 *
 * Os `CREATE TABLE` são cópia literal do `createSql` de `app/schemas/.../5.json`. Qualquer
 * diferença de tipo, nulabilidade ou default faz o Room recusar o banco na abertura.
 *
 * @param dateFormatProvider formato de data escolhido pelo usuário, para ler o `dueDate` em
 *   texto da v4 antes de tentar a lista de formatos conhecidos
 * @param onUnparsedDueDates recebe quantas faturas tinham `dueDate` ilegível (só a contagem)
 */
class Migration4To5(
    private val dateFormatProvider: () -> String?,
    private val onUnparsedDueDates: (Int) -> Unit,
    private val locale: Locale = Locale.getDefault()
) : Migration(4, 5) {

    override fun migrate(db: SupportSQLiteDatabase) {
        migrateInvoices(db)
        migrateInvoiceItems(db)

        db.execSQL("ALTER TABLE `products_services` ADD COLUMN `unit` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `clients` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`invoiceId` INTEGER NOT NULL, `amount` REAL NOT NULL, `paidAt` INTEGER NOT NULL, " +
                "`method` TEXT NOT NULL, `note` TEXT NOT NULL, `receiptNumber` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_invoiceId` ON `payments` (`invoiceId`)")
        createPaymentsForPaidInvoices(db)

        checkForeignKeys(db)
    }

    private fun migrateInvoices(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoices_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, `number` TEXT NOT NULL, `clientId` INTEGER NOT NULL, " +
                "`dueDate` INTEGER NOT NULL, `createdDate` INTEGER NOT NULL, `paymentDate` INTEGER, " +
                "`notes` TEXT NOT NULL, `subtotal` REAL NOT NULL, `discountType` TEXT NOT NULL, " +
                "`discountValue` REAL NOT NULL, `discountAmount` REAL NOT NULL, `taxType` TEXT NOT NULL, " +
                "`taxValue` REAL NOT NULL, `taxAmount` REAL NOT NULL, `shipping` REAL NOT NULL, " +
                "`totalAmount` REAL NOT NULL, `amountPaid` REAL NOT NULL, `status` TEXT NOT NULL, " +
                "`currency` TEXT NOT NULL, `convertedFromId` INTEGER, " +
                "FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )

        // Desconto e imposto da v4 eram valores fixos, e o total era subtotal + tax − discount:
        // migrando como FIXED com os mesmos números, nenhum total muda. OVERDUE deixa de ser
        // gravado (passa a ser derivado da data). Fatura paga já nasce com amountPaid = total,
        // que bate com o pagamento criado em createPaymentsForPaidInvoices. O dueDate entra
        // provisório e é convertido logo abaixo, ainda lendo o texto da tabela antiga.
        db.execSQL(
            """
            INSERT INTO `invoices_new` (
                `id`, `type`, `number`, `clientId`, `dueDate`, `createdDate`, `paymentDate`, `notes`,
                `subtotal`, `discountType`, `discountValue`, `discountAmount`,
                `taxType`, `taxValue`, `taxAmount`, `shipping`, `totalAmount`, `amountPaid`,
                `status`, `currency`, `convertedFromId`)
            SELECT
                `id`, 'INVOICE', `number`, `clientId`, `createdDate`, `createdDate`,
                CASE WHEN `status` = 'PAID' THEN COALESCE(`paymentDate`, `createdDate`) ELSE `paymentDate` END,
                `notes`,
                `subtotal`, 'FIXED', `discount`, `discount`,
                'FIXED', `tax`, `tax`, 0, `totalAmount`,
                CASE WHEN `status` = 'PAID' THEN `totalAmount` ELSE 0 END,
                CASE WHEN `status` = 'OVERDUE' THEN 'SENT' ELSE `status` END,
                `currency`, NULL
            FROM `invoices`
            """.trimIndent()
        )

        convertDueDates(db)

        db.execSQL("DROP TABLE `invoices`")
        db.execSQL("ALTER TABLE `invoices_new` RENAME TO `invoices`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_clientId` ON `invoices` (`clientId`)")
    }

    /**
     * `dueDate` era o texto já formatado no padrão do usuário da época. Vira a meia-noite UTC do
     * dia lido; se nenhum formato servir, fica o dia da criação da fatura e a contagem vai para
     * o Crashlytics (nunca o texto).
     */
    private fun convertDueDates(db: SupportSQLiteDatabase) {
        val userPattern = runCatching { dateFormatProvider() }.getOrNull()
        var unparsed = 0
        db.query("SELECT `id`, `dueDate`, `createdDate` FROM `invoices`").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val text = cursor.getString(1).orEmpty()
                val createdDate = cursor.getLong(2)
                val dueDate = InvoiceDates.parseLegacy(text, userPattern, locale, referenceDate = createdDate)
                    ?: InvoiceDates.today(now = createdDate).also { unparsed++ }
                db.execSQL("UPDATE `invoices_new` SET `dueDate` = ? WHERE `id` = ?", arrayOf<Any>(dueDate, id))
            }
        }
        if (unparsed > 0) onUnparsedDueDates(unparsed)
    }

    private fun migrateInvoiceItems(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoice_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`invoiceId` INTEGER NOT NULL, `productServiceId` INTEGER, `productServiceName` TEXT NOT NULL, " +
                "`productServiceDescription` TEXT NOT NULL, `pricePerUnit` REAL NOT NULL, " +
                "`quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `lineTotal` REAL NOT NULL, " +
                "FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`productServiceId`) REFERENCES `products_services`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            """
            INSERT INTO `invoice_items_new` (
                `id`, `invoiceId`, `productServiceId`, `productServiceName`, `productServiceDescription`,
                `pricePerUnit`, `quantity`, `unit`, `lineTotal`)
            SELECT
                `id`, `invoiceId`, `productServiceId`, `productServiceName`, `productServiceDescription`,
                `pricePerUnit`, CAST(`quantity` AS REAL), '', `lineTotal`
            FROM `invoice_items`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `invoice_items`")
        db.execSQL("ALTER TABLE `invoice_items_new` RENAME TO `invoice_items`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_invoice_items_productServiceId` ON `invoice_items` (`productServiceId`)"
        )
    }

    /**
     * Fatura paga ganha um pagamento do total, para o histórico bater com o status. A numeração
     * REC-001, REC-002… é gerada aqui porque `ROW_NUMBER()` exige SQLite 3.25 (Android 11) e o
     * minSdk é 24.
     */
    private fun createPaymentsForPaidInvoices(db: SupportSQLiteDatabase) {
        var receipt = 0
        db.query(
            "SELECT `id`, `totalAmount`, `paymentDate` FROM `invoices` WHERE `status` = 'PAID' ORDER BY `id`"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                receipt++
                val paidAt = cursor.getLong(2) // já preenchido com COALESCE(paymentDate, createdDate)
                db.execSQL(
                    "INSERT INTO `payments` (`invoiceId`, `amount`, `paidAt`, `method`, `note`, `receiptNumber`, `createdAt`) " +
                        "VALUES (?, ?, ?, 'OTHER', '', ?, ?)",
                    arrayOf<Any>(
                        cursor.getInt(0),
                        cursor.getDouble(1),
                        paidAt,
                        "REC-" + receipt.toString().padStart(3, '0'),
                        paidAt
                    )
                )
            }
        }
    }

    /** Uma violação aqui aborta a migração inteira: o banco continua na v4, intacto. */
    private fun checkForeignKeys(db: SupportSQLiteDatabase) {
        db.query("PRAGMA foreign_key_check").use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                throw IllegalStateException(
                    "Migração 4→5: ${cursor.count} violações de FK (primeira em ${cursor.getString(0)})"
                )
            }
        }
    }
}
