package lt.skautai.android.util

import lt.skautai.android.data.remote.EventRoleDto

fun eventRolesForUser(eventRoles: List<EventRoleDto>, userId: String?): Set<String> =
    eventRoles.filter { it.userId == userId }.map { it.role }.toSet()

fun canManageEventSections(
    permissions: Set<String>,
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean = !isReadOnly && ("events.manage:ALL" in permissions || "VIRSININKAS" in eventRoles)

fun canManageEventInventorySections(
    permissions: Set<String>,
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean {
    if (isReadOnly) return false
    if (canManageEventSections(permissions, eventRoles, isReadOnly)) return true
    if ("events.inventory.distribute:ALL" in permissions) return true
    return eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") }
}

fun canViewEventPlan(
    permissions: Set<String>,
    eventRoles: Set<String>
): Boolean {
    if ("events.manage:ALL" in permissions || "events.inventory.distribute:ALL" in permissions) return true
    return eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS") }
}

fun canRequestEventInventory(
    eventRoles: Set<String>,
    isReadOnly: Boolean
): Boolean = !isReadOnly && eventRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS") }
