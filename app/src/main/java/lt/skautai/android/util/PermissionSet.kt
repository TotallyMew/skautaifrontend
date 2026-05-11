package lt.skautai.android.util

fun Set<String>.hasPermission(permission: String): Boolean =
    permission in this || any { it.startsWith("$permission:") }

fun Set<String>.hasPermissionScope(permission: String, scope: String): Boolean =
    "$permission:$scope" in this

fun Set<String>.hasPermissionAll(permission: String): Boolean =
    hasPermissionScope(permission, "ALL")

fun Set<String>.hasPermissionOwnUnit(permission: String): Boolean =
    hasPermissionScope(permission, "OWN_UNIT")

fun Set<String>.canViewInventory(): Boolean = hasPermission("items.view")

fun Set<String>.canCreateItems(): Boolean = hasPermission("items.create")

fun Set<String>.canManageSharedInventory(): Boolean =
    hasPermissionAll("items.transfer") || "items.transfer" in this

fun Set<String>.canManageAllItems(): Boolean =
    hasPermissionAll("items.update") || hasPermissionAll("items.delete") || canManageSharedInventory()

fun Set<String>.canExportInventory(): Boolean =
    canManageAllItems() || hasPermissionOwnUnit("items.create") || hasPermissionOwnUnit("items.update")

fun Set<String>.canImportInventory(): Boolean =
    canManageSharedInventory()

fun Set<String>.canGenerateInventoryQrPdf(): Boolean =
    canExportInventory()

fun Set<String>.canViewMembers(): Boolean = hasPermission("members.view")

fun Set<String>.canInviteMembers(): Boolean = hasPermission("invitations.create")

fun Set<String>.canViewUnits(): Boolean = hasPermission("organizational_units.view")

fun Set<String>.canManageUnits(): Boolean = hasPermissionAll("organizational_units.manage")

fun Set<String>.canManageLocations(): Boolean = hasPermission("locations.manage")

fun Set<String>.canViewReservations(): Boolean = hasPermission("reservations.view")

fun Set<String>.canCreateReservations(): Boolean = hasPermission("reservations.create")

fun Set<String>.canReviewTopLevelReservations(): Boolean = hasPermissionAll("reservations.approve")

fun Set<String>.canReviewUnitReservations(): Boolean =
    hasPermissionOwnUnit("reservations.approve") || canReviewTopLevelReservations()

fun Set<String>.canCreateRequisitions(): Boolean = hasPermission("requisitions.create")

fun Set<String>.canReviewTopLevelRequisitions(): Boolean = hasPermissionAll("requisitions.approve")

fun Set<String>.canForwardUnitRequests(): Boolean = hasPermission("items.request.forward.bendras")
