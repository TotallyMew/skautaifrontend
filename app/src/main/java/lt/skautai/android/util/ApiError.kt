package lt.skautai.android.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import retrofit2.Response

private data class ErrorEnvelope(
    val error: String? = null,
    val message: String? = null,
    val details: String? = null
)

private val gson = Gson()

private val translations = mapOf(
    "Insufficient permissions" to "Neturite teisių atlikti šį veiksmą.",
    "Not authenticated" to "Prisijungimas baigėsi. Prisijunkite iš naujo.",
    "Invalid token" to "Prisijungimas baigėsi. Prisijunkite iš naujo.",
    "Login failed" to "Neteisingas el. paštas arba slaptažodis.",
    "Registration failed" to "Registracija nepavyko. Patikrinkite įvestus duomenis.",
    "Setup failed" to "Konfigūracija nepavyko.",
    "X-Tuntas-Id header required" to "Tuntas nepasirinktas.",
    "Invalid tuntas ID" to "Neteisingas tuntas.",
    "User ID required" to "Nenurodytas vartotojas.",
    "Invalid user ID" to "Neteisingas vartotojo ID.",
    "Member not found" to "Narys nerastas.",
    "Reservation not found" to "Rezervacija nerasta.",
    "Item not found" to "Inventoriaus objektas nerastas.",
    "Not found" to "Nerasta.",
    "Email already registered" to "Šis el. paštas jau užregistruotas.",
    "Invalid email format" to "Įveskite teisingą el. pašto adresą.",
    "Current password is required" to "Įveskite dabartinį slaptažodį.",
    "Invalid current password" to "Dabartinis slaptažodis neteisingas.",
    "Password updated" to "Slaptažodis pakeistas.",
    "New password must be different" to "Naujas slaptažodis turi skirtis nuo dabartinio.",
    "Invalid phone format" to "Įveskite teisingą telefono numerį.",
    "Invite code already used" to "Pakvietimo kodas jau panaudotas.",
    "Invite code expired" to "Pakvietimo kodas nebegalioja.",
    "Invalid invite code" to "Neteisingas pakvietimo kodas.",
)

private fun httpFallback(code: Int, fallback: String): String = when (code) {
    401 -> "Prisijungimas baigėsi. Prisijunkite iš naujo."
    403 -> "Neturite teisių atlikti šį veiksmą."
    404 -> "Nurodytas objektas nerastas."
    in 500..599 -> "Serverio klaida. Bandykite vėliau."
    else -> fallback
}

private fun JsonElement.stringOrNull(): String? =
    if (isJsonPrimitive && asJsonPrimitive.isString) asString else null

private fun parseRawMessage(body: String?): String? {
    val trimmed = body?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    if (trimmed.startsWith("<")) return null

    return try {
        if (!trimmed.startsWith("{")) return trimmed
        val envelope = gson.fromJson(trimmed, ErrorEnvelope::class.java)
        envelope.error
            ?: envelope.message
            ?: envelope.details
            ?: JsonParser.parseString(trimmed).asJsonObject.get("error")?.stringOrNull()
            ?: JsonParser.parseString(trimmed).asJsonObject.get("message")?.stringOrNull()
            ?: JsonParser.parseString(trimmed).asJsonObject.get("details")?.stringOrNull()
    } catch (_: Exception) {
        trimmed
    }
}

fun parseApiError(body: String?, code: Int, fallback: String): String {
    val raw = parseRawMessage(body)?.trim('"')?.takeIf { it.isNotBlank() }
    return when {
        raw != null -> translations[raw] ?: httpFallback(code, raw)
        else -> httpFallback(code, fallback)
    }
}

fun <T> Response<T>.errorMessage(fallback: String): String =
    parseApiError(errorBody()?.string(), code(), fallback)
