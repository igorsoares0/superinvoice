package com.example.superinvoice.data.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.data.Payment
import com.example.superinvoice.data.calc.InvoiceCalculator
import com.example.superinvoice.data.database.entities.AdjustmentType
import com.example.superinvoice.data.database.entities.DocumentType
import com.example.superinvoice.data.payment.PaymentLinks
import com.example.superinvoice.data.payment.PaymentQr
import com.example.superinvoice.data.payment.PaymentQrSource
import com.example.superinvoice.util.InvoiceDates
import com.example.superinvoice.util.formatQuantity
import com.example.superinvoice.util.getCurrencySymbol
import com.example.superinvoice.util.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import online.isdevapps.superinvoice.R

enum class InvoiceTemplate {
    CLASSIC,
    MODERN,
    PROFESSIONAL
}

@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pageWidth = 595  // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 60f

    /** 4x os pontos ~= 288 DPI: o teto útil para impressão. */
    private val PRINT_OVERSAMPLE = 4f

    /** Vinco entre páginas no preview empilhado, em pontos. */
    private val previewPageGap = 12

    data class BusinessInfo(
        val businessName: String = "",
        val ownerName: String = "",
        val email: String = "",
        val phone: String = "",
        val website: String = "",
        val address: String = "",
        val city: String = "",
        val state: String = "",
        val zipCode: String = "",
        val taxId: String = ""
    )

    data class PaymentInfo(
        val bankName: String = "",
        val accountHolderName: String = "",
        val accountNumber: String = "",
        val routingNumber: String = "",
        val iban: String = "",
        val swiftCode: String = "",
        val bankAddress: String = "",
        val paymentTerms: String = "",
        val additionalInstructions: String = ""
    )

    /**
     * "Cidade, UF CEP" — a mesma composição aparecia seis vezes, três para
     * o negócio e três para o cliente.
     */
    /**
     * Desenha a assinatura e devolve o novo `yPos`.
     *
     * Antes, se o caminho estivesse salvo mas o arquivo tivesse sumido, ou
     * se a imagem não decodificasse, o código não caía nem no `catch` nem
     * no ramo nulo: não desenhava nada, não avançava o cursor e ninguém
     * ficava sabendo. Agora qualquer falha cai no nome do titular.
     */
    private fun drawSignature(
        pager: InvoicePager,
        paints: InvoicePaints,
        signaturePath: String?,
        ownerName: String,
        yPos: Float,
        maxHeight: Float,
        fallbackTextSize: Float
    ): Float {
        val y = pager.flowTo(yPos, maxHeight + 20f)

        val bitmap = signaturePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                runCatching {
                    if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                }.getOrNull()
            }

        if (bitmap != null) {
            val drawn = runCatching {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                // Largura limitada como a do logo: uma assinatura muito
                // alongada invadiria a coluna vizinha.
                val maxWidth = (pageWidth - 2 * margin) / 2f
                var height = maxHeight
                var width = height * aspectRatio
                if (width > maxWidth) {
                    width = maxWidth
                    height = width / aspectRatio
                }
                drawImageAt(pager.canvas, bitmap, margin, y, width, height)
                height
            }.getOrNull()
            bitmap.recycle()
            if (drawn != null) return y + drawn + 15f
        }

        if (ownerName.isEmpty()) return y
        val signaturePaint = paints.text(fallbackTextSize)
        pager.canvas.drawText(ownerName, margin, y, signaturePaint)
        return y + paints.advance(signaturePaint, 20f)
    }

    /**
     * Coloca uma imagem no PDF preservando a resolução original.
     *
     * Antes, cada imagem era reduzida com `createScaledBitmap` para o
     * tamanho em PONTOS e só então desenhada — um QR de 80x80 pontos virava
     * uma imagem de 80x80 pixels, ou seja 72 DPI, que borra ao imprimir e
     * às vezes nem escaneia. Desenhando no retângulo de destino, o PDF
     * guarda os pixels originais e o leitor escala na hora de exibir.
     *
     * Acima de ~288 DPI não há ganho visível, então uma foto de 12MP é
     * reduzida a esse teto para o arquivo não explodir.
     */
    private fun drawImageAt(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
            isDither = true
        }
        val capPx = (width * PRINT_OVERSAMPLE).toInt().coerceAtLeast(1)
        val source = if (bitmap.width > capPx) {
            val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
            Bitmap.createScaledBitmap(
                bitmap,
                capPx,
                (capPx * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        canvas.drawBitmap(
            source,
            null,
            RectF(left, top, left + width, top + height),
            paint
        )
        if (source !== bitmap) source.recycle()
    }

    /** QR gerado (Pix ou link), em pontos. O Pix com valor dá ~45 módulos: ~1,7 pt por módulo. */
    private val generatedQrSize = 90f

    /**
     * Até os links de pagamento o bloco só aparecia com nome de banco, e quem tinha só o QR
     * enviado não via nada no PDF. Agora basta um meio de pagamento qualquer.
     */
    private fun hasPaymentBlock(
        paymentInfo: PaymentInfo,
        links: PaymentLinks,
        qrImagePath: String?
    ): Boolean =
        paymentInfo.bankName.isNotEmpty() || links.hasAny || links.qrContent != null ||
            (qrImagePath != null && File(qrImagePath).exists())

    /**
     * Pix, PayPal e link, depois dos dados bancários (`docs/spec-links-pagamento.md`, 5.4).
     * Saem também para quem não é premium: é texto. O copia e cola do Pix não entra (~150
     * caracteres); ele vai na mensagem de compartilhamento.
     */
    private fun drawPaymentLinks(
        pager: InvoicePager,
        paints: InvoicePaints,
        links: PaymentLinks,
        x: Float,
        yStart: Float,
        maxWidth: Float,
        paint: Paint,
        step: Float
    ): Float {
        val lines = buildList {
            links.pixKey?.let {
                add(paints.truncate(text(R.string.pdf_pix_line, it), paint, maxWidth))
            }
            links.payPalUrl?.let {
                add(paints.truncate(text(R.string.pdf_paypal_line, it.removePrefix("https://")), paint, maxWidth))
            }
            // URL não tem espaço onde quebrar. Se passar de 3 linhas, o QR leva o link inteiro.
            links.customUrl?.let {
                addAll(paints.wrap(text(R.string.pdf_pay_online_line, it), paint, maxWidth, maxLines = 3))
            }
        }
        var y = yStart
        lines.forEach { line ->
            pager.canvas.drawText(line, x, y, paint)
            y += paints.advance(paint, step)
        }
        return y
    }

    /** Quanto o [drawPaymentLinks] e o QR gerado acrescentam a um bloco que reserva espaço. */
    private fun paymentLinksExtraHeight(links: PaymentLinks, lineStep: Float): Float {
        val lines = (if (links.pixKey != null) 1 else 0) +
            (if (links.payPalUrl != null) 1 else 0) +
            (if (links.customUrl != null) 3 else 0)
        // A reserva antiga já contava com a imagem de 80 pontos.
        val qr = if (links.qrContent != null) generatedQrSize + 20f - 80f else 0f
        return lines * lineStep + qr
    }

    private fun paymentQrHeight(links: PaymentLinks, qrImagePath: String?): Float = when {
        links.qrContent != null -> generatedQrSize + 20f
        qrImagePath != null && File(qrImagePath).exists() -> 90f
        else -> 0f
    }

    /**
     * O QR do bloco de pagamento: o gerado, quando há (premium, decisão L2 da spec), ou a imagem
     * enviada pelo usuário. Devolve o y logo abaixo.
     */
    private fun drawPaymentQr(
        pager: InvoicePager,
        paints: InvoicePaints,
        links: PaymentLinks,
        qrImagePath: String?,
        x: Float,
        yStart: Float
    ): Float {
        val matrix = links.qrContent?.let { PaymentQr.encode(it) }
        if (matrix != null) {
            val module = generatedQrSize / matrix.width
            // Sem uma sobreposição mínima, alguns leitores de PDF mostram um fio branco entre
            // fileiras vizinhas de módulos.
            val overlap = module * 0.05f
            val ink = paints.fill(Color.BLACK)
            PaymentQr.forEachDarkRun(matrix) { row, start, end ->
                pager.canvas.drawRect(
                    x + start * module,
                    yStart + row * module,
                    x + end * module + overlap,
                    yStart + (row + 1) * module + overlap,
                    ink
                )
            }
            val captionPaint = paints.text(7f, color = paints.style.muted, align = Paint.Align.CENTER)
            val caption = text(
                if (links.qrSource == PaymentQrSource.PIX) R.string.pdf_qr_caption_pix
                else R.string.pdf_qr_caption_scan
            )
            // A margem branca do QR (4 módulos) já afasta a legenda dos módulos.
            val captionY = yStart + generatedQrSize - 4 * module + paints.lineHeight(captionPaint)
            pager.canvas.drawText(caption, x + generatedQrSize / 2f, captionY, captionPaint)
            return captionY + 10f
        }

        var y = yStart
        qrImagePath?.let { qrPath ->
            try {
                val qrFile = File(qrPath)
                if (qrFile.exists()) {
                    val qrBitmap = BitmapFactory.decodeFile(qrPath)
                    qrBitmap?.let {
                        val qrSize = 80f
                        drawImageAt(pager.canvas, it, x, y, qrSize, qrSize)
                        y += qrSize + 10f
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }
        return y
    }

    /**
     * Observações da fatura (`Invoice.notes`). O campo existia no formulário e era salvo, mas
     * nenhum template o desenhava. Vira a página linha a linha, então texto longo não é cortado.
     * Devolve o y logo abaixo, já com [gapAfter]; sem observações, devolve [yStart].
     */
    private fun drawInvoiceNotes(
        pager: InvoicePager,
        paints: InvoicePaints,
        notes: String,
        title: String,
        x: Float,
        yStart: Float,
        maxWidth: Float,
        titlePaint: Paint,
        bodyPaint: Paint,
        titleStep: Float,
        lineStep: Float,
        gapAfter: Float
    ): Float {
        val trimmed = notes.trim()
        if (trimmed.isEmpty()) return yStart
        val titleHeight = paints.advance(titlePaint, titleStep)
        val lineHeight = paints.advance(bodyPaint, lineStep)
        // O título não fica sozinho no pé da página.
        var y = pager.flowTo(yStart, titleHeight + lineHeight)
        pager.canvas.drawText(title, x, y, titlePaint)
        y += titleHeight
        paints.wrapWords(trimmed, bodyPaint, maxWidth).forEach { line ->
            y = pager.flowTo(y, lineHeight)
            pager.canvas.drawText(line, x, y, bodyPaint)
            y += lineHeight
        }
        return y + gapAfter
    }

    private fun cityStateZip(city: String, state: String, zipCode: String): String =
        buildString {
            if (city.isNotEmpty()) append(city)
            if (state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(state)
            }
            if (zipCode.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(zipCode)
            }
        }

    /** Rótulos que mudam entre fatura e orçamento: título, número, vencimento × validade. */
    private class DocumentLabels(
        val title: Int,
        val number: Int,
        val dueDate: Int,
        val numberFormat: Int,
        val numberProfessional: Int,
        val dueDateProfessional: Int,
        /** No Modern, a última linha dos totais; orçamento não tem "valor a pagar". */
        val amountDue: Int
    )

    private fun labelsFor(invoice: Invoice) = if (invoice.type == DocumentType.ESTIMATE) {
        DocumentLabels(
            title = R.string.estimate_label,
            number = R.string.estimate_no_label,
            dueDate = R.string.valid_until_label,
            numberFormat = R.string.estimate_number_format,
            numberProfessional = R.string.estimate_no_professional,
            dueDateProfessional = R.string.valid_until_professional,
            amountDue = R.string.total_label
        )
    } else {
        DocumentLabels(
            title = R.string.invoice_label,
            number = R.string.invoice_no_label,
            dueDate = R.string.due_date_label,
            numberFormat = R.string.invoice_number_format,
            numberProfessional = R.string.invoice_no_professional,
            dueDateProfessional = R.string.due_date_professional,
            amountDue = R.string.amount_due_label
        )
    }

    /**
     * Rótulo do PDF na língua do aparelho. Até a Onda 1 os rótulos estavam escritos em inglês
     * aqui, e a fatura saía em inglês mesmo com o app em português ou espanhol.
     */
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun money(currencySymbol: String, value: Double) =
        "$currencySymbol${String.format("%.2f", value)}"

    /** "2", "2,5" ou "2,5 h": quantidade sem casas quando inteira, mais a unidade do item. */
    private fun quantityText(item: InvoiceItem): String {
        val quantity = formatQuantity(item.quantity)
        return if (item.unit.isBlank()) quantity else "$quantity ${item.unit}"
    }

    /** "8%" quando o ajuste é percentual; valor fixo não mostra taxa. */
    private fun rateOf(type: AdjustmentType, value: Double): String? =
        if (type == AdjustmentType.PERCENT) "${formatQuantity(value)}%" else null

    /**
     * Linhas entre o subtotal e o total, na ordem do cálculo: desconto, imposto (que incide sobre
     * o subtotal menos o desconto) e frete. Cada template passa os rótulos no seu estilo.
     */
    private fun adjustmentRows(
        invoice: Invoice,
        currencySymbol: String,
        discountLabel: (rate: String?) -> String,
        taxLabel: (rate: String?) -> String,
        shippingLabel: String
    ): List<Pair<String, String>> = buildList {
        if (invoice.discountAmount > 0) {
            add(
                discountLabel(rateOf(invoice.discountType, invoice.discountValue)) to
                    "-${money(currencySymbol, invoice.discountAmount)}"
            )
        }
        if (invoice.taxAmount > 0) {
            add(taxLabel(rateOf(invoice.taxType, invoice.taxValue)) to money(currencySymbol, invoice.taxAmount))
        }
        if (invoice.shipping > 0) {
            add(shippingLabel to money(currencySymbol, invoice.shipping))
        }
    }

    /**
     * Onde começa a coluna de rótulos dos totais. Rótulo traduzido ou com taxa
     * ("DESCUENTO (12,5%)") pode passar do espaço projetado; aí a coluna inteira recua para a
     * esquerda em vez de o rótulo invadir o valor.
     */
    private fun totalsLabelX(designedX: Float, valueStartX: Float, labels: List<Pair<String, Paint>>): Float {
        val widest = labels.maxOfOrNull { (label, paint) -> paint.measureText(label) } ?: 0f
        return minOf(designedX, valueStartX - widest - 10f)
    }

    fun generateInvoicePdf(
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String = "USD",
        dateFormat: String = "MM/dd/yyyy",
        logoPath: String? = null,
        signaturePath: String? = null,
        paymentQrCodePath: String? = null,
        paymentLinks: PaymentLinks = PaymentLinks.NONE,
        template: InvoiceTemplate = InvoiceTemplate.CLASSIC,
        isPremium: Boolean = true,
        style: InvoiceStyle = InvoiceStyle.Default
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val paints = InvoicePaints(style)
            val pager = InvoicePager(
                document = pdfDocument,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                margin = margin,
                // Em cada página, por cima do conteúdo.
                onBeforeFinishPage = if (!isPremium) ({ c -> drawWatermark(c) }) else null
            )
            val links = paymentLinks.forPlan(isPremium, hasUploadedQr = paymentQrCodePath != null)

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    drawClassicTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
                InvoiceTemplate.MODERN -> {
                    drawModernTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    drawProfessionalTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
            }

            pager.finish()

            // Save to file using modern MediaStore API (Android 10+) or legacy method
            val fileName = if (invoice.type == DocumentType.ESTIMATE) {
                "Estimate_${invoice.number}.pdf"
            } else {
                "Invoice_${invoice.number}.pdf"
            }
            val file: File?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri: Uri? = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }

                    // Get the file path for return value
                    val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                    val filePath = cursor?.use {
                        if (it.moveToFirst()) {
                            val columnIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (columnIndex >= 0) it.getString(columnIndex) else null
                        } else null
                    }

                    file = if (filePath != null) File(filePath) else {
                        // Fallback: create a temp file reference
                        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    }
                } else {
                    file = null
                }
            } else {
                // Android 9 and below: Use legacy method
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }

            pdfDocument.close()
            file
        } catch (e: Exception) {
            // Error handled silently
            null
        }
    }

    fun generateInvoicePreviewBitmap(
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String = "USD",
        dateFormat: String = "MM/dd/yyyy",
        logoPath: String? = null,
        signaturePath: String? = null,
        paymentQrCodePath: String? = null,
        paymentLinks: PaymentLinks = PaymentLinks.NONE,
        template: InvoiceTemplate = InvoiceTemplate.CLASSIC,
        scale: Int = 5,
        isPremium: Boolean = true,
        style: InvoiceStyle = InvoiceStyle.Default
    ): Bitmap? {
        return try {
            // Create PDF document in memory
            val pdfDocument = PdfDocument()
            val paints = InvoicePaints(style)
            val pager = InvoicePager(
                document = pdfDocument,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                margin = margin,
                // Em cada página, por cima do conteúdo.
                onBeforeFinishPage = if (!isPremium) ({ c -> drawWatermark(c) }) else null
            )
            val links = paymentLinks.forPlan(isPremium, hasUploadedQr = paymentQrCodePath != null)

            // Draw the invoice content based on template
            when (template) {
                InvoiceTemplate.CLASSIC -> {
                    drawClassicTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
                InvoiceTemplate.MODERN -> {
                    drawModernTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
                InvoiceTemplate.PROFESSIONAL -> {
                    drawProfessionalTemplate(pager, paints, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath, paymentQrCodePath, links)
                }
            }

            pager.finish()

            // Save to temporary file
            val tempFile = File.createTempFile("invoice_preview_", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Render PDF to Bitmap with configurable resolution
            val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            // Todas as páginas empilhadas numa imagem só, com um vinco
            // entre elas — assim o preview mostra a fatura inteira sem
            // precisar de controle de página na tela.
            val pageGap = if (pdfRenderer.pageCount > 1) previewPageGap * scale else 0
            val bitmap = Bitmap.createBitmap(
                pageWidth * scale,
                pageHeight * scale * pdfRenderer.pageCount +
                    pageGap * (pdfRenderer.pageCount - 1),
                Bitmap.Config.ARGB_8888
            )

            val bitmapCanvas = Canvas(bitmap)
            bitmapCanvas.drawColor(if (pdfRenderer.pageCount > 1) Color.LTGRAY else Color.WHITE)

            for (index in 0 until pdfRenderer.pageCount) {
                val pdfPage = pdfRenderer.openPage(index)
                val top = index * (pageHeight * scale + pageGap)
                val pageBitmap = Bitmap.createBitmap(
                    pageWidth * scale,
                    pageHeight * scale,
                    Bitmap.Config.ARGB_8888
                )
                Canvas(pageBitmap).drawColor(Color.WHITE)
                pdfPage.render(
                    pageBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                bitmapCanvas.drawBitmap(pageBitmap, 0f, top.toFloat(), null)
                pageBitmap.recycle()
                pdfPage.close()
            }
            pdfRenderer.close()
            fileDescriptor.close()

            // Clean up temp file
            tempFile.delete()

            bitmap
        } catch (e: Exception) {
            // Error handled silently
            null
        }
    }

    /**
     * Recibo de um pagamento (Onda 1, seção 5.2): um layout só, com a cor de destaque do usuário
     * e a marca d'água para quem não é premium. Vai para o cache do app, de onde é compartilhado
     * pelo FileProvider; recibo não é salvo em Downloads.
     *
     * @param paidToDate soma deste pagamento com os anteriores a ele, para o saldo do recibo ser o
     *   do dia do pagamento, mesmo se o recibo for gerado depois de outros pagamentos
     */
    fun generateReceiptPdf(
        invoice: Invoice,
        client: Client,
        payment: Payment,
        paidToDate: Double,
        businessInfo: BusinessInfo,
        dateFormat: String,
        logoPath: String?,
        isPremium: Boolean,
        style: InvoiceStyle = InvoiceStyle.Default
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pager = InvoicePager(
                document = pdfDocument,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                margin = margin,
                onBeforeFinishPage = if (!isPremium) ({ c -> drawWatermark(c) }) else null
            )
            drawReceipt(pager, InvoicePaints(style), invoice, client, payment, paidToDate, businessInfo, dateFormat, logoPath)
            pager.finish()

            val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(directory, "Receipt_${payment.receiptNumber}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun drawReceipt(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        payment: Payment,
        paidToDate: Double,
        businessInfo: BusinessInfo,
        dateFormat: String,
        logoPath: String?
    ) {
        val currencySymbol = getCurrencySymbol(invoice.currency)
        val right = pageWidth - margin
        var yPos = margin

        val titlePaint = paints.text(
            24f, family = InvoicePaints.Family.Display, color = paints.style.accent,
            align = Paint.Align.RIGHT, tracking = 0.15f
        )
        val labelPaint = paints.text(9f, weight = InvoicePaints.Weight.Bold, tracking = 0.1f)
        val bodyPaint = paints.text(11f)
        val valuePaint = paints.text(11f, align = Paint.Align.RIGHT)
        val mutedPaint = paints.text(9f, color = paints.style.muted)
        val linePaint = paints.stroke(color = paints.style.ink, width = 1f)

        // Logo à esquerda e título à direita, na mesma faixa.
        var logoBottom = yPos
        logoPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }.getOrNull()?.let { bitmap ->
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                var logoHeight = 50f
                var logoWidth = logoHeight * aspectRatio
                val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                if (logoWidth > maxLogoWidth) {
                    logoWidth = maxLogoWidth
                    logoHeight = logoWidth / aspectRatio
                }
                drawImageAt(pager.canvas, bitmap, margin, yPos, logoWidth, logoHeight)
                bitmap.recycle()
                logoBottom = yPos + logoHeight
            }
        }
        pager.canvas.drawText(text(R.string.receipt_title), right, yPos + 20f, titlePaint)
        yPos = maxOf(logoBottom + 20f, yPos + paints.advance(titlePaint, 45f))

        // Negócio à esquerda; número e data do recibo à direita.
        var leftY = yPos
        val businessLines = listOf(
            businessInfo.businessName,
            businessInfo.ownerName,
            businessInfo.email,
            businessInfo.phone,
            businessInfo.address,
            cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode),
            businessInfo.taxId.takeIf { it.isNotEmpty() }?.let { text(R.string.tax_id_label, it) }.orEmpty()
        ).filter { it.isNotEmpty() }
        businessLines.forEachIndexed { index, line ->
            val paint = if (index == 0) paints.text(11f, weight = InvoicePaints.Weight.Bold) else mutedPaint
            pager.canvas.drawText(paints.truncate(line, paint, (pageWidth - 2 * margin) / 2f), margin, leftY, paint)
            leftY += paints.advance(paint, 14f)
        }

        var rightY = yPos
        val numberLabel = text(R.string.receipt_no_label)
        val dateLabel = text(R.string.date_label)
        val infoX = right - 190f
        pager.canvas.drawText(numberLabel, infoX, rightY, labelPaint)
        pager.canvas.drawText(payment.receiptNumber, right, rightY, valuePaint)
        rightY += paints.advance(valuePaint, 15f)
        pager.canvas.drawText(dateLabel, infoX, rightY, labelPaint)
        pager.canvas.drawText(InvoiceDates.format(payment.paidAt, dateFormat), right, rightY, valuePaint)
        rightY += paints.advance(valuePaint, 15f)

        yPos = maxOf(leftY, rightY) + 20f
        pager.canvas.drawLine(margin, yPos, right, yPos, linePaint)
        yPos += 30f

        // Quem pagou.
        pager.canvas.drawText(text(R.string.received_from_label), margin, yPos, labelPaint)
        yPos += paints.advance(labelPaint, 18f)
        pager.canvas.drawText(client.name, margin, yPos, bodyPaint)
        yPos += paints.advance(bodyPaint, 15f)
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, margin, yPos, mutedPaint)
            yPos += paints.advance(mutedPaint, 14f)
        }
        yPos += 20f

        // O valor recebido, em destaque.
        pager.canvas.drawText(text(R.string.amount_received_label), margin, yPos, labelPaint)
        yPos += paints.advance(labelPaint, 34f)
        val amountPaint = paints.text(28f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        pager.canvas.drawText(money(currencySymbol, payment.amount), margin, yPos, amountPaint)
        yPos += paints.advance(amountPaint, 30f)

        pager.canvas.drawLine(margin, yPos, right, yPos, linePaint)
        yPos += 25f

        val balance = InvoiceCalculator.balanceDue(invoice.totalAmount, paidToDate)
        val rows = listOf(
            text(R.string.payment_method_label) to text(payment.method.labelRes),
            text(R.string.invoice_reference_label) to "#${invoice.number}",
            text(R.string.invoice_total_label) to money(currencySymbol, invoice.totalAmount),
            text(R.string.paid_to_date_label) to money(currencySymbol, paidToDate)
        )
        rows.forEach { (label, value) ->
            pager.canvas.drawText(label, margin, yPos, labelPaint)
            pager.canvas.drawText(value, right, yPos, valuePaint)
            yPos += paints.advance(valuePaint, 18f)
        }
        val balanceLabelPaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        val balanceValuePaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.RIGHT)
        pager.canvas.drawText(text(R.string.remaining_balance_label), margin, yPos, balanceLabelPaint)
        pager.canvas.drawText(money(currencySymbol, balance), right, yPos, balanceValuePaint)
        yPos += paints.advance(balanceValuePaint, 30f)

        if (payment.note.isNotEmpty()) {
            yPos = pager.flowTo(yPos, 20f)
            pager.canvas.drawText(
                paints.truncate(text(R.string.notes_label, payment.note), mutedPaint, pageWidth - 2 * margin),
                margin, yPos, mutedPaint
            )
        }
    }

    private fun drawWatermark(canvas: Canvas) {
        val watermarkPaint = Paint().apply {
            color = Color.GRAY
            alpha = 38 // ~15% opacity (255 * 0.15)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("SUPERINVOICE", pageWidth / 2f, pageHeight * 0.75f, watermarkPaint)
    }

    private fun drawClassicTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?,
        paymentLinks: PaymentLinks
    ) {
        val labels = labelsFor(invoice)
        var yPos = margin

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        // Calculate logo dimensions (max height 50, maintain aspect ratio)
                        val maxLogoHeight = 50f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        // Draw logo on the left
                        drawImageAt(pager.canvas, it, margin, yPos, logoWidth, logoHeight)
                        yPos += logoHeight + 20f
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Title with line
        val titlePaint = paints.text(24f, family = InvoicePaints.Family.Display, color = paints.style.accent, tracking = 0.15f)

        val linePaint = paints.stroke(color = paints.style.ink, width = 1f)

        // Draw line
        pager.canvas.drawLine(margin, yPos + 15, pageWidth - 200f, yPos + 15, linePaint)

        // Draw INVOICE text
        pager.canvas.drawText(text(labels.title), pageWidth - 180f, yPos + 20, titlePaint)
        yPos += paints.advance(titlePaint, 60f)

        // Section: ISSUED TO and Invoice Info
        val sectionTitlePaint = paints.text(9f, weight = InvoicePaints.Weight.Bold, tracking = 0.1f)

        val bodyPaint = paints.text(11f)

        // Left column - FROM (Business Information)
        val leftYStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.from_label), margin, leftYPos, sectionTitlePaint)
            leftYPos += paints.advance(sectionTitlePaint, 18f)
            pager.canvas.drawText(businessInfo.businessName, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
            if (businessInfo.ownerName.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.email.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.email, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.phone.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.phone, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.website.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.website, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.address.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.address, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
            if (cityStateZip.isNotEmpty()) {
                pager.canvas.drawText(cityStateZip, margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
            if (businessInfo.taxId.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.tax_id_label, businessInfo.taxId), margin, leftYPos, bodyPaint)
                leftYPos += paints.advance(bodyPaint, 15f)
            }
        }

        // Right column - INVOICE INFO
        var rightYPos = yPos
        val rightX = pageWidth - margin - 150f
        // Espaço para o rótulo: o projetado, ou o que a fonte realmente ocupa.
        val labelGap = paints.labelGap(
            sectionTitlePaint,
            listOf(text(labels.number), text(R.string.date_label), text(labels.dueDate)),
            100f
        )

        pager.canvas.drawText(text(labels.number), rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(invoice.number, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        // Format createdDate (Long timestamp) for DATE
        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))

        val formattedDueDate = InvoiceDates.format(invoice.dueDate, dateFormat)

        pager.canvas.drawText(text(R.string.date_label), rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(formattedCreatedDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        pager.canvas.drawText(text(labels.dueDate), rightX, rightYPos, sectionTitlePaint)
        pager.canvas.drawText(formattedDueDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        // Advance to the max height of both columns
        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Second row: ISSUED TO (left) and PAY TO (right)
        leftYPos = yPos
        rightYPos = yPos

        // ISSUED TO (left)
        pager.canvas.drawText(text(R.string.issued_to_label), margin, leftYPos, sectionTitlePaint)
        leftYPos += paints.advance(sectionTitlePaint, 18f)
        pager.canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 15f)
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.notes_label, client.notes), margin, leftYPos, bodyPaint)
            leftYPos += paints.advance(bodyPaint, 15f)
        }

        // PAY TO (right)
        val payRightX = pageWidth / 2f + 20f
        if (hasPaymentBlock(paymentInfo, paymentLinks, paymentQrCodePath)) {
            pager.canvas.drawText(text(R.string.pay_to_label), payRightX, rightYPos, sectionTitlePaint)
            rightYPos += paints.advance(sectionTitlePaint, 18f)
            if (paymentInfo.bankName.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankName, payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankAddress, payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.acc_name_label, paymentInfo.accountHolderName), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.acc_number_label, paymentInfo.accountNumber), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.routing_label, paymentInfo.routingNumber), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.iban_label, paymentInfo.iban), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.swift_label, paymentInfo.swiftCode), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.terms_label, paymentInfo.paymentTerms), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.notes_label, paymentInfo.additionalInstructions), payRightX, rightYPos, bodyPaint)
                rightYPos += paints.advance(bodyPaint, 15f)
            }

            rightYPos = drawPaymentLinks(
                pager, paints, paymentLinks, payRightX, rightYPos,
                maxWidth = pageWidth - margin - payRightX, paint = bodyPaint, step = 15f
            )

            rightYPos = drawPaymentQr(pager, paints, paymentLinks, paymentQrCodePath, payRightX, rightYPos)
        }

        // Advance to max height
        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Divider line before table
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // Table Header — repetido no topo de cada página de continuação.
        val drawTableHeader: (Float) -> Float = { start ->
            var y = start
            pager.canvas.drawText(text(R.string.description_label), margin, y, sectionTitlePaint)
            pager.canvas.drawText(text(R.string.unit_price_label), pageWidth - margin - 280f, y, sectionTitlePaint)
            pager.canvas.drawText(text(R.string.qty_label), pageWidth - margin - 150f, y, sectionTitlePaint)
            pager.canvas.drawText(text(R.string.total_label), pageWidth - margin - 80f, y, sectionTitlePaint)
            y += paints.advance(sectionTitlePaint, 20f)
            pager.canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y + paints.advance(sectionTitlePaint, 15f)
        }
        yPos = drawTableHeader(yPos)

        // Table Items
        val currencySymbol = getCurrencySymbol(currency)
        val descriptionPaint = paints.text(9f, color = paints.style.muted)
        val classicRowHeight = paints.advance(bodyPaint, 14f) + paints.advance(descriptionPaint, 14f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, classicRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - margin - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, bodyPaint, descWidth),
                margin, yPos, bodyPaint
            )
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}", pageWidth - margin - 280f, yPos, bodyPaint)
            pager.canvas.drawText(paints.truncate(quantityText(item), bodyPaint, 62f), pageWidth - margin - 150f, yPos, bodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}", pageWidth - margin - 80f, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 14f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 14f)
            } else {
                yPos += paints.advance(descriptionPaint, 6f)
            }
        }

        yPos += 20f

        val classicRows = adjustmentRows(
            invoice, currencySymbol,
            discountLabel = { rate -> rate?.let { text(R.string.discount_rate_label, it) } ?: text(R.string.discount_label) },
            taxLabel = { rate -> rate?.let { text(R.string.tax_rate_label, it) } ?: text(R.string.tax_label) },
            shippingLabel = text(R.string.shipping_label)
        )
        val isPartlyPaid = invoice.amountPaid > 0

        // Totais e assinatura seguem juntos; se não couberem, vão inteiros
        // para a próxima página em vez de serem cortados.
        yPos = pager.flowTo(yPos, 150f + 15f * (classicRows.size + if (isPartlyPaid) 2 else 0))

        // Totals
        val totalValueX = pageWidth - margin - 80f
        val totalPaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        val classicLabels = listOf(text(R.string.subtotal_label)) + classicRows.map { it.first } +
            if (isPartlyPaid) listOf(text(R.string.paid_label)) else emptyList()
        val totalX = totalsLabelX(
            designedX = pageWidth - margin - 180f,
            valueStartX = totalValueX,
            labels = classicLabels.map { it to sectionTitlePaint } +
                listOf(text(R.string.total_label), text(R.string.balance_due_label)).map { it to totalPaint }
        )

        pager.canvas.drawText(text(R.string.subtotal_label), totalX, yPos, sectionTitlePaint)
        pager.canvas.drawText(money(currencySymbol, invoice.subtotal), totalValueX, yPos, bodyPaint)
        yPos += paints.advance(bodyPaint, 15f)

        classicRows.forEach { (label, value) ->
            pager.canvas.drawText(label, totalX, yPos, sectionTitlePaint)
            pager.canvas.drawText(value, totalValueX, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 15f)
        }

        pager.canvas.drawText(text(R.string.total_label), totalX, yPos, totalPaint)
        pager.canvas.drawText(money(currencySymbol, invoice.totalAmount), totalValueX, yPos, totalPaint)

        if (isPartlyPaid) {
            yPos += paints.advance(totalPaint, 15f)
            pager.canvas.drawText(text(R.string.paid_label), totalX, yPos, sectionTitlePaint)
            pager.canvas.drawText("-${money(currencySymbol, invoice.amountPaid)}", totalValueX, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 15f)
            pager.canvas.drawText(text(R.string.balance_due_label), totalX, yPos, totalPaint)
            pager.canvas.drawText(
                money(currencySymbol, InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)),
                totalValueX, yPos, totalPaint
            )
        }
        yPos += paints.advance(totalPaint, 40f)

        yPos = drawInvoiceNotes(
            pager, paints, invoice.notes, text(R.string.pdf_notes_title_classic), margin, yPos,
            maxWidth = pageWidth - 2 * margin, titlePaint = sectionTitlePaint, bodyPaint = bodyPaint,
            titleStep = 18f, lineStep = 15f, gapAfter = 25f
        )

        // Business signature
        yPos = drawSignature(pager, paints, signaturePath, businessInfo.ownerName, yPos, maxHeight = 40f, fallbackTextSize = 18f)
    }

    private fun drawModernTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?,
        paymentLinks: PaymentLinks
    ) {
        val labels = labelsFor(invoice)
        val currencySymbol = getCurrencySymbol(currency)
        var yPos = margin

        val sectionTitlePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, tracking = 0.05f)

        val bodyPaint = paints.text(9f)

        val bodySmallPaint = paints.text(8f, color = paints.style.muted)

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        drawImageAt(pager.canvas, it, margin, yPos, logoWidth, logoHeight)
                        yPos += logoHeight + 20f
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Business Information (left) and Invoice # + Dates (right)
        val topSectionStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            val businessNamePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold)
            pager.canvas.drawText(businessInfo.businessName, margin, leftYPos, businessNamePaint)
            leftYPos += paints.advance(businessNamePaint, 13f)

            if (businessInfo.ownerName.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            if (businessInfo.address.isNotEmpty()) {
                pager.canvas.drawText(businessInfo.address, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
            if (cityStateZip.isNotEmpty()) {
                pager.canvas.drawText(cityStateZip, margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
            if (businessInfo.taxId.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.tax_id_label, businessInfo.taxId), margin, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 11f)
            }
        }

        // Right side - Invoice number and dates
        var rightYPos = topSectionStart
        val rightX = pageWidth - margin - 120f

        val invoiceTitlePaint = paints.text(12f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        pager.canvas.drawText(text(labels.numberFormat, invoice.number), rightX, rightYPos, invoiceTitlePaint)
        rightYPos += paints.advance(invoiceTitlePaint, 15f)

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        val formattedDueDate = InvoiceDates.format(invoice.dueDate, dateFormat)

        pager.canvas.drawText(text(R.string.date_label), rightX, rightYPos, sectionTitlePaint)
        rightYPos += paints.advance(sectionTitlePaint, 12f)
        pager.canvas.drawText(formattedCreatedDate, rightX, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 15f)

        pager.canvas.drawText(text(labels.dueDate), rightX, rightYPos, sectionTitlePaint)
        rightYPos += paints.advance(sectionTitlePaint, 12f)
        pager.canvas.drawText(formattedDueDate, rightX, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Draw horizontal line separator
        val linePaint = paints.stroke(width = 0.5f)
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // BILLED TO section
        leftYPos = yPos
        pager.canvas.drawText(text(R.string.billed_to_label), margin, leftYPos, bodySmallPaint)
        leftYPos += paints.advance(bodySmallPaint, 15f)
        pager.canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 12f)
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.notes_label, client.notes), margin, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 12f)
        }

        yPos = leftYPos + 20f

        // Draw horizontal line separator
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        // Table Header
        val tableTitlePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, tracking = 0.1f)

        val pricePaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, align = Paint.Align.RIGHT, tracking = 0.1f)
        val drawTableHeader: (Float) -> Float = { start ->
            pager.canvas.drawText(text(R.string.description_label), margin, start, tableTitlePaint)
            pager.canvas.drawText(text(R.string.rate_label), pageWidth - margin - 280f, start, tableTitlePaint)
            pager.canvas.drawText(text(R.string.qty_label), pageWidth - margin - 200f, start, tableTitlePaint)
            pager.canvas.drawText(text(R.string.price_label), pageWidth - margin, start, pricePaint)
            start + paints.advance(pricePaint, 20f)
        }
        yPos = drawTableHeader(yPos)

        // Table rows
        val tableBodyPaint = paints.text(9f)

        val priceBodyPaint = paints.text(9f, align = Paint.Align.RIGHT)

        val descriptionPaint = paints.text(8f, color = paints.style.muted)

        val modernRowHeight = paints.advance(tableBodyPaint, 12f) + paints.advance(descriptionPaint, 12f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, modernRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - margin - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, tableBodyPaint, descWidth),
                margin, yPos, tableBodyPaint
            )
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 280f, yPos, tableBodyPaint)
            pager.canvas.drawText(paints.truncate(quantityText(item), tableBodyPaint, 75f), pageWidth - margin - 200f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin, yPos, priceBodyPaint)
            yPos += paints.advance(tableBodyPaint, 12f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 12f)
            } else {
                yPos += paints.advance(descriptionPaint, 6f)
            }
        }

        yPos += 30f

        // Draw horizontal line before totals
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 25f

        val modernRows = adjustmentRows(
            invoice, currencySymbol,
            discountLabel = { rate -> rate?.let { text(R.string.discount_rate_label, it) } ?: text(R.string.discount_label) },
            taxLabel = { rate -> rate?.let { text(R.string.vat_rate_label, it) } ?: text(R.string.vat_label) },
            shippingLabel = text(R.string.shipping_label)
        )
        val isPartlyPaid = invoice.amountPaid > 0
        yPos = pager.flowTo(yPos, 150f + 15f * (modernRows.size + if (isPartlyPaid) 2 else 0))

        // Totals section (right aligned)
        val totalLabelPaint = paints.text(9f)

        val totalValuePaint = paints.text(9f, align = Paint.Align.RIGHT)

        val amountDuePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)

        val amountDueValuePaint = paints.text(10f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.RIGHT)

        // Com pagamento, "valor a pagar" passa a ser o saldo, e o total aparece numa linha comum.
        val amountDue = if (isPartlyPaid) {
            InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)
        } else {
            invoice.totalAmount
        }
        val modernValues = listOf(invoice.subtotal, invoice.totalAmount, invoice.amountPaid, amountDue)
            .map { money(currencySymbol, it) } + modernRows.map { it.second }
        val totalsX = totalsLabelX(
            designedX = pageWidth - margin - 180f,
            valueStartX = pageWidth - margin - (modernValues.maxOfOrNull { amountDueValuePaint.measureText("-$it") } ?: 0f),
            labels = (listOf(text(R.string.subtotal_label), text(R.string.total_label), text(R.string.paid_label)) +
                modernRows.map { it.first }).map { it to totalLabelPaint } +
                (text(labels.amountDue) to amountDuePaint)
        )

        pager.canvas.drawText(text(R.string.subtotal_label), totalsX, yPos, totalLabelPaint)
        pager.canvas.drawText(money(currencySymbol, invoice.subtotal), pageWidth - margin, yPos, totalValuePaint)
        yPos += paints.advance(totalLabelPaint, 15f)

        modernRows.forEach { (label, value) ->
            pager.canvas.drawText(label, totalsX, yPos, totalLabelPaint)
            pager.canvas.drawText(value, pageWidth - margin, yPos, totalValuePaint)
            yPos += paints.advance(totalLabelPaint, 15f)
        }

        if (isPartlyPaid) {
            pager.canvas.drawText(text(R.string.total_label), totalsX, yPos, totalLabelPaint)
            pager.canvas.drawText(money(currencySymbol, invoice.totalAmount), pageWidth - margin, yPos, totalValuePaint)
            yPos += paints.advance(totalLabelPaint, 15f)
            pager.canvas.drawText(text(R.string.paid_label), totalsX, yPos, totalLabelPaint)
            pager.canvas.drawText("-${money(currencySymbol, invoice.amountPaid)}", pageWidth - margin, yPos, totalValuePaint)
            yPos += paints.advance(totalLabelPaint, 15f)
        }

        pager.canvas.drawText(text(labels.amountDue), totalsX, yPos, amountDuePaint)
        pager.canvas.drawText(money(currencySymbol, amountDue), pageWidth - margin, yPos, amountDueValuePaint)
        yPos += paints.advance(amountDuePaint, 30f)

        yPos = drawInvoiceNotes(
            pager, paints, invoice.notes, text(R.string.pdf_notes_title_modern), margin, yPos,
            maxWidth = pageWidth - 2 * margin, titlePaint = bodySmallPaint, bodyPaint = bodyPaint,
            titleStep = 15f, lineStep = 12f, gapAfter = 20f
        )

        // Payment Information section
        if (hasPaymentBlock(paymentInfo, paymentLinks, paymentQrCodePath)) {
            pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += 20f

            pager.canvas.drawText(text(R.string.payment_information_label), margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 15f)

            if (paymentInfo.bankName.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.bank_label, paymentInfo.bankName), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.account_holder_label, paymentInfo.accountHolderName), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.account_number_label, paymentInfo.accountNumber), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.routing_number_label, paymentInfo.routingNumber), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.iban_label, paymentInfo.iban), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.swift_label, paymentInfo.swiftCode), margin, yPos, bodyPaint)
                yPos += paints.advance(bodyPaint, 12f)
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.bank_address_label, paymentInfo.bankAddress), margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.payment_terms_label, paymentInfo.paymentTerms), margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.additional_instructions_label, paymentInfo.additionalInstructions), margin, yPos, bodySmallPaint)
                yPos += paints.advance(bodySmallPaint, 12f)
            }

            yPos = drawPaymentLinks(
                pager, paints, paymentLinks, margin, yPos,
                maxWidth = pageWidth - 2 * margin, paint = bodyPaint, step = 12f
            )

            // O Modern não reserva espaço para este bloco: o QR vira a página em vez de ser
            // cortado no pé dela.
            yPos = pager.flowTo(yPos, paymentQrHeight(paymentLinks, paymentQrCodePath))
            yPos = drawPaymentQr(pager, paints, paymentLinks, paymentQrCodePath, margin, yPos)
            yPos += 15f
        }

        // Business signature
        yPos = drawSignature(pager, paints, signaturePath, businessInfo.ownerName, yPos, maxHeight = 35f, fallbackTextSize = 14f)

        // Move to bottom section
        // Se o conteúdo já chegou aqui, abre página nova em vez de sobrescrever.
        yPos = pager.reserveFooter(yPos, 100f)

        // Título grande no rodapé, à esquerda
        val invoiceBottomPaint = paints.text(48f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)
        pager.canvas.drawText(text(labels.title), margin, yPos, invoiceBottomPaint)
        yPos += paints.advance(invoiceBottomPaint, 30f)

        // Thank you message
        val thankYouPaint = paints.text(8f, color = paints.style.muted, tracking = 0.1f)
        pager.canvas.drawText(text(R.string.thank_you_label), margin, yPos, thankYouPaint)
        yPos += paints.advance(thankYouPaint, 15f)

        // Contact information at bottom
        val contactPaint = paints.text(7f)

        if (businessInfo.phone.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.helpdesk_label, businessInfo.phone), margin, yPos, contactPaint)
            yPos += paints.advance(contactPaint, 10f)
        }
        if (businessInfo.email.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.email_label, businessInfo.email), margin, yPos, contactPaint)
            yPos += paints.advance(contactPaint, 10f)
        }
        if (businessInfo.website.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.web_label, businessInfo.website), margin, yPos, contactPaint)
        }
    }

    private fun drawProfessionalTemplate(
        pager: InvoicePager,
        paints: InvoicePaints,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?,
        paymentQrCodePath: String?,
        paymentLinks: PaymentLinks
    ) {
        val labels = labelsFor(invoice)
        val currencySymbol = getCurrencySymbol(currency)
        var yPos = margin

        val bodyPaint = paints.text(9f)

        // TODO Fase C: o Professional usa DKGRAY aqui e GRAY na descrição do item;
        // normalizar muda pixel, então fica para quando a cor virar configurável.
        val bodySmallPaint = paints.text(8f, color = Color.DKGRAY)

        val labelPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold)

        // Draw logo if exists
        logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        val maxLogoHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        var logoHeight = maxLogoHeight
                        // A largura era irrestrita: um logo 10:1 invadia a coluna direita.
                        val maxLogoWidth = (pageWidth - 2 * margin) / 3f
                        var logoWidth = logoHeight * aspectRatio
                        if (logoWidth > maxLogoWidth) {
                            logoHeight = maxLogoWidth / aspectRatio
                            logoWidth = maxLogoWidth
                        }

                        drawImageAt(pager.canvas, it, margin, yPos, logoWidth, logoHeight)
                        yPos += logoHeight + 20f
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            }
        }

        // Business info at top left
        if (businessInfo.businessName.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.businessName, margin, yPos, bodyPaint)
            yPos += paints.advance(bodyPaint, 12f)
        }
        if (businessInfo.ownerName.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.ownerName, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.email.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.email, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.phone.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.phone, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.address.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.address, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        val cityStateZip = cityStateZip(businessInfo.city, businessInfo.state, businessInfo.zipCode)
        if (cityStateZip.isNotEmpty()) {
            pager.canvas.drawText(cityStateZip, margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }
        if (businessInfo.taxId.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.tax_id_label, businessInfo.taxId), margin, yPos, bodySmallPaint)
            yPos += paints.advance(bodySmallPaint, 10f)
        }

        yPos += paints.advance(bodySmallPaint, 20f)

        // Título grande, centralizado
        val titlePaint = paints.text(32f, family = InvoicePaints.Family.Display, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.CENTER, tracking = 0.3f)
        pager.canvas.drawText(text(labels.title), pageWidth / 2f, yPos, titlePaint)
        yPos += paints.advance(titlePaint, 50f)

        // Two columns: Issued to (left) and Invoice info (right)
        val leftX = margin
        val rightX = pageWidth - margin - 150f
        var leftYPos = yPos
        var rightYPos = yPos

        // Left column - Issued to
        pager.canvas.drawText(text(R.string.issued_to_professional), leftX, leftYPos, labelPaint)
        leftYPos += paints.advance(labelPaint, 12f)
        pager.canvas.drawText(client.name, leftX, leftYPos, bodyPaint)
        leftYPos += paints.advance(bodyPaint, 11f)
        if (client.email.isNotEmpty()) {
            pager.canvas.drawText(client.email, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.phone.isNotEmpty()) {
            pager.canvas.drawText(client.phone, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.address.isNotEmpty()) {
            pager.canvas.drawText(client.address, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        val clientCityStateZip = cityStateZip(client.city, client.state, client.zipCode)
        if (clientCityStateZip.isNotEmpty()) {
            pager.canvas.drawText(clientCityStateZip, leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }
        if (client.notes.isNotEmpty()) {
            pager.canvas.drawText(text(R.string.notes_label, client.notes), leftX, leftYPos, bodySmallPaint)
            leftYPos += paints.advance(bodySmallPaint, 11f)
        }

        // Right column - Invoice No and Dates
        val labelGap = paints.labelGap(
            labelPaint,
            listOf(
                text(labels.numberProfessional),
                text(R.string.date_issued_label),
                text(labels.dueDateProfessional)
            ),
            80f
        )
        pager.canvas.drawText(text(labels.numberProfessional), rightX, rightYPos, labelPaint)
        pager.canvas.drawText("#${invoice.number}", rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 12f)

        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))
        pager.canvas.drawText(text(R.string.date_issued_label), rightX, rightYPos, labelPaint)
        pager.canvas.drawText(formattedCreatedDate, rightX + labelGap, rightYPos, bodyPaint)
        rightYPos += paints.advance(bodyPaint, 12f)

        val formattedDueDate = InvoiceDates.format(invoice.dueDate, dateFormat)
        pager.canvas.drawText(text(labels.dueDateProfessional), rightX, rightYPos, labelPaint)
        pager.canvas.drawText(formattedDueDate, rightX + labelGap, rightYPos, bodyPaint)

        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Table with black header
        val tableHeaderPaint = paints.fill(paints.style.accent)

        val tableHeaderTextPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, color = paints.style.onAccent)

        val tableBodyPaint = paints.text(9f)

        val headerHeight = maxOf(25f, paints.lineHeight(tableHeaderTextPaint) + 12f)
        val totalHeaderPaint = paints.text(8f, weight = InvoicePaints.Weight.Bold, color = paints.style.onAccent, align = Paint.Align.RIGHT)
        val drawTableHeader: (Float) -> Float = { start ->
            pager.canvas.drawRect(margin, start, pageWidth - margin, start + headerHeight, tableHeaderPaint)
            val headerY = start + 16f
            pager.canvas.drawText(text(R.string.description_header), margin + 10f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText(text(R.string.quantity_header), pageWidth - margin - 280f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText(text(R.string.unit_price_header), pageWidth - margin - 180f, headerY, tableHeaderTextPaint)
            pager.canvas.drawText(text(R.string.total_header), pageWidth - margin - 10f, headerY, totalHeaderPaint)
            start + headerHeight + 15f
        }
        yPos = drawTableHeader(yPos)

        // Table rows
        val descriptionPaint = paints.text(8f, color = paints.style.muted)

        val totalPaint = paints.text(9f, align = Paint.Align.RIGHT)
        val professionalRowHeight = paints.advance(tableBodyPaint, 14f) + paints.advance(descriptionPaint, 12f)
        items.forEach { item ->
            yPos = pager.flowTo(yPos, professionalRowHeight, drawTableHeader)
            val descWidth = (pageWidth - margin - 280f) - (margin + 10f) - 12f
            pager.canvas.drawText(
                paints.truncate(item.productServiceName, tableBodyPaint, descWidth),
                margin + 10f, yPos, tableBodyPaint
            )
            pager.canvas.drawText(paints.truncate(quantityText(item), tableBodyPaint, 90f), pageWidth - margin - 280f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}",
                pageWidth - margin - 180f, yPos, tableBodyPaint)
            pager.canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}",
                pageWidth - margin - 10f, yPos, totalPaint)
            yPos += paints.advance(tableBodyPaint, 12f)
            if (item.productServiceDescription.isNotEmpty()) {
                pager.canvas.drawText(
                    paints.truncate(item.productServiceDescription, descriptionPaint, descWidth),
                    margin + 10f, yPos, descriptionPaint
                )
                yPos += paints.advance(descriptionPaint, 14f)
            } else {
                yPos += paints.advance(descriptionPaint, 8f)
            }
        }

        yPos += 20f

        // Horizontal line separator
        val linePaint = paints.stroke(width = 1f)
        pager.canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 30f

        val professionalRows = adjustmentRows(
            invoice, currencySymbol,
            discountLabel = { rate -> rate?.let { text(R.string.discount_rate_colon, it) } ?: text(R.string.discount_colon) },
            taxLabel = { rate -> rate?.let { text(R.string.tax_rate_colon, it) } ?: text(R.string.tax_colon) },
            shippingLabel = text(R.string.shipping_colon)
        )
        val isPartlyPaid = invoice.amountPaid > 0

        // Bottom section: Payment info (left) and Totals (right).
        // O bloco inteiro migra de página se não couber.
        yPos = pager.flowTo(
            yPos,
            200f + 12f * (professionalRows.size + if (isPartlyPaid) 2 else 0) +
                paymentLinksExtraHeight(paymentLinks, lineStep = 10f)
        )
        leftYPos = yPos
        rightYPos = yPos

        // Left - Payment Info
        if (hasPaymentBlock(paymentInfo, paymentLinks, paymentQrCodePath)) {
            pager.canvas.drawText(text(R.string.payment_info_label), leftX, leftYPos, labelPaint)
            leftYPos += paints.advance(labelPaint, 12f)
            if (paymentInfo.bankName.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankName, leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.bankAddress.isNotEmpty()) {
                pager.canvas.drawText(paymentInfo.bankAddress, leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.account_name_label, paymentInfo.accountHolderName), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.account_no_label, paymentInfo.accountNumber), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.routing_label, paymentInfo.routingNumber), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.iban.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.iban_label, paymentInfo.iban), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.swift_label, paymentInfo.swiftCode), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.terms_label, paymentInfo.paymentTerms), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                pager.canvas.drawText(text(R.string.notes_label, paymentInfo.additionalInstructions), leftX, leftYPos, bodySmallPaint)
                leftYPos += paints.advance(bodySmallPaint, 10f)
            }

            leftYPos = drawPaymentLinks(
                pager, paints, paymentLinks, leftX, leftYPos,
                // Até a coluna dos totais, com folga.
                maxWidth = pageWidth - margin - 170f - leftX, paint = bodySmallPaint, step = 10f
            )

            leftYPos = drawPaymentQr(pager, paints, paymentLinks, paymentQrCodePath, leftX, leftYPos)
        }

        // Right - Totals
        val totalsLabelPaint = paints.text(9f)

        val totalsValuePaint = paints.text(9f, align = Paint.Align.RIGHT)

        val totalBoldPaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent)

        val totalBoldValuePaint = paints.text(11f, weight = InvoicePaints.Weight.Bold, color = paints.style.accent, align = Paint.Align.RIGHT)

        val balanceDue = InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)
        val professionalValues = listOf(invoice.subtotal, invoice.totalAmount, invoice.amountPaid, balanceDue)
            .map { money(currencySymbol, it) } + professionalRows.map { it.second }
        val totalsX = totalsLabelX(
            designedX = pageWidth - margin - 150f,
            valueStartX = pageWidth - margin - 10f -
                (professionalValues.maxOfOrNull { totalBoldValuePaint.measureText("-$it") } ?: 0f),
            labels = (listOf(text(R.string.subtotal_colon), text(R.string.paid_colon)) +
                professionalRows.map { it.first }).map { it to totalsLabelPaint } +
                listOf(text(R.string.total_colon), text(R.string.balance_due_colon)).map { it to totalBoldPaint }
        )

        pager.canvas.drawText(text(R.string.subtotal_colon), totalsX, rightYPos, totalsLabelPaint)
        pager.canvas.drawText(money(currencySymbol, invoice.subtotal),
            pageWidth - margin - 10f, rightYPos, totalsValuePaint)
        rightYPos += paints.advance(totalsLabelPaint, 12f)

        professionalRows.forEach { (label, value) ->
            pager.canvas.drawText(label, totalsX, rightYPos, totalsLabelPaint)
            pager.canvas.drawText(value, pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += paints.advance(totalsLabelPaint, 12f)
        }

        pager.canvas.drawText(text(R.string.total_colon), totalsX, rightYPos, totalBoldPaint)
        pager.canvas.drawText(money(currencySymbol, invoice.totalAmount),
            pageWidth - margin - 10f, rightYPos, totalBoldValuePaint)

        if (isPartlyPaid) {
            rightYPos += paints.advance(totalBoldPaint, 14f)
            pager.canvas.drawText(text(R.string.paid_colon), totalsX, rightYPos, totalsLabelPaint)
            pager.canvas.drawText("-${money(currencySymbol, invoice.amountPaid)}",
                pageWidth - margin - 10f, rightYPos, totalsValuePaint)
            rightYPos += paints.advance(totalsLabelPaint, 12f)
            pager.canvas.drawText(text(R.string.balance_due_colon), totalsX, rightYPos, totalBoldPaint)
            pager.canvas.drawText(money(currencySymbol, balanceDue),
                pageWidth - margin - 10f, rightYPos, totalBoldValuePaint)
        }

        // Signature
        yPos = maxOf(leftYPos, rightYPos) + 30f
        yPos = drawInvoiceNotes(
            pager, paints, invoice.notes, text(R.string.pdf_notes_title_professional), leftX, yPos,
            maxWidth = pageWidth - 2 * margin, titlePaint = labelPaint, bodyPaint = bodySmallPaint,
            titleStep = 12f, lineStep = 11f, gapAfter = 25f
        )
        yPos = drawSignature(pager, paints, signaturePath, businessInfo.ownerName, yPos, maxHeight = 35f, fallbackTextSize = 14f)

        // Website at bottom center
        yPos = pager.reserveFooter(yPos, 20f)
        val websitePaint = paints.text(8f, align = Paint.Align.CENTER)
        if (businessInfo.website.isNotEmpty()) {
            pager.canvas.drawText(businessInfo.website, pageWidth / 2f, yPos, websitePaint)
        }
    }
}
