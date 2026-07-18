package lt.skautai.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

object QrPdfShareLauncher {
    fun share(
        context: Context,
        items: List<PrintableQrItem>,
        layout: QrPdfLayout = QrPdfLayout.Standard,
        codeType: InventoryCodeType = InventoryCodeType.Qr
    ) {
        val pdfFile = QrPdfDocumentGenerator.createPdf(context.cacheDir, items, layout, codeType)
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Dalintis inventoriaus kodų PDF")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
