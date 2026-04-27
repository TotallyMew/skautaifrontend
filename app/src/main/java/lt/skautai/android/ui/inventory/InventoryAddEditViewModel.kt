package lt.skautai.android.ui.inventory

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager

data class InventoryAddEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isCreatingLocation: Boolean = false,
    val isSuccess: Boolean = false,
    val formError: String? = null,
    val snackbarMessage: String? = null,
    val nameError: String? = null,
    val quantityError: String? = null,
    val categoryError: String? = null,
    val orgUnitError: String? = null,
    val name: String = "",
    val description: String = "",
    val type: String = "COLLECTIVE",
    val category: String = "CAMPING",
    val condition: String = "GOOD",
    val custodianId: String? = null,
    val origin: String = "UNIT_ACQUIRED",
    val quantity: String = "1",
    val notes: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val photoUrl: String = "",
    val selectedPhotoUri: String = "",
    val temporaryStorageLabel: String = "",
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedOrgUnitId: String = "",
    val locations: List<LocationDto> = emptyList(),
    val selectedLocationId: String = "",
    val tuntasId: String = "",
    val mode: String = "SHARED",
    val canManageLocations: Boolean = true
)

@HiltViewModel
class InventoryAddEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val locationRepository: LocationRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAddEditUiState())
    val uiState: StateFlow<InventoryAddEditUiState> = _uiState.asStateFlow()

    fun init(itemId: String?, mode: String?) {
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: ""
            val activeOrgUnitId = tokenManager.activeOrgUnitId.first().orEmpty()
            val resolvedMode = mode ?: "SHARED"
            val permissions = tokenManager.permissions.first()
            val canManageLocations = permissions.contains("locations.manage") ||
                permissions.contains("locations.manage:ALL") ||
                permissions.contains("locations.manage:OWN_UNIT")
            _uiState.value = _uiState.value.copy(
                tuntasId = tuntasId,
                isLoading = itemId != null,
                mode = resolvedMode,
                type = defaultTypeForMode(resolvedMode),
                origin = defaultOriginForMode(resolvedMode),
                selectedOrgUnitId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId else "",
                custodianId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId.ifBlank { null } else null,
                canManageLocations = canManageLocations
            )

            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(orgUnits = units)
                }

            locationRepository.getLocations()
                .onSuccess { locations ->
                    _uiState.value = _uiState.value.copy(locations = locations)
                }

            if (itemId != null) {
                itemRepository.getItem(itemId)
                    .onSuccess { item ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = item.name,
                            description = item.description ?: "",
                            type = item.type,
                            category = item.category,
                            condition = item.condition,
                            custodianId = item.custodianId,
                            origin = item.origin,
                            quantity = item.quantity.toString(),
                            notes = item.notes ?: "",
                            purchaseDate = item.purchaseDate ?: "",
                            purchasePrice = item.purchasePrice?.toString() ?: "",
                            photoUrl = item.photoUrl ?: "",
                            selectedPhotoUri = "",
                            temporaryStorageLabel = item.temporaryStorageLabel ?: "",
                            selectedOrgUnitId = item.custodianId ?: "",
                            selectedLocationId = item.locationId ?: ""
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            formError = error.message ?: "Nepavyko gauti daikto."
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null, formError = null)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value, quantityError = null, formError = null)
    }

    fun onPurchaseDateChange(value: String) {
        _uiState.value = _uiState.value.copy(purchaseDate = value)
    }

    fun onPurchasePriceChange(value: String) {
        _uiState.value = _uiState.value.copy(purchasePrice = value)
    }

    fun onPurchaseDateSelected(value: String?) {
        _uiState.value = _uiState.value.copy(purchaseDate = value.orEmpty())
    }

    fun onTypeChange(value: String) {
        _uiState.value = _uiState.value.copy(type = value)
    }

    fun onCategoryChange(value: String) {
        _uiState.value = _uiState.value.copy(category = value, categoryError = null, formError = null)
    }

    fun onConditionChange(value: String) {
        _uiState.value = _uiState.value.copy(condition = value)
    }

    fun onOriginChange(value: String) {
        _uiState.value = _uiState.value.copy(origin = value)
    }

    fun onTemporaryStorageLabelChange(value: String) {
        _uiState.value = _uiState.value.copy(temporaryStorageLabel = value)
    }

    fun onOrgUnitChange(unitId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = unitId ?: "",
            custodianId = unitId,
            orgUnitError = null,
            formError = null
        )
    }

    fun onLocationChange(locationId: String?) {
        _uiState.value = _uiState.value.copy(selectedLocationId = locationId ?: "")
    }

    fun createPrivateLocation(name: String) {
        if (!_uiState.value.canManageLocations) {
            _uiState.value = _uiState.value.copy(
                formError = "Lokacijas gali parinkti arba kurti tik vadovai."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingLocation = true, formError = null)
            locationRepository.createLocation(
                lt.skautai.android.data.remote.CreateLocationRequestDto(
                    name = name,
                    visibility = "PRIVATE"
                )
            ).onSuccess { location ->
                _uiState.value = _uiState.value.copy(
                    isCreatingLocation = false,
                    locations = (_uiState.value.locations + location).distinctBy { it.id },
                    selectedLocationId = location.id
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCreatingLocation = false,
                    formError = error.message ?: "Nepavyko sukurti lokacijos."
                )
            }
        }
    }

    fun showContextValidationErrors() {
        val state = _uiState.value
        val categoryError = if (state.category.isBlank()) "Pasirinkite inventoriaus kategorija." else null
        val orgUnitError = if (state.mode == "UNIT_OWN" && state.selectedOrgUnitId.isBlank()) {
            "Pasirinkite aktyvu vieneta."
        } else null
        _uiState.value = state.copy(
            categoryError = categoryError,
            orgUnitError = orgUnitError,
            formError = if (categoryError != null || orgUnitError != null) {
                "Patikslinkite pazymetus laukus."
            } else {
                null
            }
        )
    }

    fun showInfoValidationErrors() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Pavadinimas yra privalomas." else null
        val quantityError = if (state.quantity.toIntOrNull() == null || state.quantity.toIntOrNull() ?: 0 < 1) {
            "Kiekis turi buti teigiamas skaicius."
        } else null
        _uiState.value = state.copy(
            nameError = nameError,
            quantityError = quantityError,
            formError = if (nameError != null || quantityError != null) {
                "Patikslinkite pazymetus laukus."
            } else {
                null
            }
        )
    }

    fun showValidationError(message: String) {
        val state = _uiState.value
        _uiState.value = when {
            "kategorij" in message.lowercase() -> state.copy(
                categoryError = message,
                formError = "Patikslinkite pazymetus laukus."
            )
            "vieneto" in message.lowercase() -> state.copy(
                orgUnitError = message,
                formError = "Patikslinkite pazymetus laukus."
            )
            "pavadinimas" in message.lowercase() -> state.copy(
                nameError = message,
                formError = "Patikslinkite pazymetus laukus."
            )
            "kiekis" in message.lowercase() -> state.copy(
                quantityError = message,
                formError = "Patikslinkite pazymetus laukus."
            )
            else -> state.copy(formError = message)
        }
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingPhoto = true,
                selectedPhotoUri = uri.toString(),
                formError = null
            )
            uploadRepository.uploadImage(uri)
                .onSuccess { url ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        photoUrl = url
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        formError = error.message ?: "Nepavyko ikelti nuotraukos."
                    )
                }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun prepareNextItem() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            isSaving = false,
            formError = null,
            snackbarMessage = null,
            nameError = null,
            quantityError = null,
            categoryError = null,
            orgUnitError = null,
            name = "",
            description = "",
            condition = "GOOD",
            quantity = "1",
            notes = "",
            purchaseDate = "",
            purchasePrice = "",
            photoUrl = "",
            selectedPhotoUri = ""
        )
    }

    fun save(itemId: String?) {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Pavadinimas yra privalomas." else null
        val quantityError = if (state.quantity.toIntOrNull() == null || state.quantity.toIntOrNull() ?: 0 < 1) {
            "Kiekis turi buti teigiamas skaicius."
        } else null
        val categoryError = if (state.category.isBlank()) "Pasirinkite inventoriaus kategorija." else null
        val orgUnitError = if (state.mode == "UNIT_OWN" && state.selectedOrgUnitId.isBlank()) {
            "Pasirinkite aktyvu vieneta."
        } else {
            null
        }

        if (nameError != null || quantityError != null || categoryError != null || orgUnitError != null) {
            _uiState.value = state.copy(
                nameError = nameError,
                quantityError = quantityError,
                categoryError = categoryError,
                orgUnitError = orgUnitError,
                formError = "Patikslinkite pazymetus laukus."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isSaving = true,
                formError = null,
                nameError = null,
                quantityError = null,
                categoryError = null,
                orgUnitError = null
            )

            val quantity = state.quantity.toInt()
            val price = state.purchasePrice.toDoubleOrNull()
            val locationId = state.selectedLocationId.ifBlank { null }
            val custodianId = when (state.mode) {
                "UNIT_OWN" -> state.selectedOrgUnitId.ifBlank { null }
                "SHARED" -> null
                else -> state.custodianId
            }

            if (itemId == null) {
                val request = CreateItemRequestDto(
                    name = state.name.trim(),
                    description = state.description.ifBlank { null },
                    type = state.type,
                    category = state.category,
                    custodianId = custodianId,
                    origin = state.origin,
                    quantity = quantity,
                    condition = state.condition,
                    locationId = locationId,
                    temporaryStorageLabel = state.temporaryStorageLabel.ifBlank { null },
                    photoUrl = state.photoUrl.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price
                )
                itemRepository.createItem(request)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            isSuccess = true,
                            snackbarMessage = "Issaugota. Galite prideti kita daikta."
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            formError = error.message ?: "Nepavyko issaugoti daikto."
                        )
                    }
            } else {
                val request = UpdateItemRequestDto(
                    name = state.name.trim(),
                    description = state.description.ifBlank { null },
                    type = state.type,
                    category = state.category,
                    condition = state.condition,
                    quantity = quantity,
                    custodianId = custodianId,
                    locationId = locationId,
                    temporaryStorageLabel = state.temporaryStorageLabel.ifBlank { null },
                    photoUrl = state.photoUrl.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price
                )
                itemRepository.updateItem(itemId, request)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            formError = error.message ?: "Nepavyko atnaujinti daikto."
                        )
                    }
            }
        }
    }

    private fun defaultTypeForMode(mode: String): String = when (mode) {
        "PERSONAL" -> "INDIVIDUAL"
        "UNIT_OWN" -> "COLLECTIVE"
        else -> "COLLECTIVE"
    }

    private fun defaultOriginForMode(mode: String): String = when (mode) {
        "UNIT_OWN" -> "UNIT_ACQUIRED"
        "PERSONAL" -> "UNIT_ACQUIRED"
        else -> "UNIT_ACQUIRED"
    }
}
