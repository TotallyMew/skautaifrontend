package lt.skautai.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationValidationTest {
    @Test
    fun normalizesEmail() {
        assertEquals("test@example.com", RegistrationValidation.normalizeEmail(" Test@Example.COM "))
    }

    @Test
    fun validatesEmailFormat() {
        assertNull(RegistrationValidation.emailError("test@example.com"))
        assertEquals("Įveskite teisingą el. pašto adresą", RegistrationValidation.emailError("test"))
    }

    @Test
    fun validatesPasswordRules() {
        assertNull(RegistrationValidation.passwordError("testas123"))
        assertEquals("Slaptažodis turi būti bent 8 simbolių", RegistrationValidation.passwordError("test123"))
        assertEquals("Slaptažodyje turi būti bent vienas skaičius", RegistrationValidation.passwordError("testasabc"))
        assertEquals("Slaptažodyje turi būti bent viena raidė", RegistrationValidation.passwordError("12345678"))
    }

    @Test
    fun validatesKrastasList() {
        assertEquals(
            listOf("Alytaus", "Kauno", "Klaipėdos", "Marijampolės", "Šiaulių", "Tauragės", "Telšių", "Utenos", "Vilniaus"),
            RegistrationValidation.allowedKrastai
        )
        assertNull(RegistrationValidation.krastasError("Vilniaus"))
        assertEquals("Pasirinkite kraštą iš sąrašo", RegistrationValidation.krastasError("Vilnius"))
    }
}
