package lt.skautai.android.util

object RegistrationValidation {
    const val NAME_MIN_LENGTH = 2
    const val NAME_MAX_LENGTH = 100
    const val SURNAME_MIN_LENGTH = 2
    const val SURNAME_MAX_LENGTH = 100
    const val EMAIL_MAX_LENGTH = 255
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128
    const val PHONE_MAX_LENGTH = 20
    const val TUNTAS_NAME_MIN_LENGTH = 2
    const val TUNTAS_NAME_MAX_LENGTH = 100
    const val INVITE_CODE_MAX_LENGTH = 20

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
    private val personNameRegex = Regex("^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$")
    private val tuntasNameRegex = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'()\\-]*$")
    private val phoneRegex = Regex("^\\+?[0-9][0-9 ()\\-]*$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun nameError(name: String): String? = personNameError(
        value = name,
        blankMessage = "Įveskite vardą.",
        tooShortMessage = "Vardas turi būti bent $NAME_MIN_LENGTH simbolių.",
        tooLongMessage = "Vardas negali būti ilgesnis nei $NAME_MAX_LENGTH simbolių.",
        invalidMessage = "Varde naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
        minLength = NAME_MIN_LENGTH,
        maxLength = NAME_MAX_LENGTH
    )

    fun surnameError(surname: String): String? = personNameError(
        value = surname,
        blankMessage = "Įveskite pavardę.",
        tooShortMessage = "Pavardė turi būti bent $SURNAME_MIN_LENGTH simbolių.",
        tooLongMessage = "Pavardė negali būti ilgesnė nei $SURNAME_MAX_LENGTH simbolių.",
        invalidMessage = "Pavardėje naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
        minLength = SURNAME_MIN_LENGTH,
        maxLength = SURNAME_MAX_LENGTH
    )

    fun emailError(email: String): String? {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isBlank() -> "Įveskite el. paštą."
            normalized.length > EMAIL_MAX_LENGTH -> "El. paštas negali būti ilgesnis nei $EMAIL_MAX_LENGTH simboliai."
            !emailRegex.matches(normalized) -> "Įveskite teisingą el. pašto adresą."
            else -> null
        }
    }

    fun passwordError(password: String): String? {
        return when {
            password.isBlank() -> "Įveskite slaptažodį."
            password.length < PASSWORD_MIN_LENGTH -> "Slaptažodis turi būti bent $PASSWORD_MIN_LENGTH simbolių."
            password.length > PASSWORD_MAX_LENGTH -> "Slaptažodis negali būti ilgesnis nei $PASSWORD_MAX_LENGTH simboliai."
            password.any { it.isWhitespace() } -> "Slaptažodyje negali būti tarpų."
            password.none { it.isLetter() } -> "Slaptažodyje turi būti bent viena raidė."
            password.none { it.isDigit() } -> "Slaptažodyje turi būti bent vienas skaičius."
            else -> null
        }
    }

    fun phoneError(phone: String): String? {
        val normalized = phone.trim()
        val digitCount = normalized.count { it.isDigit() }
        return when {
            normalized.isBlank() -> null
            normalized.length > PHONE_MAX_LENGTH -> "Telefono numeris negali būti ilgesnis nei $PHONE_MAX_LENGTH simbolių."
            !phoneRegex.matches(normalized) -> "Telefono numeryje naudokite tik skaičius, +, tarpus, brūkšnį arba skliaustus."
            digitCount < 5 -> "Telefono numeryje turi būti bent 5 skaičiai."
            digitCount > 15 -> "Telefono numeryje negali būti daugiau nei 15 skaičių."
            else -> null
        }
    }

    fun tuntasNameError(tuntasName: String): String? {
        val trimmed = tuntasName.trim()
        return when {
            trimmed.isBlank() -> "Įveskite tunto pavadinimą."
            trimmed.length < TUNTAS_NAME_MIN_LENGTH -> "Tunto pavadinimas turi būti bent $TUNTAS_NAME_MIN_LENGTH simbolių."
            trimmed.length > TUNTAS_NAME_MAX_LENGTH -> "Tunto pavadinimas negali būti ilgesnis nei $TUNTAS_NAME_MAX_LENGTH simbolių."
            trimmed.none { it.isLetter() } -> "Tunto pavadinime turi būti bent viena raidė."
            !tuntasNameRegex.matches(trimmed) -> "Tunto pavadinime naudokite tik raides, skaičius, tarpus ir įprastus skyrybos ženklus."
            else -> null
        }
    }

    fun krastasError(krastas: String): String? {
        return when {
            krastas.isBlank() -> "Pasirinkite kraštą."
            krastas !in allowedKrastai -> "Pasirinkite kraštą iš sąrašo."
            else -> null
        }
    }

    fun inviteCodeError(inviteCode: String): String? {
        val trimmed = inviteCode.trim()
        return when {
            trimmed.isBlank() -> "Įveskite pakvietimo kodą."
            trimmed.length > INVITE_CODE_MAX_LENGTH -> "Pakvietimo kodas negali būti ilgesnis nei $INVITE_CODE_MAX_LENGTH simbolių."
            else -> null
        }
    }

    private fun personNameError(
        value: String,
        blankMessage: String,
        tooShortMessage: String,
        tooLongMessage: String,
        invalidMessage: String,
        minLength: Int,
        maxLength: Int
    ): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> blankMessage
            trimmed.length < minLength -> tooShortMessage
            trimmed.length > maxLength -> tooLongMessage
            !personNameRegex.matches(trimmed) -> invalidMessage
            else -> null
        }
    }
}
