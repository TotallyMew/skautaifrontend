package lt.skautai.android.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LithuanianNameVocativeFormatterTest {
    @Test
    fun formatsCommonMasculineNames() {
        assertEquals("Augustai", LithuanianNameVocativeFormatter.vocative("Augustas"))
        assertEquals("Jonai", LithuanianNameVocativeFormatter.vocative("Jonas"))
        assertEquals("Mariau", LithuanianNameVocativeFormatter.vocative("Marius"))
        assertEquals("Pauliau", LithuanianNameVocativeFormatter.vocative("Paulius"))
        assertEquals("Andriau", LithuanianNameVocativeFormatter.vocative("Andrius"))
        assertEquals("Tautvydi", LithuanianNameVocativeFormatter.vocative("Tautvydis"))
        assertEquals("Mindaugį", LithuanianNameVocativeFormatter.vocative("Mindaugys"))
    }

    @Test
    fun formatsCommonFeminineNames() {
        assertEquals("Egle", LithuanianNameVocativeFormatter.vocative("Eglė"))
        assertEquals("Dovile", LithuanianNameVocativeFormatter.vocative("Dovilė"))
        assertEquals("Ieva", LithuanianNameVocativeFormatter.vocative("Ieva"))
        assertEquals("Austėja", LithuanianNameVocativeFormatter.vocative("Austėja"))
    }

    @Test
    fun usesFirstNameFromFullName() {
        assertEquals("Augustai", LithuanianNameVocativeFormatter.firstNameVocative("Augustas Petrauskas"))
    }

    @Test
    fun preservesUppercaseNames() {
        assertEquals("AUGUSTAI", LithuanianNameVocativeFormatter.vocative("AUGUSTAS"))
    }

    @Test
    fun blankNameFallsBackToSkautai() {
        assertEquals("skautai", LithuanianNameVocativeFormatter.firstNameVocative("   "))
        assertEquals("skautai", LithuanianNameVocativeFormatter.firstNameVocative(null))
    }
}
