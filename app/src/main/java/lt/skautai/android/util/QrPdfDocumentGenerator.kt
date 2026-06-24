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
    private const val pagePadding = 24
    private const val gutter = 16
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    fun createPdf(
        cacheDir: File,
        items: List<PrintableQrItem>,
        layout: QrPdfLayout = QrPdfLayout.Standard
    ): File {
        require(items.isNotEmpty()) { "Bent vienas QR elementas yra privalomas" }

        val outputDir = File(cacheDir, "shared-qr-pdfs").apply { mkdirs() }
        val outputFile = File(
            outputDir,
            "inventorius-qr-${layout.fileSuffix}-${LocalDateTime.now().format(fileNameFormatter)}.pdf"
        )

        val document = PdfDocument()
        val cellWidth = (pageWidth - (pagePadding * 2) - (gutter * (layout.columns - 1))) / layout.columns
        val cellHeight = (pageHeight - (pagePadding * 2) - (gutter * (layout.rows - 1))) / layout.rows
        val maxItemsPerPage = layout.columns * layout.rows

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
                    val column = index % layout.columns
                    val row = index / layout.columns
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

                    val contentLeft = left + layout.cellPadding
                    val contentTop = top + layout.cellPadding
                    val contentWidth = cellWidth - (layout.cellPadding * 2)
                    val qrSize = min(
                        contentWidth,
                        max(layout.minQrSize, (cellHeight * layout.qrHeightFraction).toInt())
                    )
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

enum class QrPdfLayout(
    val label: String,
    val description: String,
    val qrSizeLabel: String,
    val fileSuffix: String,
    val columns: Int,
    val rows: Int,
    val cellPadding: Int,
    val minQrSize: Int,
    val qrHeightFraction: Float
) {
    Small(
        label = "Maži (~2 cm QR)",
        description = "Smulkiems įrankiams ir mažoms etiketėms",
        qrSizeLabel = "QR apie 2 cm, etiketė apie 4,4 x 3 cm",
        fileSuffix = "mazi",
        columns = 4,
        rows = 8,
        cellPadding = 8,
        minQrSize = 58,
        qrHeightFraction = 0.62f
    ),
    Standard(
        label = "Standartiniai (~3,3 cm QR)",
        description = "Bendram inventoriui",
        qrSizeLabel = "QR apie 3,3 cm, etiketė apie 9,4 x 6,6 cm",
        fileSuffix = "standartiniai",
        columns = 2,
        rows = 4,
        cellPadding = 14,
        minQrSize = 92,
        qrHeightFraction = 0.5f
    ),
    Large(
        label = "Dideli (~8,9 cm QR)",
        description = "Palapinėms, dėžėms ir didesniems daiktams",
        qrSizeLabel = "QR apie 8,9 cm, etiketė apie 19,3 x 13,7 cm",
        fileSuffix = "dideli",
        columns = 1,
        rows = 2,
        cellPadding = 22,
        minQrSize = 180,
        qrHeightFraction = 0.65f
    )
}
