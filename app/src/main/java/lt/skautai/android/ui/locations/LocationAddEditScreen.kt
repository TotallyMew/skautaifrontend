package lt.skautai.android.ui.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateLocationRequestDto
import lt.skautai.android.data.remote.LocationVisibility
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateLocationRequestDto
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.util.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAddEditScreen(
    locationId: String?,
    parentLocationId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LocationAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isCreateMode = locationId == null

    LaunchedEffect(locationId, parentLocationId) {
        viewModel.init(locationId, parentLocationId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSaved()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreateMode) "Nauja lokacija" else "Redaguoti lokacija") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isCreateMode) "Sukurkite nauja lokacija" else "Atnaujinkite lokacijos informacija",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.parentPath?.let {
                                    "Lokacija bus kuriama po: $it"
                                } ?: "Uzpildykite pagrindinius duomenis, kad vieta butu lengvai randama kataloge.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            }

            item {
                SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SkautaiSectionHeader(
                            title = "Pagrindine informacija",
                            subtitle = "Pavadinimas, matomumas ir priklausomybe vienetui."
                        )

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("Pavadinimas") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.nameError != null,
                            supportingText = {
                                uiState.nameError?.let { Text(it) }
                            }
                        )

                        VisibilityField(
                            selected = uiState.visibility,
                            onSelected = viewModel::onVisibilityChange
                        )

                        if (uiState.visibility == "UNIT") {
                            UnitField(
                                units = uiState.units,
                                selectedId = uiState.ownerUnitId,
                                onSelected = viewModel::onOwnerUnitChange,
                                error = uiState.ownerUnitError
                            )
                        }

                        if (!uiState.parentPath.isNullOrBlank()) {
                            Text(
                                text = "Tevine lokacija: ${uiState.parentPath}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SkautaiSectionHeader(
                            title = "Kontaktine vieta",
                            subtitle = "Adresas ir trumpas apra?ymas, kurie padeda rasti viet?."
                        )

                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = viewModel::onAddressChange,
                            label = { Text("Adresas") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = { Text("Aprašymas") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            item {
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SkautaiSectionHeader(
                            title = "Koordinates",
                            subtitle = "Neprivaloma, bet naudinga atidarymui zemelapyje."
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.latitude,
                                onValueChange = viewModel::onLatitudeChange,
                                label = { Text("Platuma") },
                                placeholder = { Text("Pvz. 54.6872") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.longitude,
                                onValueChange = viewModel::onLongitudeChange,
                                label = { Text("Ilguma") },
                                placeholder = { Text("Pvz. 25.2797") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.save(locationId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(if (isCreateMode) "Sukurti lokacija" else "Išsaugoti pakeitimus")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisibilityField(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVisibility = LocationVisibility.fromApiValue(selected)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedVisibility.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LocationVisibility.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSelected(option.apiValue)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitField(
    units: List<OrganizationalUnitDto>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.id == selectedId }?.name.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Vienetas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            isError = error != null,
            supportingText = {
                error?.let { Text(it) }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onSelected(unit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class LocationAddEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val name: String = "",
    val nameError: String? = null,
    val visibility: String = "PUBLIC",
    val ownerUnitId: String? = null,
    val ownerUnitError: String? = null,
    val units: List<OrganizationalUnitDto> = emptyList(),
    val allowedUnitIds: Set<String> = emptySet(),
    val canManageAllUnits: Boolean = false,
    val parentLocationId: String? = null,
    val parentPath: String? = null,
    val address: String = "",
    val description: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val error: String? = null
)

@HiltViewModel
class LocationAddEditViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val organizationalUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationAddEditUiState())
    val uiState: StateFlow<LocationAddEditUiState> = _uiState.asStateFlow()

    fun init(locationId: String?, parentLocationId: String?) {
        viewModelScope.launch {
            val permissions = tokenManager.permissions.first()
            val canManageAllUnits = "locations.manage:ALL" in permissions
            val currentUserId = tokenManager.userId.first()
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            val allUnits = organizationalUnitRepository.getUnits().getOrDefault(emptyList())
            val allowedUnitIds = resolveAllowedUnitIds(currentUserId, activeUnitId)
            val visibleUnits = if (canManageAllUnits) {
                allUnits
            } else {
                allUnits.filter { it.id in allowedUnitIds }
            }
            val parent = parentLocationId?.let { locationRepository.getLocation(it).getOrNull() }
            val state = if (locationId != null) {
                val locationResult = locationRepository.getLocation(locationId)
                val location = locationResult.getOrNull()
                if (location == null) {
                    LocationAddEditUiState(
                        isLoading = false,
                        units = visibleUnits,
                        allowedUnitIds = allowedUnitIds,
                        canManageAllUnits = canManageAllUnits,
                        error = locationResult.exceptionOrNull()?.message ?: "Nepavyko gauti lokacijos."
                    )
                } else {
                    val resolvedOwnerUnitId = when {
                        location.visibility != "UNIT" -> null
                        canManageAllUnits || location.ownerUnitId in allowedUnitIds -> location.ownerUnitId ?: activeUnitId
                        else -> null
                    }
                    LocationAddEditUiState(
                        isLoading = false,
                        name = location.name,
                        visibility = location.visibility,
                        ownerUnitId = resolvedOwnerUnitId,
                        ownerUnitError = if (
                            location.visibility == "UNIT" &&
                            location.ownerUnitId != null &&
                            !canManageAllUnits &&
                            location.ownerUnitId !in allowedUnitIds
                        ) {
                            "Galite naudoti tik savo vienetus."
                        } else {
                            null
                        },
                        units = visibleUnits,
                        allowedUnitIds = allowedUnitIds,
                        canManageAllUnits = canManageAllUnits,
                        parentLocationId = location.parentLocationId ?: parentLocationId,
                        parentPath = parent?.fullPath,
                        address = location.address.orEmpty(),
                        description = location.description.orEmpty(),
                        latitude = location.latitude?.toString().orEmpty(),
                        longitude = location.longitude?.toString().orEmpty()
                    )
                }
            } else {
                val defaultOwnerUnitId = when {
                    parent?.visibility == "UNIT" && (canManageAllUnits || parent.ownerUnitId in allowedUnitIds) ->
                        parent.ownerUnitId ?: activeUnitId
                    else -> activeUnitId?.takeIf { canManageAllUnits || it in allowedUnitIds }
                }
                LocationAddEditUiState(
                    isLoading = false,
                    visibility = parent?.visibility ?: "PUBLIC",
                    ownerUnitId = if ((parent?.visibility ?: "PUBLIC") == "UNIT") defaultOwnerUnitId else null,
                    units = visibleUnits,
                    allowedUnitIds = allowedUnitIds,
                    canManageAllUnits = canManageAllUnits,
                    parentLocationId = parentLocationId,
                    parentPath = parent?.fullPath
                )
            }
            _uiState.value = state
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun onVisibilityChange(value: String) {
        val state = _uiState.value
        val fallbackUnitId = state.ownerUnitId
            ?: state.units.firstOrNull()?.id
            ?: state.allowedUnitIds.firstOrNull()
        _uiState.value = state.copy(
            visibility = value,
            ownerUnitError = null,
            ownerUnitId = if (value == "UNIT") fallbackUnitId else null
        )
    }

    fun onOwnerUnitChange(value: String?) {
        _uiState.value = _uiState.value.copy(ownerUnitId = value, ownerUnitError = null)
    }

    fun onAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onLatitudeChange(value: String) {
        _uiState.value = _uiState.value.copy(latitude = value)
    }

    fun onLongitudeChange(value: String) {
        _uiState.value = _uiState.value.copy(longitude = value)
    }

    fun save(locationId: String?) {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val nameError = if (trimmedName.isBlank()) "Iveskite pavadinima." else null
        val ownerUnitError = when {
            state.visibility != "UNIT" -> null
            state.ownerUnitId == null -> "Pasirinkite vienetą."
            !state.canManageAllUnits && state.ownerUnitId !in state.allowedUnitIds ->
                "Galite pasirinkti tik savo vieneta."
            else -> null
        }
        if (nameError != null || ownerUnitError != null) {
            _uiState.value = state.copy(
                nameError = nameError,
                ownerUnitError = ownerUnitError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val result = if (locationId == null) {
                locationRepository.createLocation(
                    CreateLocationRequestDto(
                        name = trimmedName,
                        visibility = state.visibility,
                        parentLocationId = state.parentLocationId,
                        ownerUnitId = state.ownerUnitId,
                        address = state.address.ifBlank { null },
                        description = state.description.ifBlank { null },
                        latitude = state.latitude.toDoubleOrNull(),
                        longitude = state.longitude.toDoubleOrNull()
                    )
                )
            } else {
                locationRepository.updateLocation(
                    locationId,
                    UpdateLocationRequestDto(
                        name = trimmedName,
                        visibility = state.visibility,
                        parentLocationId = state.parentLocationId,
                        ownerUnitId = state.ownerUnitId,
                        address = state.address.ifBlank { null },
                        description = state.description.ifBlank { null },
                        latitude = state.latitude.toDoubleOrNull(),
                        longitude = state.longitude.toDoubleOrNull()
                    )
                )
            }
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Nepavyko issaugoti lokacijos."
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun resolveAllowedUnitIds(
        currentUserId: String?,
        activeUnitId: String?
    ): Set<String> {
        val ids = linkedSetOf<String>()
        activeUnitId?.let(ids::add)
        if (currentUserId == null) return ids

        val member = memberRepository.getMember(currentUserId).getOrNull() ?: return ids
        member.unitAssignments.orEmpty()
            .map { it.organizationalUnitId }
            .forEach(ids::add)
        member.leadershipRoles
            .filter { it.termStatus == "ACTIVE" }
            .mapNotNull { it.organizationalUnitId }
            .forEach(ids::add)
        return ids
    }
}
