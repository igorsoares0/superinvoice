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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
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
        val paymentTerms: String = ""
    )

    fun generateInvoicePdf(
        invoice: Invoice,
        client: Client,
        items: List<InvoiceItem>,
        businessInfo: BusinessInfo,
        paymentInfo: PaymentInfo,
        currency: String = "USD",
        logoPath: String? = null,
        signaturePath: String? = null
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw the invoice content
            drawClassicTemplate(canvas, invoice, client, items, businessInfo, paymentInfo, currency, logoPath, signaturePath)

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

        // ISSUED TO
        canvas.drawText("ISSUED TO:", margin, yPos, sectionTitlePaint)
        yPos += 18f
        canvas.drawText(client.name, margin, yPos, bodyPaint)
        yPos += 15f
        if (client.email.isNotEmpty()) {
            canvas.drawText(client.email, margin, yPos, bodyPaint)
            yPos += 15f
        }
        if (client.phone.isNotEmpty()) {
            canvas.drawText(client.phone, margin, yPos, bodyPaint)
        }

        // Invoice info (right side)
        var rightYPos = margin + 40f
        val rightX = pageWidth - margin - 150f

        canvas.drawText("INVOICE NO:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(invoice.number, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        canvas.drawText("DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(invoice.dueDate, rightX + 100f, rightYPos, bodyPaint)
        rightYPos += 15f

        canvas.drawText("DUE DATE:", rightX, rightYPos, sectionTitlePaint)
        canvas.drawText(invoice.dueDate, rightX + 100f, rightYPos, bodyPaint)

        yPos += 40f

        // PAY TO Section
        if (paymentInfo.bankName.isNotEmpty()) {
            canvas.drawText("PAY TO:", margin, yPos, sectionTitlePaint)
            yPos += 18f
            canvas.drawText(paymentInfo.bankName, margin, yPos, bodyPaint)
            yPos += 15f
            if (paymentInfo.accountHolderName.isNotEmpty()) {
                canvas.drawText("Account Name: ${paymentInfo.accountHolderName}", margin, yPos, bodyPaint)
                yPos += 15f
            }
            if (paymentInfo.accountNumber.isNotEmpty()) {
                canvas.drawText("Account Number: ${paymentInfo.accountNumber}", margin, yPos, bodyPaint)
                yPos += 15f
            }
        }

        yPos += 30f

        // Table Header
        canvas.drawText("DESCRIPTION", margin, yPos, sectionTitlePaint)
        canvas.drawText("UNIT PRICE", pageWidth - margin - 280f, yPos, sectionTitlePaint)
        canvas.drawText("QTY", pageWidth - margin - 150f, yPos, sectionTitlePaint)
        canvas.drawText("TOTAL", pageWidth - margin - 80f, yPos, sectionTitlePaint)

        yPos += 20f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 15f

        // Table Items
        items.forEach { item ->
            canvas.drawText(item.productServiceName, margin, yPos, bodyPaint)
            canvas.drawText("$${String.format("%.2f", item.pricePerUnit)}", pageWidth - margin - 280f, yPos, bodyPaint)
            canvas.drawText("${item.quantity}", pageWidth - margin - 150f, yPos, bodyPaint)
            canvas.drawText("$${String.format("%.2f", item.lineTotal)}", pageWidth - margin - 80f, yPos, bodyPaint)
            yPos += 20f
        }

        yPos += 20f

        // Totals
        val totalX = pageWidth - margin - 180f
        val totalValueX = pageWidth - margin - 80f

        canvas.drawText("SUBTOTAL", totalX, yPos, sectionTitlePaint)
        canvas.drawText("$${String.format("%.2f", invoice.subtotal)}", totalValueX, yPos, bodyPaint)
        yPos += 15f

        if (invoice.tax > 0) {
            canvas.drawText("TAX", totalX, yPos, sectionTitlePaint)
            canvas.drawText("$${String.format("%.2f", invoice.tax)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        if (invoice.discount > 0) {
            canvas.drawText("DISCOUNT", totalX, yPos, sectionTitlePaint)
            canvas.drawText("-$${String.format("%.2f", invoice.discount)}", totalValueX, yPos, bodyPaint)
            yPos += 15f
        }

        val totalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("TOTAL", totalX, yPos, totalPaint)
        canvas.drawText("$${String.format("%.2f", invoice.totalAmount)}", totalValueX, yPos, totalPaint)
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

        // Footer with business info
        if (businessInfo.businessName.isNotEmpty()) {
            yPos = pageHeight - margin - 40f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(businessInfo.businessName, margin, yPos, footerPaint)
            yPos += 12f
            if (businessInfo.email.isNotEmpty()) {
                canvas.drawText(businessInfo.email, margin, yPos, footerPaint)
            }
            if (businessInfo.phone.isNotEmpty()) {
                canvas.drawText(" | ${businessInfo.phone}", margin + 150f, yPos, footerPaint)
            }
        }
    }
}
