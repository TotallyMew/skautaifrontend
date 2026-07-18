package lt.skautai.android.util

import lt.skautai.android.data.remote.EventRoleDto

fun eventRolesForUser(eventRoles: List<EventRoleDto>, userId: String?): Set<String> =
    eventRoles.filter { it.userId == userId }.map { it.role }.toSet()

fun canManageEventSections(
    permissions: Set<String>,
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean = !isReadOnly && "VIRSININKAS" in eventRoles

fun canManageEventInventorySections(
    permissions: Set<String>,
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean {
    if (isReadOnly) return false
    return eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") }
}

fun canManageEventFinanceSections(
    permissions: Set<String>,
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean {
    if (isReadOnly) return false
    return eventRoles.any { it in setOf("VIRSININKAS", "FINANSININKAS") }
}

fun canManageEventPurchaseSections(
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean = !isReadOnly && eventRoles.any {
    it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "FINANSININKAS")
}

fun canViewEventFinanceSections(
    permissions: Set<String>,
    eventRoles: Set<String>
): Boolean = "event_purchases.invoice.download:ALL" in permissions || eventRoles.any {
    it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "FINANSININKAS")
}

fun canViewEventPlan(
    permissions: Set<String>,
    eventRoles: Set<String>
): Boolean {
    return eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS") }
}

fun canRequestEventInventory(
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean = !isReadOnly && eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS") }
