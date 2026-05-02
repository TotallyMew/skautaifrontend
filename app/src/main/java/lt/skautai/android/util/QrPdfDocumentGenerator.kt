package lt.skautai.android.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

object QrPdfDocumentGenerator {
    private const val pageWidth = 595
    private const val pageHeight = 842
    private const val columns = 2
    private const val rows = 4
    private const val pagePadding = 24
    private const val gutter = 16
    private const val cellPadding = 14
    private const val maxItemsPerPage = columns * rows
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    fun createPdf(cacheDir: File, items: List<PrintableQrItem>): File {
        require(items.isNotEmpty()) { "Bent vienas QR elementas yra privalomas" }

        val outputDir = File(cacheDir, "shared-qr-pdfs").apply { mkdirs() }
        val outputFile = File(
            outputDir,
            "inventorius-qr-${LocalDateTime.now().format(fileNameFormatter)}.pdf"
        )

        val document = PdfDocument()
        val cellWidth = (pageWidth - (pagePadding * 2) - (gutter * (columns - 1))) / columns
        val cellHeight = (pageHeight - (pagePadding * 2) - (gutter * (rows - 1))) / rows

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 214, 224)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 78, 86)
            textSize = 10f
        }

        try {
            items.chunked(maxItemsPerPage).forEachIndexed { pageIndex, pageItems ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                pageItems.forEachIndexed { index, item ->
                    val column = index % columns
                    val row = index / columns
                    val left = pagePadding + column * (cellWidth + gutter)
                    val top = pagePadding + row * (cellHeight + gutter)
                    val right = left + cellWidth
                    val bottom = top + cellHeight

                    canvas.drawRoundRect(
                        left.toFloat(),
                        top.toFloat(),
                        right.toFloat(),
                        bottom.toFloat(),
                        14f,
                        14f,
                        borderPaint
                    )

                    val contentLeft = left + cellPadding
                    val contentTop = top + cellPadding
                    val contentWidth = cellWidth - (cellPadding * 2)
                    val qrSize = min(contentWidth, max(92, cellHeight / 2))
                    val qrBitmap = QrCodeBitmap.create(item.payload, 640)
                    canvas.drawBitmap(qrBitmap, null, android.graphics.Rect(
                        contentLeft,
                        contentTop,
                        contentLeft + qrSize,
                        contentTop + qrSize
                    ), null)

                    val textTop = contentTop + qrSize + 10
                    val titleLayout = staticLayout(
                        text = item.title,
                        paint = titlePaint,
                        width = contentWidth,
                        maxLines = 2
                    )
                    canvas.save()
                    canvas.translate(contentLeft.toFloat(), textTop.toFloat())
                    titleLayout.draw(canvas)
                    canvas.restore()

                    val subtitleLayout = staticLayout(
                        text = item.subtitle,
                        paint = subtitlePaint,
                        width = contentWidth,
                        maxLines = 2
                    )
                    canvas.save()
                    canvas.translate(
                        contentLeft.toFloat(),
                        (textTop + titleLayout.height + 6).toFloat()
                    )
                    subtitleLayout.draw(canvas)
                    canvas.restore()
                }

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
            return outputFile
        } finally {
            document.close()
        }
    }

    private fun staticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
    }
}
