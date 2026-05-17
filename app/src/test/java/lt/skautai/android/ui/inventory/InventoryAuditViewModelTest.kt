package lt.skautai.android.ui.inventory

import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.ItemCheckResult
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryAuditViewModelTest {

    @Test
    fun `summary counts each audit result and unchecked items`() {
        val items = listOf(
            testItem("1", "Palapinė"),
            testItem("2", "Puodas"),
            testItem("3", "Kirvis"),
            testItem("4", "Vaistinėlė"),
            testItem("5", "Virvė")
        )

        val summary = buildInventoryAuditSummary(
            items = items,
            results = mapOf(
                "1" to AuditEntryDraft(ItemCheckResult.FOUND, actualQuantity = 1),
                "2" to AuditEntryDraft(ItemCheckResult.MISSING, actualQuantity = 0),
                "3" to AuditEntryDraft(ItemCheckResult.MISPLACED, actualQuantity = 1),
                "4" to AuditEntryDraft(ItemCheckResult.DAMAGED, actualQuantity = 1)
            )
        )

        assertEquals(5, summary.total)
        assertEquals(1, summary.found)
        assertEquals(1, summary.missing)
        assertEquals(1, summary.misplaced)
        assertEquals(1, summary.damaged)
        assertEquals(1, summary.unchecked)
    }

    @Test
    fun `mark unchecked as missing preserves existing results`() {
        val items = listOf(
            testItem("1", "Palapinė"),
            testItem("2", "Puodas"),
            testItem("3", "Kirvis")
        )

        val updated = applyMissingToUnchecked(
            items = items,
            results = mapOf("1" to AuditEntryDraft(ItemCheckResult.FOUND, actualQuantity = 1))
        )

        assertEquals(
            mapOf(
                "1" to AuditEntryDraft(ItemCheckResult.FOUND, actualQuantity = 1),
                "2" to AuditEntryDraft(ItemCheckResult.MISSING, actualQuantity = 0),
                "3" to AuditEntryDraft(ItemCheckResult.MISSING, actualQuantity = 0)
            ),
            updated
        )
    }

    private fun testItem(id: String, name: String) = ItemDto(
        id = id,
        qrToken = "qr-$id",
        tuntasId = "tuntas-1",
        custodianId = null,
        custodianName = null,
        origin = "UNIT_ACQUIRED",
        name = name,
        description = null,
        type = "COLLECTIVE",
        category = "CAMPING",
        condition = "GOOD",
        quantity = 1,
        locationId = null,
        locationName = null,
        locationPath = null,
        temporaryStorageLabel = null,
        sourceSharedItemId = null,
        quantityBreakdown = emptyList(),
        totalQuantityAcrossCustodians = 1,
        responsibleUserId = null,
        responsibleUserName = null,
        createdByUserId = null,
        createdByUserName = null,
        photoUrl = null,
        purchaseDate = null,
        purchasePrice = null,
        notes = null,
        customFields = emptyList(),
        status = "ACTIVE",
        submittedByUserId = null,
        submittedByUserName = null,
        targetScope = null,
        reviewedByUserId = null,
        rejectionReason = null,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )
}
