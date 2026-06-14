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

    @Test
    fun `inventory import accepts excel style numbers dates and prices`() {
        val csv = """
            Pavadinimas;Kiekis;Pirkimo data;Pirkimo kaina;Minimalus likutis
            Palapine;2.0;2026.06.12;1 234,50 EUR;1.0
        """.trimIndent()

        val result = InventoryCsv.parseInventory(csv, type = "COLLECTIVE", custodianId = null)

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.rows.size)
        assertEquals(2, result.rows.first().quantity)
        assertEquals("2026-06-12", result.rows.first().purchaseDate)
        assertEquals(1234.50, result.rows.first().purchasePrice ?: 0.0, 0.001)
        assertEquals(1, result.rows.first().minimumQuantity)
    }

    @Test
    fun `inventory import converts excel serial purchase date`() {
        val csv = """
            Pavadinimas;Pirkimo data
            Virve;46185
        """.trimIndent()

        val result = InventoryCsv.parseInventory(csv, type = "COLLECTIVE", custodianId = null)

        assertTrue(result.errors.isEmpty())
        assertEquals("2026-06-12", result.rows.first().purchaseDate)
    }

    @Test
    fun `event plan import detects header row not first`() {
        val csv = """
            Renginio inventoriaus planas
            Pavadinimas;Kiekis;Pastabos
            Puodas;3.0;Virtuvei
        """.trimIndent()

        val result = InventoryCsv.parseEventPlan(csv)

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.rows.size)
        assertEquals("Puodas", result.rows.first().name)
        assertEquals(3, result.rows.first().plannedQuantity)
    }
}
