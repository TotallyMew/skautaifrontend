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
    "Request failed" to "Užklausa nepavyko.",
    "Internal server error" to "Vidinė serverio klaida.",
    "Insufficient permissions" to "Neturite teisių atlikti šį veiksmą.",
    "Super admin access required" to "Reikalingos superadministratoriaus teisės.",
    "Not authenticated" to SESSION_EXPIRED_MESSAGE,
    "Invalid token" to SESSION_EXPIRED_MESSAGE,
    "Invalid refresh token" to SESSION_EXPIRED_MESSAGE,
    "Login failed" to "Neteisingas el. paštas arba slaptažodis.",
    "Invalid email or password" to "Neteisingas el. paštas arba slaptažodis.",
    "Registration failed" to "Registracija nepavyko. Patikrinkite įvestus duomenis.",
    "Setup failed" to "Konfigūracija nepavyko.",
    "Refresh failed" to SESSION_EXPIRED_MESSAGE,
    "Invalid bootstrap token" to "Neteisingas pradinis prieigos kodas.",
    "X-Tuntas-Id header required" to TUNTAS_SELECTION_REQUIRED_MESSAGE,
    "Missing X-Tuntas-Id header" to TUNTAS_SELECTION_REQUIRED_MESSAGE,
    "Invalid tuntas ID" to "Neteisingas tunto ID.",
    "Missing tuntas ID" to "Nenurodytas tuntas.",
    "User ID required" to "Nenurodytas vartotojas.",
    "Invalid user ID" to "Neteisingas vartotojo ID.",
    "User not found" to "Vartotojas nerastas.",
    "Too many failed login attempts. Please try again later." to "Per daug nesėkmingų bandymų prisijungti. Pabandykite vėliau.",
    "Member not found" to "Narys nerastas.",
    "Rank not found" to "Laipsnis nerastas.",
    "Role not found" to "Pareigos nerastos.",
    "Reservation not found" to "Rezervacija nerasta.",
    "Reservation is not accessible" to "Rezervacija nepasiekiama.",
    "Item not found" to "Inventoriaus objektas nerastas.",
    "Item not found or not active" to "Inventoriaus objektas nerastas arba neaktyvus.",
    "Event not found" to "Renginys nerastas.",
    "Location not found" to "Lokacija nerasta.",
    "Location not found or not active" to "Lokacija nerasta.",
    "Organizational unit not found" to "Vienetas nerastas.",
    "Request not found" to "Prašymas nerastas.",
    "Kit not found" to "Rinkinys nerastas.",
    "Inventory kit not found" to "Inventoriaus rinkinys nerastas.",
    "Template not found" to "Šablonas nerastas.",
    "Purchase not found" to "Pirkimas nerastas.",
    "Invoice file not found" to "Sąskaitos failas nerastas.",
    "Shared item not found" to "Bendro inventoriaus objektas nerastas.",
    "Shared inventory item not found" to "Bendro inventoriaus objektas nerastas.",
    "Unit item not found" to "Vieneto inventoriaus objektas nerastas.",
    "Source shared item not found" to "Pradinis bendro inventoriaus objektas nerastas.",
    "Responsible user not found" to "Atsakingas narys nerastas.",
    "Only active leaders can create a tuntas-level request" to "Tik aktyvus draugininkas arba tuntinio lygio vadovas gali teikti prašymą tuntui.",
    "Tik aktyvus draugininkas arba tuntinio lygio vadovas gali kurti prašymą tuntui" to "Tik aktyvus draugininkas arba tuntinio lygio vadovas gali teikti prašymą tuntui.",
    "Not found" to "Nerasta.",
    "Not a member of this tuntas" to "Nesate šio tunto narys.",
    "You are not an active member of this tuntas" to "Nesate aktyvus šio tunto narys.",
    "Email already registered" to "Šis el. paštas jau užregistruotas.",
    "Tuntas name already exists" to "Tuntas tokiu pavadinimu jau yra.",
    "Invalid email format" to "Įveskite teisingą el. pašto adresą.",
    "Invalid phone format" to "Įveskite teisingą telefono numerį.",
    "Current password is required" to "Įveskite dabartinį slaptažodį.",
    "Invalid current password" to "Dabartinis slaptažodis neteisingas.",
    "Password updated" to "Slaptažodis pakeistas.",
    "New password must be different" to "Naujas slaptažodis turi skirtis nuo dabartinio.",
    "Name is required" to "Įveskite vardą.",
    "Name must be at least 2 characters" to "Vardas turi būti bent 2 simbolių.",
    "Name must be at most 100 characters" to "Vardas negali būti ilgesnis nei 100 simbolių.",
    "Name contains invalid characters" to "Varde naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
    "Surname is required" to "Įveskite pavardę.",
    "Surname must be at least 2 characters" to "Pavardė turi būti bent 2 simbolių.",
    "Surname must be at most 100 characters" to "Pavardė negali būti ilgesnė nei 100 simbolių.",
    "Surname contains invalid characters" to "Pavardėje naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
    "Email is required" to "Įveskite el. paštą.",
    "Email must be at most 255 characters" to "El. paštas negali būti ilgesnis nei 255 simboliai.",
    "Password is required" to "Įveskite slaptažodį.",
    "Password must be at least 8 characters" to "Slaptažodis turi būti bent 8 simbolių.",
    "Password must be at most 128 characters" to "Slaptažodis negali būti ilgesnis nei 128 simboliai.",
    "Password cannot contain spaces" to "Slaptažodyje negali būti tarpų.",
    "Password must contain a letter" to "Slaptažodyje turi būti bent viena raidė.",
    "Password must contain a number" to "Slaptažodyje turi būti bent vienas skaičius.",
    "Phone must be at most 20 characters" to "Telefono numeris negali būti ilgesnis nei 20 simbolių.",
    "Phone must contain at least 5 digits" to "Telefono numeryje turi būti bent 5 skaičiai.",
    "Phone must contain at most 15 digits" to "Telefono numeryje negali būti daugiau nei 15 skaičių.",
    "Tuntas name is required" to "Įveskite tunto pavadinimą.",
    "Tuntas name must be at least 2 characters" to "Tunto pavadinimas turi būti bent 2 simbolių.",
    "Tuntas name must be at most 100 characters" to "Tunto pavadinimas negali būti ilgesnis nei 100 simbolių.",
    "Tuntas name must contain a letter" to "Tunto pavadinime turi būti bent viena raidė.",
    "Tuntas name contains invalid characters" to "Tunto pavadinime naudokite tik raides, skaičius, tarpus ir įprastus skyrybos ženklus.",
    "Krastas is required" to "Pasirinkite kraštą.",
    "Invalid krastas" to "Pasirinkite kraštą iš sąrašo.",
    "Invite code is required" to "Įveskite pakvietimo kodą.",
    "Invite code must be at most 20 characters" to "Pakvietimo kodas negali būti ilgesnis nei 20 simbolių.",
    "Invite code already used" to "Pakvietimo kodas jau panaudotas.",
    "Invite code expired" to "Pakvietimo kodas nebegalioja.",
    "Invalid invite code" to "Neteisingas pakvietimo kodas.",
    "Unknown role type" to "Nežinomas pareigų tipas.",
    "Failed to update profile" to "Profilio atnaujinti nepavyko.",
    "Failed to update password" to "Slaptažodžio pakeisti nepavyko.",
    "Failed to leave tuntas" to "Tunto palikti nepavyko.",
    "Approval failed" to "Patvirtinti nepavyko.",
    "Rejection failed" to "Atmesti nepavyko.",
    "Failed to fetch organizational units" to "Vienetų gauti nepavyko.",
    "Failed to fetch members" to "Narių gauti nepavyko.",
    "Failed to assign leadership role" to "Vadovavimo pareigų priskirti nepavyko.",
    "Failed to update leadership role" to "Vadovavimo pareigų atnaujinti nepavyko.",
    "Failed to remove leadership role" to "Vadovavimo pareigų pašalinti nepavyko.",
    "Failed to assign rank" to "Laipsnio priskirti nepavyko.",
    "Failed to remove rank" to "Laipsnio pašalinti nepavyko.",
    "Failed to create inventory item" to "Inventoriaus objekto sukurti nepavyko.",
    "Invalid status" to "Neteisinga būsena.",
    "Invalid inventory type" to "Neteisingas inventoriaus tipas.",
    "Invalid event type" to "Neteisingas renginio tipas.",
    "Invalid custodian ID" to "Neteisingas saugotojo ID.",
    "Invalid location ID" to "Neteisingas lokacijos ID.",
    "Invalid item ID" to "Neteisingas inventoriaus objekto ID.",
    "Invalid event ID" to "Neteisingas renginio ID.",
    "Invalid role ID" to "Neteisingas pareigų ID.",
    "Invalid member ID" to "Neteisingas nario ID.",
    "Quantity must be at least 1" to "Kiekis turi būti bent 1.",
    "Item name is required" to "Įveskite inventoriaus objekto pavadinimą.",
    "Requesting unit is required" to "Pasirinkite prašantį vienetą.",
    "Request must be forwarded by unit leader first" to "Prašymą pirmiausia turi persiųsti vieneto vadovas.",
    "Only shared tuntas inventory can be transferred" to "Perduoti galima tik bendrą tunto inventorių.",
    "Only active shared inventory can be transferred" to "Perduoti galima tik aktyvų bendrą inventorių.",
    "Only unit inventory can be returned" to "Grąžinti galima tik vieneto inventorių.",
    "Only transferred shared inventory can be returned" to "Grąžinti galima tik iš bendro inventoriaus perduotus daiktus.",
    "Only active unit inventory can be returned" to "Grąžinti galima tik aktyvų vieneto inventorių.",
    "Cannot update an inactive item" to "Neaktyvaus inventoriaus objekto atnaujinti negalima.",
    "Item is already inactive" to "Inventoriaus objektas jau neaktyvus.",
    "Write-off reason is required" to "Įveskite nurašymo priežastį.",
    "Rejection reason is required" to "Įveskite atmetimo priežastį.",
    "Action must be APPROVED or REJECTED" to "Veiksmas turi būti patvirtinimas arba atmetimas.",
    "Action must be FORWARDED or REJECTED" to "Veiksmas turi būti persiuntimas arba atmetimas.",
    "Tuntininkas role cannot be invited" to "Tuntininko pareigos negali būti kviečiamos. Jos turi būti perleidžiamos.",
    "Tuntininkas role can only be transferred" to "Tuntininko pareigos negali būti tiesiog priskirtos. Jos turi būti perleidžiamos.",
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
    404 -> "Nurodytas objektas nerastas."
    in 500..599 -> "Serverio klaida. Bandykite vėliau."
    else -> fallback
}

private val technicalErrorMarkers = listOf(
    "org.jetbrains.exposed",
    "org.postgresql",
    "postgresql",
    "psqlexception",
    "sqlexception",
    "sqlstate",
    "jdbc:",
    "select ",
    "insert ",
    "update ",
    "delete ",
    " from ",
    " where ",
    "constraint",
    "duplicate key",
    "foreign key",
    "stacktrace",
    "exception:",
    "java.net.",
    "failed to connect to",
    "localhost",
    "10.0.2.2"
)

private fun isTechnicalMessage(message: String): Boolean {
    val normalized = message.lowercase()
    return technicalErrorMarkers.any { it in normalized }
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
        raw != null && isTechnicalMessage(raw) -> httpFallback(code, fallback)
        raw != null -> translations[raw] ?: genericLithuanianMessage(raw) ?: httpFallback(code, raw)
        else -> httpFallback(code, fallback)
    }
}

fun <T> Response<T>.errorMessage(fallback: String): String =
    parseApiError(errorBody()?.string(), code(), fallback)

fun Throwable.userFacingException(fallback: String = "Veiksmas nepavyko. Bandykite vėliau."): Exception {
    val raw = message?.trim().orEmpty()
    val safeMessage = when {
        raw.isBlank() -> fallback
        isTechnicalMessage(raw) -> fallback
        else -> genericLithuanianMessage(raw) ?: raw
    }
    return Exception(safeMessage, this)
}

private fun genericLithuanianMessage(message: String): String? {
    val normalized = message.lowercase()
    return when {
        "not found" in normalized -> "Objektas nerastas."
        "failed to" in normalized || " failed" in normalized -> "Veiksmas nepavyko. Bandykite dar kartą."
        "invalid" in normalized -> "Neteisingi duomenys."
        "required" in normalized -> "Užpildykite privalomus laukus."
        "already" in normalized -> "Toks įrašas jau yra."
        "cannot" in normalized -> "Šio veiksmo atlikti negalima."
        "only " in normalized -> "Neturite teisių atlikti šį veiksmą."
        "must " in normalized -> "Patikrinkite įvestus duomenis."
        "unknown" in normalized -> "Nežinoma reikšmė."
        else -> null
    }
}
