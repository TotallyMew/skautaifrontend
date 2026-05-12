package lt.skautai.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCsvTest {

    @Test
    fun `analyze inventory table detects header row not first`() {
        val csv = """
            Atsitiktine eilute;ne antraste
            Pavadinimas;Kiekis;Pastabos
            Palapine;2;Papildymui
        """.trimIndent()

        val table = InventoryCsv.parseTextTable(csv)
        val draft = InventoryCsv.analyzeInventoryTable("test.csv", table)

        assertEquals(1, draft.headerRowIndex)
        assertEquals(1, draft.rowCount)
        assertEquals("Pavadinimas", draft.headers.first())
    }

    @Test
    fun `inventory export uses user friendly headers`() {
        val csv = InventoryCsv.inventoryTemplate()
        assertTrue(csv.contains("Pavadinimas"))
        assertTrue(csv.contains("Kiekis"))
        assertTrue(csv.contains("Bukles priezastis"))
    }
}
