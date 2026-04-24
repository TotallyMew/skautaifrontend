package lt.skautai.android.util

object RegistrationValidation {
    val allowedKrastai = listOf(
        "Alytaus",
        "Kauno",
        "Klaipėdos",
        "Marijampolės",
        "Šiaulių",
        "Tauragės",
        "Telšių",
        "Utenos",
        "Vilniaus"
    )

    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun emailError(email: String): String? {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isBlank() -> "Įveskite el. paštą"
            !emailRegex.matches(normalized) -> "Įveskite teisingą el. pašto adresą"
            else -> null
        }
    }

    fun passwordError(password: String): String? {
        return when {
            password.isBlank() -> "Įveskite slaptažodį"
            password.length < 8 -> "Slaptažodis turi būti bent 8 simbolių"
            password.any { it.isWhitespace() } -> "Slaptažodyje negali būti tarpų"
            password.none { it.isLetter() } -> "Slaptažodyje turi būti bent viena raidė"
            password.none { it.isDigit() } -> "Slaptažodyje turi būti bent vienas skaičius"
            else -> null
        }
    }

    fun krastasError(krastas: String): String? {
        return when {
            krastas.isBlank() -> "Pasirinkite kraštą"
            krastas !in allowedKrastai -> "Pasirinkite kraštą iš sąrašo"
            else -> null
        }
    }
}
