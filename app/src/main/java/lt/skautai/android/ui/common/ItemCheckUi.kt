package lt.skautai.android.ui.common

import androidx.compose.runtime.Composable

enum class ItemCheckResult {
    FOUND,
    MISSING,
    MISPLACED,
    DAMAGED,
    CONSUMED
}

data class ItemCheckSummary(
    val total: Int,
    val found: Int,
    val missing: Int,
    val misplaced: Int,
    val damaged: Int,
    val consumed: Int,
    val unchecked: Int
)

fun buildItemCheckSummary(
    total: Int,
    results: Collection<ItemCheckResult?>
): ItemCheckSummary {
    var found = 0
    var missing = 0
    var misplaced = 0
    var damaged = 0
    var consumed = 0
    results.forEach { result ->
        when (result) {
            ItemCheckResult.FOUND -> found += 1
            ItemCheckResult.MISSING -> missing += 1
            ItemCheckResult.MISPLACED -> misplaced += 1
            ItemCheckResult.DAMAGED -> damaged += 1
            ItemCheckResult.CONSUMED -> consumed += 1
            null -> Unit
        }
    }
    return ItemCheckSummary(
        total = total,
        found = found,
        missing = missing,
        misplaced = misplaced,
        damaged = damaged,
        consumed = consumed,
        unchecked = (total - found - missing - misplaced - damaged - consumed).coerceAtLeast(0)
    )
}

fun itemCheckResultLabel(result: ItemCheckResult): String = when (result) {
    ItemCheckResult.FOUND -> "Rasta"
    ItemCheckResult.MISSING -> "Nerasta"
    ItemCheckResult.MISPLACED -> "Ne vietoje"
    ItemCheckResult.DAMAGED -> "Sugadinta"
    ItemCheckResult.CONSUMED -> "Sunaudota"
}

fun itemCheckResultTone(result: ItemCheckResult): SkautaiStatusTone = when (result) {
    ItemCheckResult.FOUND -> SkautaiStatusTone.Success
    ItemCheckResult.MISSING -> SkautaiStatusTone.Danger
    ItemCheckResult.MISPLACED -> SkautaiStatusTone.Warning
    ItemCheckResult.DAMAGED -> SkautaiStatusTone.Info
    ItemCheckResult.CONSUMED -> SkautaiStatusTone.Neutral
}

@Composable
fun ItemCheckResultPill(
    result: ItemCheckResult?,
    emptyLabel: String = "Nežymėta"
) {
    if (result == null) {
        SkautaiStatusPill(label = emptyLabel, tone = SkautaiStatusTone.Neutral)
    } else {
        SkautaiStatusPill(label = itemCheckResultLabel(result), tone = itemCheckResultTone(result))
    }
}
