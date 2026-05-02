package lt.skautai.android.util

import lt.skautai.android.data.remote.ItemDto

data class PrintableQrItem(
    val itemId: String,
    val title: String,
    val subtitle: String,
    val payload: String
)

fun ItemDto.toPrintableQrItemOrNull(): PrintableQrItem? {
    if (id.startsWith("local-") || qrToken.isBlank()) return null

    val subtitle = locationPath?.takeIf { it.isNotBlank() }
        ?: locationName?.takeIf { it.isNotBlank() }
        ?: "Nenurodyta"

    return PrintableQrItem(
        itemId = id,
        title = name,
        subtitle = subtitle,
        payload = QrPayload.forScanToken(qrToken)
    )
}
