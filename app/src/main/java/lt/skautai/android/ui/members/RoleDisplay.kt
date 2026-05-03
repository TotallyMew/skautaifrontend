package lt.skautai.android.ui.members

internal fun displayRoleName(roleName: String): String = when (roleName) {
    "Vyr. skautu draugoves draugininkas" -> "Vyr. skautų draugovės draugininkas"
    "Vyr. skautu draugoves draugininko pavaduotojas" -> "Vyr. skautų draugovės draugininko pavaduotojas"
    "Vyr. skautu burelio pirmininkas" -> "Vyr. skautų būrelio pirmininkas"
    "Vyr. skautu burelio pirmininko pavaduotojas" -> "Vyr. skautų būrelio pirmininko pavaduotojas"
    "Vyr. skauciu draugoves draugininkas" -> "Vyr. skaučių draugovės draugininkė"
    "Vyr. skauciu draugoves draugininko pavaduotojas" -> "Vyr. skaučių draugovės draugininkės pavaduotoja"
    "Vyr. skauciu burelio pirmininkas" -> "Vyr. skaučių būrelio pirmininkė"
    "Vyr. skauciu burelio pirmininko pavaduotojas" -> "Vyr. skaučių būrelio pirmininkės pavaduotoja"
    "Patyres skautas" -> "Patyręs skautas"
    else -> roleName
}
