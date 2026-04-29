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

const val SESSION_EXPIRED_MESSAGE = "Prisijungimas baigėsi. Prisijunkite iš naujo."
const val TUNTAS_SELECTION_REQUIRED_MESSAGE = "Pirmiausia pasirinkite tuntą."

private val translations = mapOf(
    "Insufficient permissions" to "Neturite teisių atlikti šį veiksmą.",
    "Not authenticated" to SESSION_EXPIRED_MESSAGE,
    "Invalid token" to SESSION_EXPIRED_MESSAGE,
    "Invalid refresh token" to SESSION_EXPIRED_MESSAGE,
    "Login failed" to "Neteisingas el. paštas arba slaptažodis.",
    "Invalid email or password" to "Neteisingas el. paštas arba slaptažodis.",
    "Registration failed" to "Registracija nepavyko. Patikrinkite įvestus duomenis.",
    "Setup failed" to "Konfigūracija nepavyko.",
    "X-Tuntas-Id header required" to TUNTAS_SELECTION_REQUIRED_MESSAGE,
    "Missing X-Tuntas-Id header" to TUNTAS_SELECTION_REQUIRED_MESSAGE,
    "Invalid tuntas ID" to "Neteisingas tunto ID.",
    "Missing tuntas ID" to "Nenurodytas tuntas.",
    "User ID required" to "Nenurodytas vartotojas.",
    "Invalid user ID" to "Neteisingas vartotojo ID.",
    "User not found" to "Vartotojas n?rastas.",
    "Too many failed login attempts. Please try again later." to "Per daug nesėkmingų bandymų prisijungti. Pabandykite vėliau.",
    "Member not found" to "Narys n?rastas.",
    "Reservation not found" to "Rezervacija n?rasta.",
    "Reservation is not accessible" to "Rezervacija nepasiekiama.",
    "Item not found" to "Inventoriaus objektas n?rastas.",
    "Item not found or not active" to "Inventoriaus objektas n?rastas arba neaktyvus.",
    "Event not found" to "Renginys n?rastas.",
    "Location not found" to "Lokacija n?rasta.",
    "Location not found or not active" to "Lokacija n?rasta.",
    "Organizational unit not found" to "Vienetas n?rastas.",
    "Request not found" to "Prašymas n?rastas.",
    "Not found" to "Nerasta.",
    "Not a member of this tuntas" to "Nesate šio tunto narys.",
    "You are not an active member of this tuntas" to "Nesate aktyvus šio tunto narys.",
    "Email already registered" to "Šis el. paštas jau užregistruotas.",
    "Invalid email format" to "Įveskite teisingą el. pašto adresą.",
    "Invalid phone format" to "Įveskite teisingą telefono numerį.",
    "Current password is required" to "Įveskite dabartinį slaptažodį.",
    "Invalid current password" to "Dabartinis slaptažodis neteisingas.",
    "Password updated" to "Slaptažodis pakeistas.",
    "New password must be different" to "Naujas slaptažodis turi skirtis nuo dabartinio.",
    "Name is required" to "Įveskite vardą.",
    "Surname is required" to "Įveskite pavardę.",
    "Email is required" to "Įveskite el. paštą.",
    "Password is required" to "Įveskite slaptažodį.",
    "Password must be at least 8 characters" to "Slaptažodis turi būti bent 8 simbolių.",
    "Password cannot contain spaces" to "Slaptažodyje negali būti tarpų.",
    "Password must contain a letter" to "Slaptažodyje turi būti bent viena raidė.",
    "Password must contain a number" to "Slaptažodyje turi būti bent vienas skaičius.",
    "Invite code already used" to "Pakvietimo kodas jau panaudotas.",
    "Invite code expired" to "Pakvietimo kodas nebegalioja.",
    "Invalid invite code" to "Neteisingas pakvietimo kodas.",
    "Tuntininkas role cannot be invited" to "Tuntininko rolė negali būti kviečiama. Pareigos turi būti perleidžiamos.",
    "Tuntininkas role can only be transferred" to "Tuntininko rolė negali būti tiesiog priskirta. Pareigos turi būti perleidžiamos.",
    "Only active tuntininkas can transfer this role" to "Perleisti tuntininko pareigas gali tik aktyvus tuntininkas.",
    "Choose a different member to become tuntininkas" to "Pasirinkite kitą narį kaip naują tuntininką.",
    "Successor must be an active member of this tuntas" to "Naujas tuntininkas turi būti aktyvus šio tunto narys.",
    "Left tuntas" to "Tuntas paliktas.",
    "Cannot delete unit that has active items in its custody" to "Negalima ištrinti vieneto, kuris dar turi aktyvaus inventoriaus.",
    "Step down from active leadership roles before leaving this unit" to "Prieš palikdami vienetą atsisakykite aktyvių vadovavimo pareigų."
)

private fun httpFallback(code: Int, fallback: String): String = when (code) {
    401 -> SESSION_EXPIRED_MESSAGE
    403 -> "Neturite teisių atlikti šį veiksmą."
    404 -> "Nurodytas objektas n?rastas."
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

fun sessionExpiredException(): Exception = Exception(SESSION_EXPIRED_MESSAGE)

fun tuntasSelectionRequiredException(): Exception = Exception(TUNTAS_SELECTION_REQUIRED_MESSAGE)

fun parseApiError(body: String?, code: Int, fallback: String): String {
    val raw = parseRawMessage(body)?.trim('"')?.takeIf { it.isNotBlank() }
    return when {
        raw != null -> translations[raw] ?: httpFallback(code, raw)
        else -> httpFallback(code, fallback)
    }
}

fun <T> Response<T>.errorMessage(fallback: String): String =
    parseApiError(errorBody()?.string(), code(), fallback)
