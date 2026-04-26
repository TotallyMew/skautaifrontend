package lt.skautai.android.ui.locations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateLocationRequestDto
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.util.TokenManager

@Composable
fun LocationListScreen(
    onLocationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.refresh()
            onRefreshHandled()
        }
    }

    if (uiState.isLoading && uiState.locations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null && uiState.locations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SkautaiErrorState(
                message = uiState.error!!,
                onRetry = viewModel::refresh
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Lokacijų katalogas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tvarkyk viešas, vieneto ir asmenines vietas bei jų sublokacijas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Pridėti lokaciją")
                    }
                }
            }
        }

        uiState.error?.let { error ->
            item {
                SkautaiErrorState(
                    message = error,
                    onRetry = viewModel::refresh
                )
            }
        }

        if (uiState.isEmpty) {
            item {
                SkautaiEmptyState(
                    title = "Lokacijų dar nėra",
                    subtitle = "Sukurk pirmą viešą, vieneto arba asmeninę lokaciją.",
                    icon = Icons.Default.Place,
                    actionLabel = "Pridėti lokaciją",
                    onAction = onCreateClick
                )
            }
        } else {
            locationSection(
                title = "Viešos",
                rootCandidates = uiState.locations.filter { it.visibility == "PUBLIC" },
                expandedIds = uiState.expandedIds,
                onToggle = viewModel::toggleExpanded,
                onLocationClick = onLocationClick
            )
            locationSection(
                title = "Mano vieneto",
                rootCandidates = uiState.locations.filter {
                    it.visibility == "UNIT" && it.ownerUnitId == uiState.activeUnitId
                },
                expandedIds = uiState.expandedIds,
                onToggle = viewModel::toggleExpanded,
                onLocationClick = onLocationClick
            )
            locationSection(
                title = "Mano",
                rootCandidates = uiState.locations.filter { it.visibility == "PRIVATE" },
                expandedIds = uiState.expandedIds,
                onToggle = viewModel::toggleExpanded,
                onLocationClick = onLocationClick
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.locationSection(
    title: String,
    rootCandidates: List<LocationDto>,
    expandedIds: Set<String>,
    onToggle: (String) -> Unit,
    onLocationClick: (String) -> Unit
) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    if (rootCandidates.isEmpty()) {
        item {
            Text("Įrašų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val nodesByParent = rootCandidates.groupBy { it.parentLocationId }
    val roots = rootCandidates
        .filter { it.parentLocationId == null }
        .sortedBy { it.fullPath.lowercase() }

    items(flattenLocations(roots, nodesByParent, expandedIds), key = { it.location.id }) { node ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLocationClick(node.location.id) }
                .padding(start = (node.depth * 16).dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (node.location.hasChildren) {
                IconButton(onClick = { onToggle(node.location.id) }) {
                    Icon(
                        imageVector = if (node.location.id in expandedIds) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }
            } else {
                Spacer(modifier = Modifier.padding(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(node.location.name, fontWeight = FontWeight.Medium)
                Text(
                    node.location.fullPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCreateChild: (String) -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(locationId) {
        viewModel.load(locationId)
    }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.load(locationId)
            onRefreshHandled()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lokacijos informacija") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading && uiState.location == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.location == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    SkautaiErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.load(locationId) }
                    )
                }
            }

            uiState.location != null -> {
                val location = uiState.location!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    location.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(location.fullPath, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tipas: ${visibilityLabel(location.visibility)}")
                                location.ownerUnitName?.let { Text("Vienetas: $it") }
                            }
                        }
                    }

                    location.address?.takeIf { it.isNotBlank() }?.let { address ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Adresas", fontWeight = FontWeight.SemiBold)
                                    Text(address)
                                    TextButton(
                                        onClick = {
                                            val uri = if (location.latitude != null && location.longitude != null) {
                                                Uri.parse(
                                                    "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
                                                )
                                            } else {
                                                Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null)
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Text("Atidaryti žemėlapyje")
                                    }
                                }
                            }
                        }
                    }

                    location.description?.takeIf { it.isNotBlank() }?.let { description ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Aprašymas", fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(description)
                                }
                            }
                        }
                    }

                    if (location.isEditable) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onEdit(location.id) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isDeleting
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("Redaguoti")
                                }
                                Button(
                                    onClick = { onCreateChild(location.id) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isDeleting
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("Pridėti sublokaciją")
                                }
                            }
                        }
                        item {
                            TextButton(
                                onClick = { viewModel.delete(location.id, onBack) },
                                enabled = !uiState.isDeleting
                            ) {
                                if (uiState.isDeleting) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(18.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Trinti")
                            }
                        }
                    }
                }
            }
        }
    }
}

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

    LaunchedEffect(locationId, parentLocationId) {
        viewModel.init(locationId, parentLocationId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSaved()
        }
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
                title = { Text(if (locationId == null) "Nauja lokacija" else "Redaguoti lokaciją") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
            }
            item {
                VisibilityField(selected = uiState.visibility, onSelected = viewModel::onVisibilityChange)
            }
            if (uiState.visibility == "UNIT") {
                item {
                    UnitField(
                        units = uiState.units,
                        selectedId = uiState.ownerUnitId,
                        onSelected = viewModel::onOwnerUnitChange,
                        error = uiState.ownerUnitError
                    )
                }
            }
            if (!uiState.parentPath.isNullOrBlank()) {
                item {
                    Text(
                        "Tėvinė lokacija: ${uiState.parentPath}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Adresas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Aprašymas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
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
                            modifier = Modifier.height(20.dp)
                        )
                    } else {
                        Text("Išsaugoti")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerField(
    label: String,
    locations: List<LocationDto>,
    selectedId: String?,
    onSelected: (LocationDto?) -> Unit,
    filter: (LocationDto) -> Boolean = { true }
) {
    var expanded by remember { mutableStateOf(false) }
    val selectable = remember(locations, selectedId) {
        locations.filter { it.isLeafSelectable }.sortedBy { it.fullPath.lowercase() }
    }.filter(filter)
    val selected = selectable.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.fullPath ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Nepasirinkta") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            selectable.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.fullPath) },
                    onClick = {
                        onSelected(location)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class VisibleLocationNode(
    val location: LocationDto,
    val depth: Int
)

private fun flattenLocations(
    roots: List<LocationDto>,
    nodesByParent: Map<String?, List<LocationDto>>,
    expandedIds: Set<String>
): List<VisibleLocationNode> {
    val result = mutableListOf<VisibleLocationNode>()

    fun append(node: LocationDto, depth: Int) {
        result += VisibleLocationNode(node, depth)
        if (node.id in expandedIds) {
            nodesByParent[node.id]
                .orEmpty()
                .sortedBy { it.name.lowercase() }
                .forEach { append(it, depth + 1) }
        }
    }

    roots.forEach { append(it, 0) }
    return result
}

private fun visibilityLabel(value: String): String = when (value) {
    "PRIVATE" -> "Asmeninė"
    "UNIT" -> "Vieneto"
    else -> "Vieša"
}

data class LocationListUiState(
    val isLoading: Boolean = true,
    val locations: List<LocationDto> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val activeUnitId: String? = null,
    val error: String? = null,
    val isEmpty: Boolean = false
)

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationListUiState())
    val uiState: StateFlow<LocationListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, activeUnitId = activeUnitId)
            locationRepository.getLocations()
                .onSuccess { locations ->
                    _uiState.value = LocationListUiState(
                        isLoading = false,
                        locations = locations,
                        expandedIds = locations.filter { it.parentLocationId == null }.map { it.id }.toSet(),
                        activeUnitId = activeUnitId,
                        isEmpty = locations.isEmpty()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Nepavyko gauti lokacijų",
                        isEmpty = _uiState.value.locations.isEmpty()
                    )
                }
        }
    }

    fun toggleExpanded(id: String) {
        val expanded = _uiState.value.expandedIds.toMutableSet()
        if (!expanded.add(id)) expanded.remove(id)
        _uiState.value = _uiState.value.copy(expandedIds = expanded)
    }
}

data class LocationDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val location: LocationDto? = null,
    val error: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    fun load(locationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            locationRepository.getLocation(locationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        location = it,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        location = null,
                        error = error.message ?: "Nepavyko gauti lokacijos"
                    )
                }
        }
    }

    fun delete(locationId: String, onBack: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            locationRepository.deleteLocation(locationId)
                .onSuccess { onBack() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        actionError = error.message ?: "Nepavyko ištrinti lokacijos"
                    )
                }
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
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
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationAddEditUiState())
    val uiState: StateFlow<LocationAddEditUiState> = _uiState.asStateFlow()

    fun init(locationId: String?, parentLocationId: String?) {
        viewModelScope.launch {
            val units = organizationalUnitRepository.getUnits().getOrDefault(emptyList())
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            val parent = parentLocationId?.let { locationRepository.getLocation(it).getOrNull() }
            val state = if (locationId != null) {
                val locationResult = locationRepository.getLocation(locationId)
                val location = locationResult.getOrNull()
                if (location == null) {
                    LocationAddEditUiState(
                        isLoading = false,
                        units = units,
                        error = locationResult.exceptionOrNull()?.message ?: "Nepavyko gauti lokacijos"
                    )
                } else {
                    LocationAddEditUiState(
                        isLoading = false,
                        name = location.name,
                        visibility = location.visibility,
                        ownerUnitId = location.ownerUnitId ?: activeUnitId,
                        units = units,
                        parentLocationId = location.parentLocationId ?: parentLocationId,
                        parentPath = parent?.fullPath,
                        address = location.address.orEmpty(),
                        description = location.description.orEmpty(),
                        latitude = location.latitude?.toString().orEmpty(),
                        longitude = location.longitude?.toString().orEmpty()
                    )
                }
            } else {
                LocationAddEditUiState(
                    isLoading = false,
                    visibility = parent?.visibility ?: "PUBLIC",
                    ownerUnitId = if (parent?.visibility == "UNIT") {
                        parent.ownerUnitId ?: activeUnitId
                    } else {
                        activeUnitId
                    },
                    units = units,
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
        _uiState.value = _uiState.value.copy(
            visibility = value,
            ownerUnitError = null,
            ownerUnitId = if (value == "UNIT") _uiState.value.ownerUnitId else null
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

    fun save(locationId: String?) {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val nameError = if (trimmedName.isBlank()) "Įveskite pavadinimą" else null
        val ownerUnitError = if (state.visibility == "UNIT" && state.ownerUnitId == null) {
            "Pasirinkite vienetą"
        } else {
            null
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
                        error = error.message ?: "Nepavyko išsaugoti lokacijos"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisibilityField(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("PUBLIC", "UNIT", "PRIVATE")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = visibilityLabel(selected),
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(visibilityLabel(option)) },
                    onClick = {
                        onSelected(option)
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
