package lt.skautai.android.util

import android.net.Uri

sealed interface QrDestination {
    data class ScanToken(val token: String) : QrDestination
    data object Unknown : QrDestination
}

object QrPayload {
    fun forScanToken(token: String): String = "skautai://scan/$token"

    fun parse(rawValue: String?): QrDestination {
        val value = rawValue?.trim().orEmpty()
        if (value.isBlank()) return QrDestination.Unknown

        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return QrDestination.Unknown
        if (uri.scheme != "skautai") return QrDestination.Unknown

        return when (uri.host) {
            "scan" -> {
                val token = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
                if (token != null) QrDestination.ScanToken(token) else QrDestination.Unknown
            }
            else -> QrDestination.Unknown
        }
    }
}
