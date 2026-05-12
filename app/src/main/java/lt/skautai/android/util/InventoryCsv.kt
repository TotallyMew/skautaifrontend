package lt.skautai.android.util

import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.ItemCustomFieldDto
import lt.skautai.android.data.remote.ItemDto
import java.io.ByteArrayInputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class CsvImportResult<T>(
    val rows: List<T>,
    val skippedRows: Int,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val unknownColumns: List<String> = emptyList(),
    val missingColumns: List<String> = emptyList(),
    val mergedRows: Int = 0
) {
    val hasFatalErrors: Boolean = errors.isNotEmpty()

    fun summary(created: Int, updated: Int = 0): String = buildString {
        append("Importuota $created")
        if (updated > 0) append(", atnaujinta $updated")
        append(".")
        if (mergedRows > 0) append(" Sujungta pasikartojanciu eiluciu: $mergedRows.")
        if (skippedRows > 0) append(" Praleista: $skippedRows.")
        if (unknownColumns.isNotEmpty()) append(" Neatpazinti stulpeliai ignoruoti: ${unknownColumns.joinToString(", ")}.")
        if (warnings.isNotEmpty()) append(" ${warnings.take(2).joinToString(" ")}")
    }
}

enum class InventoryImportField(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val defaultValue: String? = null
) {
    Name("name", "Pavadinimas", required = true),
    Description("description", "Aprašymas"),
    Category("category", "Kategorija", defaultValue = "CAMPING"),
    Quantity("quantity", "Kiekis", defaultValue = "1"),
    Condition("condition", "Būklė", defaultValue = "GOOD"),
    Notes("notes", "Pastabos"),
    PurchaseDate("purchaseDate", "Pirkimo data"),
    PurchasePrice("purchasePrice", "Pirkimo kaina"),
    Location("locationName", "Lokacija"),
    UnitOfMeasure("unitOfMeasure", "Mato vienetas", defaultValue = "vnt."),
    Tags("tags", "Žymos"),
    StatusReason("statusReason", "Būklės / nurašymo priežastis"),
    Type("type", "Tipas")
}

enum class InventoryImportDuplicateMode(val label: String, val description: String) {
    Merge("Sujungti su esamais", "Jei daiktas jau yra sistemoje, importuotas kiekis bus pridėtas prie esamo įrašo."),
    CreateNew("Kurti naujus įrašus", "Pasikartojantys daiktai bus importuojami kaip atskiri įrašai."),
    SkipExisting("Praleisti esamus", "Eilutės, kurios sutampa su esamais daiktais, nebus importuotos.")
}

data class InventoryImportDraft(
    val fileName: String,
    val sourceRows: List<List<String>>,
    val headers: List<String>,
    val rows: List<List<String>>,
    val headerRowIndex: Int,
    val suggestedMapping: Map<InventoryImportField, Int?>,
    val unknownColumns: List<String>,
    val rowCount: Int
)

data class InventoryImportPreview(
    val result: CsvImportResult<CreateItemRequestDto>,
    val duplicateExistingCount: Int,
    val rowsToCreateCount: Int,
    val rowsToUpdateCount: Int
)

object InventoryCsv {
    private val inventoryHeaders = listOf(
        "Pavadinimas",
        "Aprasymas",
        "Kategorija",
        "Kiekis",
        "Bukle",
        "Pastabos",
        "Pirkimo data",
        "Pirkimo kaina",
        "Lokacija",
        "Mato vienetas",
        "Zymos",
        "Bukles priezastis"
    )

    private val eventHeaders = listOf(
        "name",
        "plannedQuantity",
        "bucketName",
        "needsPurchase",
        "notes"
    )

    private val inventoryAliases = mapOf(
        "name" to listOf("name", "pavadinimas", "daiktas", "item", "itemname"),
        "description" to listOf("description", "aprasymas", "aprasas", "desc"),
        "category" to listOf("category", "kategorija"),
        "quantity" to listOf("quantity", "kiekis", "qty", "vnt"),
        "condition" to listOf("condition", "bukle", "busena"),
        "notes" to listOf("notes", "pastabos", "komentaras", "comment"),
        "purchaseDate" to listOf("purchasedate", "purchase_date", "pirkimodata", "pirkimo_data"),
        "purchasePrice" to listOf("purchaseprice", "purchase_price", "kaina", "pirkimokaina", "pirkimo_kaina"),
        "type" to listOf("type", "tipas"),
        "custodianName" to listOf("custodianname", "custodian", "vienetas", "savininkas"),
        "locationName" to listOf("locationname", "location", "vieta", "lokacija", "sandelys", "sandelis", "lentyna", "deze"),
        "unitOfMeasure" to listOf("unitofmeasure", "unit", "measure", "matovienetas", "mato_vienetas", "vienetas"),
        "tags" to listOf("tags", "zymos", "tagai", "labels"),
        "statusReason" to listOf("statusreason", "conditionreason", "priezastis", "buklespriezastis", "nurasymopriezastis"),
        "responsibleUserName" to listOf("responsibleusername", "responsible", "atsakingas"),
        "status" to listOf("status", "busena")
    )

    private val eventAliases = mapOf(
        "name" to listOf("name", "pavadinimas", "daiktas", "item", "itemname"),
        "plannedQuantity" to listOf("plannedquantity", "planned_quantity", "quantity", "kiekis", "planuojamaskiekis", "planuojamas_kiekis"),
        "bucketName" to listOf("bucketname", "bucket", "paskirtis", "pastovykle", "pastovykles"),
        "needsPurchase" to listOf("needspurchase", "needs_purchase", "reikiapirkti", "reikia_pirkti"),
        "notes" to listOf("notes", "pastabos", "komentaras", "comment"),
        "availableQuantity" to listOf("availablequantity", "available_quantity", "turimaskiekis", "turimas_kiekis"),
        "shortageQuantity" to listOf("shortagequantity", "shortage_quantity", "trukumas"),
        "responsibleUserName" to listOf("responsibleusername", "responsible", "atsakingas")
    )

    fun inventoryTemplate(): String = toCsv(listOf(inventoryHeaders))

    fun eventTemplate(): String = toCsv(listOf(eventHeaders))

    fun exportInventory(items: List<ItemDto>): String {
        val rows = items.map { item ->
            listOf(
                item.name,
                item.description.orEmpty(),
                item.category,
                item.quantity.toString(),
                item.condition,
                item.notes.orEmpty(),
                item.purchaseDate.orEmpty(),
                item.purchasePrice?.toString().orEmpty(),
                item.locationPath ?: item.locationName.orEmpty(),
                item.customFieldValue("Mato vienetas").orEmpty(),
                item.customFieldValue("Žymos").orEmpty(),
                item.customFieldValue("Priežastis").orEmpty(),
                item.type,
                item.custodianName.orEmpty(),
                item.responsibleUserName.orEmpty(),
                item.status
            )
        }
        return toCsv(listOf(inventoryHeaders) + rows)
    }

    fun exportEventPlan(items: List<EventInventoryItemDto>): String {
        val rows = items.map { item ->
            listOf(
                item.name,
                item.plannedQuantity.toString(),
                item.bucketName.orEmpty(),
                item.needsPurchase.toString(),
                item.notes.orEmpty(),
                item.availableQuantity.toString(),
                item.shortageQuantity.toString(),
                item.responsibleUserName.orEmpty()
            )
        }
        return toCsv(listOf(eventHeaders + listOf("availableQuantity", "shortageQuantity", "responsibleUserName")) + rows)
    }

    fun parseTextTable(text: String): List<List<String>> = parse(text)

    fun parseXlsxTable(bytes: ByteArray): List<List<String>> = XlsxTableReader.readFirstSheet(bytes)

    fun analyzeInventoryTable(fileName: String, table: List<List<String>>): InventoryImportDraft {
        val headerRowIndex = detectHeaderRowIndex(table, inventoryAliases, requiredCanonical = "name")
        val headers = table.getOrNull(headerRowIndex).orEmpty()
        val headerMap = HeaderMap.from(headers, inventoryAliases)
        val suggested = InventoryImportField.entries.associateWith { field ->
            headerMap.indexOf(field.key).takeIf { it >= 0 }
        }
        return InventoryImportDraft(
            fileName = fileName,
            sourceRows = table,
            headers = headers,
            rows = table.drop(headerRowIndex + 1),
            headerRowIndex = headerRowIndex,
            suggestedMapping = suggested,
            unknownColumns = headerMap.unknownColumns,
            rowCount = (table.size - (headerRowIndex + 1)).coerceAtLeast(0)
        )
    }

    fun withHeaderRow(draft: InventoryImportDraft, headerRowIndex: Int): InventoryImportDraft {
        val safeIndex = headerRowIndex.coerceIn(0, (draft.sourceRows.lastIndex).coerceAtLeast(0))
        val headers = draft.sourceRows.getOrNull(safeIndex).orEmpty()
        val headerMap = HeaderMap.from(headers, inventoryAliases)
        val suggested = InventoryImportField.entries.associateWith { field ->
            headerMap.indexOf(field.key).takeIf { it >= 0 }
        }
        return draft.copy(
            headers = headers,
            rows = draft.sourceRows.drop(safeIndex + 1),
            headerRowIndex = safeIndex,
            suggestedMapping = suggested,
            unknownColumns = headerMap.unknownColumns,
            rowCount = (draft.sourceRows.size - (safeIndex + 1)).coerceAtLeast(0)
        )
    }

    fun previewInventoryImport(
        draft: InventoryImportDraft,
        mapping: Map<InventoryImportField, Int?>,
        type: String,
        custodianId: String?,
        existingItems: List<ItemDto>,
        duplicateMode: InventoryImportDuplicateMode
    ): InventoryImportPreview {
        val result = parseInventoryRows(
            rows = draft.rows,
            mapping = mapping,
            type = type,
            custodianId = custodianId,
            unknownColumns = draft.unknownColumns
        )
        if (result.hasFatalErrors) return InventoryImportPreview(result, 0, 0, 0)

        val existingKeys = existingItems.map {
            inventoryKey(it.name, it.category, it.condition, it.type)
        }.toSet()
        val duplicateCount = result.rows.count {
            inventoryKey(it.name, it.category, it.condition, it.type) in existingKeys
        }
        return InventoryImportPreview(
            result = result,
            duplicateExistingCount = duplicateCount,
            rowsToCreateCount = when (duplicateMode) {
                InventoryImportDuplicateMode.Merge -> result.rows.size - duplicateCount
                InventoryImportDuplicateMode.CreateNew -> result.rows.size
                InventoryImportDuplicateMode.SkipExisting -> result.rows.size - duplicateCount
            },
            rowsToUpdateCount = if (duplicateMode == InventoryImportDuplicateMode.Merge) duplicateCount else 0
        )
    }

    fun parseInventory(
        csv: String,
        type: String,
        custodianId: String?
    ): CsvImportResult<CreateItemRequestDto> {
        val table = parse(csv)
        if (table.isEmpty()) return CsvImportResult(emptyList(), 0, errors = listOf("Failas tuscias."))
        if (table.size == 1) return CsvImportResult(emptyList(), 0, errors = listOf("Faile yra tik antraste, be importuojamu eiluciu."))

        val headerRowIndex = detectHeaderRowIndex(table, inventoryAliases, requiredCanonical = "name")
        val headerMap = HeaderMap.from(table[headerRowIndex], inventoryAliases)
        val mapping = InventoryImportField.entries.associateWith { field ->
            headerMap.indexOf(field.key).takeIf { it >= 0 }
        }
        return parseInventoryRows(table.drop(headerRowIndex + 1), mapping, type, custodianId, headerMap.unknownColumns)
    }

    fun parseInventoryRows(
        rows: List<List<String>>,
        mapping: Map<InventoryImportField, Int?>,
        type: String,
        custodianId: String?,
        unknownColumns: List<String> = emptyList()
    ): CsvImportResult<CreateItemRequestDto> {
        val missingColumns = InventoryImportField.entries
            .filter { it.required && mapping[it] == null }
            .map { it.label }
        if (missingColumns.isNotEmpty()) {
            return CsvImportResult(
                rows = emptyList(),
                skippedRows = rows.size,
                errors = listOf("Truksta privalomo stulpelio: ${missingColumns.joinToString(", ")}."),
                unknownColumns = unknownColumns,
                missingColumns = missingColumns
            )
        }

        val parsedRows = mutableListOf<CreateItemRequestDto>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        rows.forEachIndexed { index, row ->
            val rowNumber = index + 2
            val name = row.value(mapping, InventoryImportField.Name).trim()
            val quantityText = row.value(mapping, InventoryImportField.Quantity).trim()
            val quantity = if (quantityText.isBlank()) 1 else quantityText.toIntOrNull()
            if (name.isBlank()) {
                skipped += 1
                warnings += "Eilute $rowNumber praleista: nenurodytas pavadinimas."
                return@forEachIndexed
            }
            if (quantity == null || quantity < 1) {
                skipped += 1
                warnings += "Eilute $rowNumber praleista: netinkamas kiekis."
                return@forEachIndexed
            }

            val category = row.value(mapping, InventoryImportField.Category)
                .normalizeInventoryCategory()
                .ifBlank { "CAMPING" }
            val condition = row.value(mapping, InventoryImportField.Condition)
                .normalizeCondition()
                .ifBlank { "GOOD" }
            val rowType = row.value(mapping, InventoryImportField.Type)
                .normalizeInventoryType(type)

            parsedRows += CreateItemRequestDto(
                name = name,
                description = row.value(mapping, InventoryImportField.Description).blankToNull(),
                type = rowType,
                category = category,
                custodianId = custodianId,
                origin = "UNIT_ACQUIRED",
                quantity = quantity,
                condition = condition,
                temporaryStorageLabel = row.value(mapping, InventoryImportField.Location).blankToNull(),
                notes = row.value(mapping, InventoryImportField.Notes).blankToNull(),
                purchaseDate = row.value(mapping, InventoryImportField.PurchaseDate).blankToNull(),
                purchasePrice = row.value(mapping, InventoryImportField.PurchasePrice).replace(',', '.').toDoubleOrNull(),
                customFields = listOfNotNull(
                    row.value(mapping, InventoryImportField.UnitOfMeasure)
                        .trim()
                        .ifBlank { "vnt." }
                        .let { ItemCustomFieldDto(fieldName = "Mato vienetas", fieldValue = it) },
                    row.value(mapping, InventoryImportField.Tags)
                        .blankToNull()
                        ?.let { ItemCustomFieldDto(fieldName = "Žymos", fieldValue = it) },
                    row.value(mapping, InventoryImportField.StatusReason)
                        .blankToNull()
                        ?.let { ItemCustomFieldDto(fieldName = "Priežastis", fieldValue = it) }
                ),
                duplicateHandling = "CREATE_NEW"
            )
        }

        val mergedRows = parsedRows.size
        val merged = parsedRows
            .groupBy { it.inventoryKey() }
            .values
            .map { group -> group.reduceInventoryRows() }

        return CsvImportResult(
            rows = merged,
            skippedRows = skipped,
            warnings = warnings,
            unknownColumns = unknownColumns,
            mergedRows = mergedRows - merged.size
        )
    }

    fun parseEventPlan(csv: String): CsvImportResult<CreateEventInventoryItemRequestDto> {
        val table = parse(csv)
        if (table.isEmpty()) return CsvImportResult(emptyList(), 0, errors = listOf("Failas tuscias."))
        if (table.size == 1) return CsvImportResult(emptyList(), 0, errors = listOf("Faile yra tik antraste, be importuojamu eiluciu."))

        val headerMap = HeaderMap.from(table.first(), eventAliases)
        val missingColumns = listOf("name").filterNot { headerMap.has(it) }
        if (missingColumns.isNotEmpty()) {
            return CsvImportResult(
                rows = emptyList(),
                skippedRows = table.size - 1,
                errors = listOf("Truksta privalomo stulpelio: ${missingColumns.joinToString(", ")}."),
                unknownColumns = headerMap.unknownColumns,
                missingColumns = missingColumns
            )
        }

        val parsedRows = mutableListOf<CreateEventInventoryItemRequestDto>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        table.drop(1).forEachIndexed { index, row ->
            val rowNumber = index + 2
            val name = row.value(headerMap, "name").trim()
            val quantityText = row.value(headerMap, "plannedQuantity").trim()
            val quantity = if (quantityText.isBlank()) 1 else quantityText.toIntOrNull()
            if (name.isBlank()) {
                skipped += 1
                warnings += "Eilute $rowNumber praleista: nenurodytas pavadinimas."
                return@forEachIndexed
            }
            if (quantity == null || quantity < 1) {
                skipped += 1
                warnings += "Eilute $rowNumber praleista: netinkamas kiekis."
                return@forEachIndexed
            }

            parsedRows += CreateEventInventoryItemRequestDto(
                name = name,
                plannedQuantity = quantity,
                notes = row.value(headerMap, "notes").blankToNull()
            )
        }

        val mergedRows = parsedRows.size
        val merged = parsedRows
            .groupBy { it.name.normalizedKey() }
            .values
            .map { group -> group.reduceEventRows() }

        return CsvImportResult(
            rows = merged,
            skippedRows = skipped,
            warnings = warnings,
            unknownColumns = headerMap.unknownColumns,
            mergedRows = mergedRows - merged.size
        )
    }

    private fun toCsv(rows: List<List<String>>): String =
        rows.joinToString("\n") { row -> row.joinToString(",") { it.escapeCsv() } }

    private fun parse(csv: String): List<List<String>> {
        val delimiter = detectDelimiter(csv)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                c == '"' && inQuotes && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    cell.append('"')
                    i += 1
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i += 1
                    row.add(cell.toString())
                    cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i += 1
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    private fun detectDelimiter(csv: String): Char {
        val firstLine = csv.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        return listOf(',', ';', '\t').maxBy { delimiter -> firstLine.countOutsideQuotes(delimiter) }
    }

    private fun String.countOutsideQuotes(target: Char): Int {
        var count = 0
        var inQuotes = false
        var i = 0
        while (i < length) {
            val c = this[i]
            when {
                c == '"' && inQuotes && i + 1 < length && this[i + 1] == '"' -> i += 1
                c == '"' -> inQuotes = !inQuotes
                c == target && !inQuotes -> count += 1
            }
            i += 1
        }
        return count
    }

    private fun List<String>.value(headers: HeaderMap, name: String): String {
        val index = headers.indexOf(name)
        return if (index >= 0 && index < size) this[index] else ""
    }

    private fun List<String>.value(mapping: Map<InventoryImportField, Int?>, field: InventoryImportField): String {
        val index = mapping[field] ?: return ""
        return if (index >= 0 && index < size) this[index] else ""
    }

    private fun List<CreateItemRequestDto>.reduceInventoryRows(): CreateItemRequestDto {
        val first = first()
        return first.copy(
            quantity = sumOf { it.quantity },
            notes = joinDistinctNotBlank(map { it.notes }),
            description = first.description ?: firstNotNullOfOrNull { it.description },
            purchaseDate = first.purchaseDate ?: firstNotNullOfOrNull { it.purchaseDate },
            purchasePrice = first.purchasePrice ?: firstNotNullOfOrNull { it.purchasePrice },
            customFields = mergeCustomFields(flatMap { it.customFields })
        )
    }

    private fun mergeCustomFields(fields: List<ItemCustomFieldDto>): List<ItemCustomFieldDto> =
        fields
            .groupBy { it.fieldName.trim().lowercase(Locale.ROOT) }
            .values
            .mapNotNull { group ->
                val name = group.firstOrNull()?.fieldName?.trim().orEmpty()
                if (name.isBlank()) {
                    null
                } else {
                    ItemCustomFieldDto(
                        fieldName = name,
                        fieldValue = joinDistinctNotBlank(group.map { it.fieldValue })
                    )
                }
            }

    private fun List<CreateEventInventoryItemRequestDto>.reduceEventRows(): CreateEventInventoryItemRequestDto {
        val first = first()
        return first.copy(
            plannedQuantity = sumOf { it.plannedQuantity },
            notes = joinDistinctNotBlank(map { it.notes })
        )
    }

    private fun joinDistinctNotBlank(values: List<String?>): String? =
        values.mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }

    private fun CreateItemRequestDto.inventoryKey(): String =
        listOf(name, category, condition, type).joinToString("|") { it.normalizedKey() }

    fun inventoryKey(name: String, category: String, condition: String, type: String): String =
        listOf(name, category, condition, type).joinToString("|") { it.normalizedKey() }

    fun eventPlanKey(name: String): String = name.normalizedKey()

    private fun String.escapeCsv(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }

    private fun String.blankToNull(): String? = trim().ifBlank { null }

    private fun String.normalizeInventoryCategory(): String {
        val value = trim()
        if (value.isBlank()) return ""
        if (value.uppercase(Locale.ROOT).startsWith("CUSTOM_")) return value.uppercase(Locale.ROOT).take(30)
        val key = value.normalizedKey()
        return when (key) {
            "camping", "stovyklavimas", "stovyklavimo", "zygio", "zygiams", "palapines" -> "CAMPING"
            "tools", "irankiai", "irankiai remontui" -> "TOOLS"
            "cooking", "virtuve", "maistas", "maisto gamyba", "gaminimas" -> "COOKING"
            "first aid", "firstaid", "vaistinele", "pirma pagalba", "pirmos pagalbos" -> "FIRST_AID"
            "uniforms", "uniformos", "aprangos", "apranga" -> "UNIFORMS"
            "books", "knygos", "literatura" -> "BOOKS"
            "personal loans", "personalloans", "asmeniniai", "skolinami", "asmeniniai skolinimai" -> "PERSONAL_LOANS"
            else -> {
                val code = value.toCustomOptionCode(maxLength = 30)
                if (code.isBlank()) "CAMPING" else "CUSTOM_$code"
            }
        }
    }

    private fun String.normalizeCondition(): String {
        val key = trim().normalizedKey()
        return when (key) {
            "" -> ""
            "good", "gera", "geras", "tvarkinga", "tvarkingas", "veikia" -> "GOOD"
            "damaged", "sugadinta", "sugadintas", "pazeista", "pazeistas", "remontuotina", "blogesne" -> "DAMAGED"
            "written off", "writtenoff", "nurasyta", "nurasytas", "netinkama", "netinkamas" -> "WRITTEN_OFF"
            else -> {
                val code = trim().toCustomOptionCode(maxLength = 30)
                if (code.isBlank()) "GOOD" else "CUSTOM_$code"
            }
        }
    }

    private fun ItemDto.customFieldValue(name: String): String? =
        customFields.firstOrNull { it.fieldName.equals(name, ignoreCase = true) }?.fieldValue

    private fun String.normalizeInventoryType(defaultType: String): String {
        val key = trim().normalizedKey()
        return when (key) {
            "" -> defaultType
            "collective", "bendras", "tunto", "vieneto" -> "COLLECTIVE"
            "individual", "asmeninis", "asmenine" -> "INDIVIDUAL"
            "assigned", "priskirtas", "priskirta" -> "ASSIGNED"
            else -> defaultType
        }
    }

    private fun String.toCustomOptionCode(maxLength: Int): String {
        val withoutAccents = Normalizer.normalize(trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return withoutAccents
            .uppercase(Locale.ROOT)
            .replace("[^A-Z0-9]+".toRegex(), "_")
            .trim('_')
            .take(maxLength - "CUSTOM_".length)
    }

    private fun String.normalizedKey(): String {
        val withoutAccents = Normalizer.normalize(trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }

    private data class HeaderMap(
        private val indexes: Map<String, Int>,
        val unknownColumns: List<String>
    ) {
        fun has(name: String): Boolean = indexes.containsKey(name)
        fun indexOf(name: String): Int = indexes[name] ?: -1

        companion object {
            fun from(headers: List<String>, aliases: Map<String, List<String>>): HeaderMap {
                val aliasLookup = aliases.flatMap { (canonical, values) ->
                    values.map { it.normalizedAlias() to canonical }
                }.toMap()
                val indexes = mutableMapOf<String, Int>()
                val unknown = mutableListOf<String>()
                headers.forEachIndexed { index, rawHeader ->
                    val header = rawHeader.trim()
                    if (header.isBlank()) return@forEachIndexed
                    val canonical = aliasLookup[header.normalizedAlias()]
                    if (canonical == null) {
                        unknown += header
                    } else if (!indexes.containsKey(canonical)) {
                        indexes[canonical] = index
                    }
                }
                return HeaderMap(indexes, unknown.distinct())
            }

            private fun String.normalizedAlias(): String =
                Normalizer.normalize(trim(), Normalizer.Form.NFD)
                    .replace("\\p{Mn}+".toRegex(), "")
                    .lowercase(Locale.ROOT)
                    .replace("[^a-z0-9]+".toRegex(), "")
        }
    }

    private fun detectHeaderRowIndex(
        table: List<List<String>>,
        aliases: Map<String, List<String>>,
        requiredCanonical: String
    ): Int {
        if (table.isEmpty()) return 0
        val aliasLookup = aliases.flatMap { (canonical, values) ->
            values.map { normalizeAliasValue(it) to canonical }
        }.toMap()
        val candidates = table.take(10).mapIndexed { rowIndex, row ->
            val canonicalHits = row.mapNotNull { aliasLookup[normalizeAliasValue(it)] }.toSet()
            val score = canonicalHits.size
            Triple(rowIndex, score, requiredCanonical in canonicalHits)
        }
        val withRequired = candidates.filter { it.third }
        val best = (withRequired.ifEmpty { candidates }).maxByOrNull { it.second }
        return best?.first ?: 0
    }

    private fun normalizeAliasValue(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "")

    private object XlsxTableReader {
        fun readFirstSheet(bytes: ByteArray): List<List<String>> {
            val entries = unzip(bytes)
            val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"].orEmpty())
            val sheetPath = firstSheetPath(entries) ?: "xl/worksheets/sheet1.xml"
            val sheetXml = entries[sheetPath].orEmpty()
            if (sheetXml.isBlank()) return emptyList()
            return parseSheet(sheetXml, sharedStrings)
        }

        private fun unzip(bytes: ByteArray): Map<String, String> {
            val entries = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                        entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
            }
            return entries
        }

        private fun firstSheetPath(entries: Map<String, String>): String? {
            val workbook = entries["xl/workbook.xml"].orEmpty()
            val rels = entries["xl/_rels/workbook.xml.rels"].orEmpty()
            if (workbook.isBlank() || rels.isBlank()) return null
            val sheets = workbook.toXmlDocument().getElementsByTagName("sheet")
            if (sheets.length == 0) return null
            val relationId = sheets.item(0).attributes?.getNamedItem("r:id")?.nodeValue ?: return null
            val relationships = rels.toXmlDocument().getElementsByTagName("Relationship")
            for (i in 0 until relationships.length) {
                val node = relationships.item(i)
                if (node.attributes?.getNamedItem("Id")?.nodeValue == relationId) {
                    val target = node.attributes?.getNamedItem("Target")?.nodeValue ?: return null
                    return if (target.startsWith("/")) {
                        target.removePrefix("/")
                    } else {
                        "xl/${target.removePrefix("xl/")}"
                    }
                }
            }
            return null
        }

        private fun parseSharedStrings(xml: String): List<String> {
            if (xml.isBlank()) return emptyList()
            val nodes = xml.toXmlDocument().getElementsByTagName("si")
            return List(nodes.length) { index -> nodes.item(index).textContent.orEmpty() }
        }

        private fun parseSheet(xml: String, sharedStrings: List<String>): List<List<String>> {
            val rowNodes = xml.toXmlDocument().getElementsByTagName("row")
            val rows = mutableListOf<List<String>>()
            for (rowIndex in 0 until rowNodes.length) {
                val cellNodes = rowNodes.item(rowIndex).childNodes
                val cells = mutableMapOf<Int, String>()
                for (cellIndex in 0 until cellNodes.length) {
                    val cellNode = cellNodes.item(cellIndex)
                    if (cellNode.nodeName != "c") continue
                    val ref = cellNode.attributes?.getNamedItem("r")?.nodeValue.orEmpty()
                    val column = ref.takeWhile { it.isLetter() }.excelColumnIndex()
                    if (column < 0) continue
                    val type = cellNode.attributes?.getNamedItem("t")?.nodeValue
                    val raw = cellNode.childNodes.asSequence()
                        .firstOrNull { it.nodeName == "v" || it.nodeName == "is" }
                        ?.textContent
                        .orEmpty()
                    cells[column] = when (type) {
                        "s" -> sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                        else -> raw
                    }
                }
                val width = (cells.keys.maxOrNull() ?: -1) + 1
                val row = List(width) { cells[it].orEmpty() }
                if (row.any { it.isNotBlank() }) rows += row
            }
            return rows
        }

        private fun String.toXmlDocument() =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }.newDocumentBuilder().parse(ByteArrayInputStream(toByteArray(Charsets.UTF_8)))

        private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> = sequence {
            for (i in 0 until length) yield(item(i))
        }

        private fun String.excelColumnIndex(): Int {
            if (isBlank()) return -1
            var result = 0
            uppercase(Locale.ROOT).forEach { char ->
                result = result * 26 + (char - 'A' + 1)
            }
            return result - 1
        }
    }
}
