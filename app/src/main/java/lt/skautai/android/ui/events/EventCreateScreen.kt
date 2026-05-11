package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.SkautaiSummaryCard
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    eventId: String?,
    onBack: () -> Unit,
    viewModel: EventCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectingDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    selectingDate?.let { target ->
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { selectingDate = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString()
                            if (target == "start") viewModel.onStartDateChange(date)
                            else viewModel.onEndDateChange(date)
                        }
                        selectingDate = null
                    }
                ) { Text("Pasirinkti") }
            },
            dismissButton = {
                TextButton(onClick = { selectingDate = null }) { Text("Uždaryti") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    EventScreenScaffold(
        title = if (uiState.isEditMode) "Redaguoti renginį" else "Naujas renginys",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EventCreateHero(uiState = uiState)

            EventFormSection(
                title = "Pagrindinė informacija",
                subtitle = "Aiškus pavadinimas, tipas ir datos padeda komandai greitai suprasti renginio rėmus."
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 24.dp))
                    }
                }

                SkautaiTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Pavadinimas *",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                EventTypeDropdown(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange,
                    enabled = !uiState.isEditMode
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EventTonalDateButton(
                        label = "Pradžia",
                        value = uiState.startDate.takeIf { it.isNotBlank() },
                        onClick = { selectingDate = "start" },
                        enabled = !uiState.isEditMode,
                        modifier = Modifier.weight(1f)
                    )
                    EventTonalDateButton(
                        label = "Pabaiga",
                        value = uiState.endDate.takeIf { it.isNotBlank() },
                        onClick = { selectingDate = "end" },
                        enabled = !uiState.isEditMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                EventAudienceDropdown(
                    selectedAudienceId = uiState.selectedAudienceId,
                    selectedAudienceLabel = uiState.selectedAudienceLabel,
                    audienceOptions = uiState.audienceOptions,
                    enabled = !uiState.isEditMode && !uiState.isAudienceLoading,
                    onAudienceSelected = viewModel::onAudienceChange
                )

                if (!uiState.isEditMode && !uiState.isAudienceLoading && uiState.audienceOptions.isEmpty()) {
                    Text(
                        text = "Pagal dabartinį rangą ir vienetus šiuo metu negalite kurti renginių.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            EventFormSection(
                title = "Pastabos",
                subtitle = "Trumpai aprašyk tik tai, kas svarbu vadovams, ūkvedžiui ar štabui."
            ) {
                SkautaiTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = "Pastabos",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6
                )
            }

            EventPrimaryButton(
                text = if (uiState.isSaving) "Saugoma..." else if (uiState.isEditMode) "Išsaugoti pakeitimus" else "Sukurti renginį",
                onClick = viewModel::saveEvent,
                enabled = !uiState.isSaving && !uiState.isLoading
            )

            if (uiState.isSaving) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun EventCreateHero(uiState: EventCreateUiState) {
    val subtitle = if (uiState.isEditMode) {
        "Atnaujink svarbiausius duomenis, kad tolesni planavimo ekranai liktų aiškūs visam štabui."
    } else {
        "Susikurk renginio pagrindą, nuo kurio prasidės planas, poreikiai, štabas ir inventoriaus eiga."
    }

    SkautaiSummaryCard(
        eyebrow = "Renginio nustatymai",
        title = if (uiState.isEditMode) "Renginio redagavimas" else "Naujas renginys",
        subtitle = subtitle,
        foresty = true,
        metrics = listOf(
            "Tipas" to eventTypeLabel(uiState.type),
            "Pradžia" to uiState.startDate.ifBlank { "--" },
            "Pabaiga" to uiState.endDate.ifBlank { "--" }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    enabled: Boolean
) {
    val types = listOf(
        "STOVYKLA" to "Stovykla",
        "SUEIGA" to "Sueiga",
        "RENGINYS" to "Renginys"
    )
    var expanded by remember { mutableStateOf(false) }
    var customType by remember(selectedType) {
        mutableStateOf(if (types.none { it.first == selectedType }) selectedType.toEventCustomInput() else "")
    }
    val selectedLabel = types.find { it.first == selectedType }?.second ?: selectedType.toEventDisplayLabel()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Tipas *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = eventFormFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            types.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onTypeSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }

    if (enabled) {
        SkautaiTextField(
            value = customType,
            onValueChange = {
                customType = it
                val normalized = it.toEventOptionCode(maxLength = 20)
                if (normalized.isNotBlank()) onTypeSelected(normalized)
            },
            label = "Kitas tipas",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )
    }
}

private fun String.toEventOptionCode(maxLength: Int): String =
    ("CUSTOM_" + toEventCustomInput())
        .uppercase()
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
        .take(maxLength)

private fun String.toEventCustomInput(): String =
    trim()
        .replace(Regex("^CUSTOM_", RegexOption.IGNORE_CASE), "")
        .replace('_', ' ')

private fun String.toEventDisplayLabel(): String =
    toEventCustomInput()
        .lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventAudienceDropdown(
    selectedAudienceId: String?,
    selectedAudienceLabel: String,
    audienceOptions: List<EventAudienceOption>,
    enabled: Boolean,
    onAudienceSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = audienceOptions
        .firstOrNull { it.organizationalUnitId == selectedAudienceId }
        ?.label
        ?: selectedAudienceLabel

    if (enabled && audienceOptions.size > 6) {
        SearchablePickerField(
            label = "Kam skirtas renginys *",
            value = selectedLabel,
            options = audienceOptions.map { (it.organizationalUnitId ?: "") to it.label },
            onSelect = { onAudienceSelected(it.ifBlank { null }) }
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Kam skirtas renginys *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = eventFormFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            audienceOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onAudienceSelected(option.organizationalUnitId)
                        expanded = false
                    }
                )
            }
        }
    }
}
