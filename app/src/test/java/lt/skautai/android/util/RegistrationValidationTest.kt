package lt.skautai.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationValidationTest {
    @Test
    fun validatesPersonNames() {
        assertNull(RegistrationValidation.nameError("Jonas"))
        assertNull(RegistrationValidation.surnameError("Kazlauskas-Jonaitis"))
        assertEquals("Vardas turi būti bent 2 simbolių.", RegistrationValidation.nameError("J"))
        assertEquals(
            "Varde naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
            RegistrationValidation.nameError("123")
        )
        assertEquals(
            "Pavardėje naudokite tik raides, tarpus, brūkšnį arba apostrofą.",
            RegistrationValidation.surnameError("Jonaitis2")
        )
        assertEquals(
            "Vardas negali būti ilgesnis nei 100 simbolių.",
            RegistrationValidation.nameError("A".repeat(101))
        )
    }

    @Test
    fun normalizesEmail() {
        assertEquals("test@example.com", RegistrationValidation.normalizeEmail(" Test@Example.COM "))
    }

    @Test
    fun validatesEmailFormat() {
        assertNull(RegistrationValidation.emailError("test@example.com"))
        assertEquals("Įveskite teisingą el. pašto adresą.", RegistrationValidation.emailError("test"))
        assertEquals(
            "El. paštas negali būti ilgesnis nei 255 simboliai.",
            RegistrationValidation.emailError("${"a".repeat(245)}@example.com")
        )
    }

    @Test
    fun validatesPasswordRules() {
        assertNull(RegistrationValidation.passwordError("testas123"))
        assertEquals("Slaptažodis turi būti bent 8 simbolių.", RegistrationValidation.passwordError("test123"))
        assertEquals(
            "Slaptažodis negali būti ilgesnis nei 128 simboliai.",
            RegistrationValidation.passwordError("a1${"b".repeat(127)}")
        )
        assertEquals("Slaptažodyje turi būti bent vienas skaičius.", RegistrationValidation.passwordError("testasabc"))
        assertEquals("Slaptažodyje turi būti bent viena raidė.", RegistrationValidation.passwordError("12345678"))
    }

    @Test
    fun validatesPhone() {
        assertNull(RegistrationValidation.phoneError(""))
        assertNull(RegistrationValidation.phoneError("+370 600 00000"))
        assertEquals(
            "Telefono numeryje naudokite tik skaičius, +, tarpus, brūkšnį arba skliaustus.",
            RegistrationValidation.phoneError("+370 abc")
        )
        assertEquals("Telefono numeryje turi būti bent 5 skaičiai.", RegistrationValidation.phoneError("1234"))
        assertEquals(
            "Telefono numeris negali būti ilgesnis nei 20 simbolių.",
            RegistrationValidation.phoneError("+370 600 00000 999999")
        )
    }

    @Test
    fun validatesTuntasName() {
        assertNull(RegistrationValidation.tuntasNameError("Vilniaus tuntas"))
        assertNull(RegistrationValidation.tuntasNameError("Vilniaus 1-asis tuntas"))
        assertEquals(
            "Tunto pavadinime turi būti bent viena raidė.",
            RegistrationValidation.tuntasNameError("123")
        )
        assertEquals(
            "Tunto pavadinimas negali būti ilgesnis nei 100 simbolių.",
            RegistrationValidation.tuntasNameError("A".repeat(101))
        )
    }

    @Test
    fun validatesKrastasList() {
        assertEquals(
            listOf("Alytaus", "Kauno", "Klaipėdos", "Marijampolės", "Šiaulių", "Tauragės", "Telšių", "Utenos", "Vilniaus"),
            RegistrationValidation.allowedKrastai
        )
        assertNull(RegistrationValidation.krastasError("Vilniaus"))
        assertEquals("Pasirinkite kraštą iš sąrašo.", RegistrationValidation.krastasError("Vilnius"))
    }

    @Test
    fun validatesInviteCodeLength() {
        assertNull(RegistrationValidation.inviteCodeError("ABC123"))
        assertEquals("Įveskite pakvietimo kodą.", RegistrationValidation.inviteCodeError(""))
        assertEquals(
            "Pakvietimo kodas negali būti ilgesnis nei 20 simbolių.",
            RegistrationValidation.inviteCodeError("A".repeat(21))
        )
    }
}
