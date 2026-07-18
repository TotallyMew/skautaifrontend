package lt.skautai.android.util

import android.net.Uri

sealed interface QrDestination {
    data class ScanToken(val token: String) : QrDestination
    data object Unknown : QrDestination
}

object QrPayload {
    private val rawTokenRegex = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{7,127}$")

    fun forScanToken(token: String): String = "skautai://scan/$token"

    fun parse(rawValue: String?): QrDestination {
        val value = rawValue?.trim().orEmpty()
        if (value.isBlank()) return QrDestination.Unknown

        if (rawTokenRegex.matches(value) && value.contains('-')) {
            return QrDestination.ScanToken(value)
        }

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
