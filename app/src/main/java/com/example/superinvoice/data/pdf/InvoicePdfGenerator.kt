package com.example.superinvoice.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.superinvoice.data.Client
import com.example.superinvoice.data.Invoice
import com.example.superinvoice.data.InvoiceItem
import com.example.superinvoice.util.getCurrencySymbol
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pageWidth = 595  // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 60f

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

    private fun reformatDateIfNeeded(dateString: String, targetFormat: String): String {
        if (dateString.isEmpty()) return dateString

        // Lista de formatos conhecidos para tentar parsear
        val knownFormats = listOf(
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd-MM-yyyy",
            "MMMM dd, yyyy",
            "dd MMMM yyyy",
            "MMM dd, yyyy",
            "MMM, dd"  // Formato antigo
        )

        // Tentar parsear com cada formato conhecido
        for (format in knownFormats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                parser.isLenient = false
                val date = parser.parse(dateString)

                // Se conseguiu parsear, reformatar com o formato alvo
                if (date != null) {
                    val formatter = SimpleDateFormat(targetFormat, Locale.getDefault())
                    return formatter.format(date)
                }
            } catch (e: Exception) {
                // Continuar tentando outros formatos
                continue
            }
        }

        // Se não conseguiu parsear com nenhum formato, retornar a string original
        return dateString
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
        signaturePath: String? = null
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw the invoice content
            drawClassicTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, dateFormat, logoPath, signaturePath)

            pdfDocument.finishPage(page)

            // Save to file
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "Invoice_${invoice.number}.pdf")

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawClassicTemplate(
        canvas: Canvas,
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String,
        dateFormat: String,
        logoPath: String?,
        signaturePath: String?
    ) {
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
                        val logoHeight = maxLogoHeight
                        val logoWidth = logoHeight * aspectRatio

                        // Draw logo on the left
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            logoWidth.toInt(),
                            logoHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += logoHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Title with line
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.15f
        }

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }

        // Draw line
        canvas.drawLine(margin, yPos + 15, pageWidth - 200f, yPos + 15, linePaint)

        // Draw INVOICE text
        canvas.drawText("INVOICE", pageWidth - 180f, yPos + 20, titlePaint)
        yPos += 60f

        // Section: ISSUED TO and Invoice Info
        val sectionTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Left column - FROM (Business Information)
        val leftYStart = yPos
        var leftYPos = yPos

        if (businessInfo.businessName.isNotEmpty()) {
            canvas.drawText("FROM:", margin, leftYPos, sectionTitlePaint)
            leftYPos += 18f
            canvas.drawText(businessInfo.businessName, margin, leftYPos, bodyPaint)
            leftYPos += 15f
            if (businessInfo.ownerName.isNotEmpty()) {
                canvas.drawText(businessInfo.ownerName, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.email.isNotEmpty()) {
                canvas.drawText(businessInfo.email, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.phone.isNotEmpty()) {
                canvas.drawText(businessInfo.phone, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.website.isNotEmpty()) {
                canvas.drawText(businessInfo.website, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.address.isNotEmpty()) {
                canvas.drawText(businessInfo.address, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            val cityStateZip = buildString {
                if (businessInfo.city.isNotEmpty()) append(businessInfo.city)
                if (businessInfo.state.isNotEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(businessInfo.state)
                }
                if (businessInfo.zipCode.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(businessInfo.zipCode)
                }
            }
            if (cityStateZip.isNotEmpty()) {
                canvas.drawText(cityStateZip, margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
            if (businessInfo.taxId.isNotEmpty()) {
                canvas.drawText("Tax ID: ${businessInfo.taxId}", margin, leftYPos, bodyPaint)
                leftYPos += 15f
            }
        }

        // Right column - INVOICE INFO
        var rightYPos = yPos
        val rightX = pageWidth - margin - 150f

        canvas.drawText("INVOICE NO:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(invoice.number, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        // Format createdDate (Long timestamp) for DATE
        val formattedCreatedDate = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(invoice.createdDate))

        // Reformat dueDate (String) for DUE DATE
        val formattedDueDate = reformatDateIfNeeded(invoice.dueDate, dateFormat)

        canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(formattedCreatedDate, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(formattedDueDate, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        // Advance to the max height of both columns
        yPos = maxOf(leftYPos, rightYPos) + 25f

        // Second row: ISSUED TO (left) and PAY TO (right)
        leftYPos = yPos
        rightYPos = yPos

        // ISSUED TO (left)
        canvas.drawText("ISSUED TO:", margin, leftYPos, sectionTitlePaint)
        leftYPos += 18f
        canvas.drawText(client.name, margin, leftYPos, bodyPaint)
        leftYPos += 15f
        if (client.email.isNotEmpty()) {
            canvas.drawText(client.email, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }
        if (client.phone.isNotEmpty()) {
            canvas.drawText(client.phone, margin, leftYPos, bodyPaint)
            leftYPos += 15f
        }

        // PAY TO (right)
        val payRightX = pageWidth / 2f + 20f
        if (paymentInfo.bankName.isNotEmpty()) {
            canvas.drawText("PAY TO:", payRightX, rightYPos, sectionTitlePaint)
            rightYPos += 18f
            canvas.drawText(paymentInfo.bankName, payRightX, rightYPos, bodyPaint)
            rightYPos += 15f
            if (paymentInfo.bankAddress.isNotEmpty()) {
                canvas.drawText(paymentInfo.bankAddress, payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                canvas.drawText("Acc Name: ${paymentInfo.accountHolderName}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                canvas.drawText("Acc Number: ${paymentInfo.accountNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.routingNumber.isNotEmpty()) {
                canvas.drawText("Routing: ${paymentInfo.routingNumber}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.iban.isNotEmpty()) {
                canvas.drawText("IBAN: ${paymentInfo.iban}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.swiftCode.isNotEmpty()) {
                canvas.drawText("SWIFT: ${paymentInfo.swiftCode}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.paymentTerms.isNotEmpty()) {
                canvas.drawText("Terms: ${paymentInfo.paymentTerms}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
            if (paymentInfo.additionalInstructions.isNotEmpty()) {
                canvas.drawText("Notes: ${paymentInfo.additionalInstructions}", payRightX, rightYPos, bodyPaint)
                rightYPos += 15f
            }
        }

        // Advance to max height
        yPos = maxOf(leftYPos, rightYPos) + 30f

        // Divider line before table
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // Table Header
        canvas.drawText("DESCRIPTION", margin, yPos, sectionTitlePaint)
        canvas.drawText("UNIT PRICE", pageWidth - margin - 280f, yPos, sectionTitlePaint)
        canvas.drawText("QTY", pageWidth - margin - 150f, yPos, sectionTitlePaint)
        canvas.drawText("TOTAL", pageWidth - margin - 80f, yPos, sectionTitlePaint)

        yPos += 20f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 15f

        // Table Items
        val currencySymbol = getCurrencySymbol(currency)
        items.forEach { item ->
            canvas.drawText(item.productServiceName, margin, yPos, bodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.pricePerUnit)}", pageWidth - margin - 280f, yPos, bodyPaint)
            canvas.drawText("${item.quantity}", pageWidth - margin - 150f, yPos, bodyPaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", item.lineTotal)}", pageWidth - margin - 80f, yPos, bodyPaint)
            yPos += 20f
        }

        yPos += 20f

        // Totals
        val totalX = pageWidth - margin - 180f
        val totalValueX = pageWidth - margin - 80f

        canvas.drawText("SUBTOTAL", totalX, yPos, sectionTitlePaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.subtotal)}", totalValueX, yPos, bodyPaint)
        yPos += 15f

        if (invoice.tax > 0) {
            canvas.drawText("TAX", totalX, yPos, sectionTitlePaint)
            canvas.drawText("$currencySymbol${String.format("%.2f", invoice.tax)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        if (invoice.discount > 0) {
            canvas.drawText("DISCOUNT", totalX, yPos, sectionTitlePaint)
            canvas.drawText("-$currencySymbol${String.format("%.2f", invoice.discount)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        val totalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("TOTAL", totalX, yPos, totalPaint)
        canvas.drawText("$currencySymbol${String.format("%.2f", invoice.totalAmount)}", totalValueX, yPos, totalPaint)
        yPos += 40f

        // Business signature
        signaturePath?.let { path ->
            try {
                val signatureFile = File(path)
                if (signatureFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    bitmap?.let {
                        // Calculate signature dimensions (max height 40, maintain aspect ratio)
                        val maxSignatureHeight = 40f
                        val aspectRatio = it.width.toFloat() / it.height.toFloat()
                        val signatureHeight = maxSignatureHeight
                        val signatureWidth = signatureHeight * aspectRatio

                        // Draw signature on the left
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            it,
                            signatureWidth.toInt(),
                            signatureHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, margin, yPos, null)
                        yPos += signatureHeight + 20f
                        scaledBitmap.recycle()
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to text signature if image fails
                if (businessInfo.ownerName.isNotEmpty()) {
                    val signaturePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
                }
            }
        } ?: run {
            // No signature image, use text signature if owner name exists
            if (businessInfo.ownerName.isNotEmpty()) {
                val signaturePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText(businessInfo.ownerName, margin, yPos, signaturePaint)
            }
        }
    }
}
