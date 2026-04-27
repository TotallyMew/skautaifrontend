package lt.skautai.android.ui.members

internal fun displayRoleName(roleName: String): String = when (roleName) {
    "Vyr. skautu draugoves draugininkas",
    "Vyr. skautu burelio pirmininkas" -> "Vyr. skautu draugininkas / pirmininkas"
    "Vyr. skautu draugoves draugininko pavaduotojas",
    "Vyr. skautu burelio pirmininko pavaduotojas" -> "Vyr. skautu draugininko / pirmininko pavaduotojas"
    "Vyr. skauciu draugoves draugininkas",
    "Vyr. skauciu burelio pirmininkas" -> "Vyr. skauciu draugininke / pirmininke"
    "Vyr. skauciu draugoves draugininko pavaduotojas",
    "Vyr. skauciu burelio pirmininko pavaduotojas" -> "Vyr. skauciu draugininkes / pirmininkes pavaduotoja"
    else -> roleName
}
