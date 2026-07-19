package lt.skautai.android.ui.inventory

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import lt.skautai.android.data.remote.ItemCapabilitiesDto
import lt.skautai.android.data.remote.ItemDto

class InventoryListBulkActionsTest {

    @Test
    fun `bulk management follows backend item capability`() {
        val readOnlyItem = testItem(
            custodianId = "unit-1",
            origin = "TRANSFERRED_FROM_TUNTAS",
            canEdit = false
        )
        val editableItem = readOnlyItem.copy(capabilities = ItemCapabilitiesDto(canEdit = true))

        assertFalse(canManageInventoryItem(readOnlyItem))
        assertTrue(canManageInventoryItem(editableItem))
    }

    @Test
    fun `missing backend capability fails closed`() {
        val item = testItem(custodianId = "unit-1", canEdit = null)

        assertFalse(canManageInventoryItem(item))
    }

    @Test
    fun `bulk request builder returns null for empty action`() {
        assertNull(buildBulkUpdateRequest(InventoryBulkAction()))
    }

    @Test
    fun `bulk request builder sets clear location and deactivate flags`() {
        val request = buildBulkUpdateRequest(
            InventoryBulkAction(
                condition = "DAMAGED",
                clearLocation = true,
                deactivate = true
            )
        )!!

        assertEquals("DAMAGED", request.condition)
        assertEquals("INACTIVE", request.status)
        assertTrue(request.clearLocationId)
        assertNull(request.locationId)
    }

    private fun testItem(
        custodianId: String? = null,
        origin: String = "UNIT_ACQUIRED",
        canEdit: Boolean? = true
    ): ItemDto = ItemDto(
        id = "item-1",
        qrToken = "qr",
        tuntasId = "tuntas-1",
        custodianId = custodianId,
        custodianName = null,
        origin = origin,
        name = "Palapinė",
        description = null,
        type = "COLLECTIVE",
        category = "CAMPING",
        condition = "GOOD",
        quantity = 1,
        locationId = "loc-1",
        locationName = null,
        locationPath = null,
        temporaryStorageLabel = null,
        sourceSharedItemId = null,
        quantityBreakdown = emptyList(),
        totalQuantityAcrossCustodians = 1,
        responsibleUserId = null,
        responsibleUserName = null,
        createdByUserId = "user-1",
        createdByUserName = "User",
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
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
        capabilities = canEdit?.let { ItemCapabilitiesDto(canEdit = it) }
    )
}
