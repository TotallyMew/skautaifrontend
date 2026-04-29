package lt.skautai.android.ui.locations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill

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
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    if (showDeleteDialog && uiState.location != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Trinti lokacija?") },
            text = {
                Text("Lokacija \"${uiState.location!!.name}\" bus istrinta. Veiksmo atsaukti nepavyks.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(uiState.location!!.id, onBack)
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Trinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Atšaukti")
                }
            }
        )
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LocationHeaderCard(location = location)
                    }

                    item {
                        SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Metaduomenys",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                MetadataRow("Tipas", visibilityLabel(location.visibility))
                                MetadataRow("Kelias", location.fullPath)
                                MetadataRow("Vienetas", location.ownerUnitName ?: "Netaikoma")
                                MetadataRow("Redagavimas", if (location.isEditable) "Leidziamas" else "Tik perziura")
                                MetadataRow("Sublokacijos", if (location.hasChildren) "Yra" else "Nera")
                            }
                        }
                    }

                    location.address?.takeIf { it.isNotBlank() }?.let { address ->
                        item {
                            SkautaiCard(
                                modifier = Modifier.fillMaxWidth(),
                                tonal = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Adresas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = address,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = {
                                            val uri = if (location.latitude != null && location.longitude != null) {
                                                Uri.parse(
                                                    "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
                                                )
                                            } else {
                                                Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                            }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null)
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text("Atidaryti zemelapyje")
                                    }
                                }
                            }
                        }
                    }

                    location.description?.takeIf { it.isNotBlank() }?.let { description ->
                        item {
                            SkautaiCard(
                                modifier = Modifier.fillMaxWidth(),
                                tonal = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Aprašymas",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (location.isEditable) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onEdit(location.id) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isDeleting
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Redaguoti")
                                }
                                Button(
                                    onClick = { onCreateChild(location.id) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isDeleting
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Pridėti sublokacija")
                                }
                            }
                        }

                        item {
                            TextButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !uiState.isDeleting,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (uiState.isDeleting) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Trinti lokacija")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationHeaderCard(location: LocationDto) {
    val parentTrail = locationParentTrail(location.fullPath, location.name)
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    parentTrail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (parentTrail == null) "Saknis" else "Pilnas kelias: ${location.fullPath}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
                Icon(
                    imageVector = detailLocationIcon(location),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill(visibilityLabel(location.visibility))
                location.ownerUnitName?.let { HeaderPill(it) }
                HeaderPill(if (location.hasChildren) "Turi sublokaciju" else "Be sublokaciju")
            }
        }
    }
}

@Composable
private fun HeaderPill(label: String) {
    SkautaiStatusPill(
        label = label,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

private fun detailLocationIcon(location: LocationDto) = when {
    location.parentLocationId == null && location.hasChildren -> Icons.Default.Business
    location.hasChildren -> Icons.Default.Folder
    location.visibility == "PUBLIC" -> Icons.Default.Public
    else -> Icons.Default.Place
}

private fun locationParentTrail(fullPath: String, currentName: String): String? {
    val segments = fullPath.split("/").map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.isEmpty()) return null
    val withoutCurrent = if (segments.lastOrNull() == currentName) segments.dropLast(1) else segments
    return withoutCurrent.takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

private fun visibilityLabel(value: String): String = when (value) {
    "PRIVATE" -> "Asmenine"
    "UNIT" -> "Vieneto"
    else -> "Viesa"
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
                        actionError = error.message ?: "Nepavyko istrinti lokacijos"
                    )
                }
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
