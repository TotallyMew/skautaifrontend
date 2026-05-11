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
import lt.skautai.android.data.remote.ItemCustomFieldDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.hasPermissionAll

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
    val unitOfMeasure: String = "vnt.",
    val tags: String = "",
    val statusReason: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val customFields: List<CustomFieldInput> = emptyList(),
    val photoUrl: String = "",
    val selectedPhotoUri: String = "",
    val temporaryStorageLabel: String = "",
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedOrgUnitId: String = "",
    val locations: List<LocationDto> = emptyList(),
    val selectedLocationId: String = "",
    val members: List<MemberDto> = emptyList(),
    val selectedResponsibleUserId: String = "",
    val tuntasId: String = "",
    val mode: String = "SHARED",
    val canManageLocations: Boolean = true,
    val canCreateSharedDirectly: Boolean = false,
    val duplicateCandidate: ItemDto? = null
)

data class CustomFieldInput(
    val fieldName: String = "",
    val fieldValue: String = ""
)

@HiltViewModel
class InventoryAddEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val locationRepository: LocationRepository,
    private val memberRepository: MemberRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAddEditUiState())
    val uiState: StateFlow<InventoryAddEditUiState> = _uiState.asStateFlow()
    private var pendingCreateRequest: CreateItemRequestDto? = null

    fun init(itemId: String?, mode: String?) {
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: ""
            val activeOrgUnitId = tokenManager.activeOrgUnitId.first().orEmpty()
            val resolvedMode = mode ?: "SHARED"
            val permissions = tokenManager.permissions.first()
            val canManageLocations = permissions.contains("locations.manage") ||
                permissions.contains("locations.manage:ALL") ||
                permissions.contains("locations.manage:OWN_UNIT")
            val canCreateSharedDirectly = permissions.hasPermissionAll("items.create")
            _uiState.value = _uiState.value.copy(
                tuntasId = tuntasId,
                isLoading = itemId != null,
                mode = resolvedMode,
                type = defaultTypeForMode(resolvedMode),
                origin = defaultOriginForMode(resolvedMode),
                selectedOrgUnitId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId else "",
                custodianId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId.ifBlank { null } else null,
                canManageLocations = canManageLocations,
                canCreateSharedDirectly = canCreateSharedDirectly
            )

            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(orgUnits = units)
                }

            locationRepository.getLocations()
                .onSuccess { locations ->
                    _uiState.value = _uiState.value.copy(locations = locations)
                }

            memberRepository.getMembers()
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(members = members.members)
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
                            unitOfMeasure = item.customFields.fieldValue("Mato vienetas") ?: "vnt.",
                            tags = item.customFields.fieldValue("Žymos").orEmpty(),
                            statusReason = item.customFields.fieldValue("Priežastis").orEmpty(),
                            purchaseDate = item.purchaseDate ?: "",
                            purchasePrice = item.purchasePrice?.toString() ?: "",
                            customFields = item.customFields.orEmpty()
                                .filterNot { field -> managedCustomFieldNames.any { it.equals(field.fieldName, ignoreCase = true) } }
                                .map {
                                    CustomFieldInput(
                                        fieldName = it.fieldName,
                                        fieldValue = it.fieldValue.orEmpty()
                                    )
                                },
                            photoUrl = item.photoUrl ?: "",
                            selectedPhotoUri = "",
                            temporaryStorageLabel = item.temporaryStorageLabel ?: "",
                            selectedOrgUnitId = item.custodianId ?: "",
                            selectedLocationId = item.locationId ?: "",
                            selectedResponsibleUserId = item.responsibleUserId ?: ""
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

    fun onUnitOfMeasureChange(value: String) {
        _uiState.value = _uiState.value.copy(unitOfMeasure = value)
    }

    fun onTagsChange(value: String) {
        _uiState.value = _uiState.value.copy(tags = value)
    }

    fun onStatusReasonChange(value: String) {
        _uiState.value = _uiState.value.copy(statusReason = value)
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

    fun addCustomField() {
        val state = _uiState.value
        _uiState.value = state.copy(customFields = state.customFields + CustomFieldInput())
    }

    fun removeCustomField(index: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(customFields = state.customFields.filterIndexed { i, _ -> i != index })
    }

    fun onCustomFieldNameChange(index: Int, value: String) {
        updateCustomField(index) { it.copy(fieldName = value) }
    }

    fun onCustomFieldValueChange(index: Int, value: String) {
        updateCustomField(index) { it.copy(fieldValue = value) }
    }

    private fun updateCustomField(index: Int, transform: (CustomFieldInput) -> CustomFieldInput) {
        val state = _uiState.value
        _uiState.value = state.copy(
            customFields = state.customFields.mapIndexed { i, field ->
                if (i == index) transform(field) else field
            }
        )
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

    fun onResponsibleUserChange(userId: String?) {
        _uiState.value = _uiState.value.copy(selectedResponsibleUserId = userId.orEmpty())
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
        val categoryError = if (state.category.isBlank()) "Pasirinkite inventoriaus kategoriją." else null
        val orgUnitError = if (state.mode == "UNIT_OWN" && state.selectedOrgUnitId.isBlank()) {
            "Pasirinkite aktyvų vienetą."
        } else null
        _uiState.value = state.copy(
            categoryError = categoryError,
            orgUnitError = orgUnitError,
            formError = if (categoryError != null || orgUnitError != null) {
                "Patikslinkite pažymėtus laukus."
            } else {
                null
            }
        )
    }

    fun showInfoValidationErrors() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Pavadinimas yra privalomas." else null
        val quantityError = if (state.quantity.toIntOrNull() == null || state.quantity.toIntOrNull() ?: 0 < 1) {
            "Kiekis turi būti teigiamas skaičius."
        } else null
        _uiState.value = state.copy(
            nameError = nameError,
            quantityError = quantityError,
            formError = if (nameError != null || quantityError != null) {
                "Patikslinkite pažymėtus laukus."
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
                formError = "Patikslinkite pažymėtus laukus."
            )
            "vieneto" in message.lowercase() -> state.copy(
                orgUnitError = message,
                formError = "Patikslinkite pažymėtus laukus."
            )
            "pavadinimas" in message.lowercase() -> state.copy(
                nameError = message,
                formError = "Patikslinkite pažymėtus laukus."
            )
            "kiekis" in message.lowercase() -> state.copy(
                quantityError = message,
                formError = "Patikslinkite pažymėtus laukus."
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
                        formError = error.message ?: "Nepavyko įkelti nuotraukos."
                    )
                }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun dismissDuplicateDialog() {
        pendingCreateRequest = null
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            duplicateCandidate = null
        )
    }

    fun createNewDuplicateRecord() {
        val request = pendingCreateRequest ?: return
        submitCreateRequest(
            request.copy(
                duplicateHandling = "CREATE_NEW",
                duplicateTargetItemId = null
            )
        )
    }

    fun addToExistingDuplicate() {
        val request = pendingCreateRequest ?: return
        val duplicateItemId = _uiState.value.duplicateCandidate?.id ?: return
        submitCreateRequest(
            request.copy(
                duplicateHandling = "ADD_TO_EXISTING",
                duplicateTargetItemId = duplicateItemId
            )
        )
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
            unitOfMeasure = "vnt.",
            tags = "",
            statusReason = "",
            purchaseDate = "",
            purchasePrice = "",
            customFields = emptyList(),
            photoUrl = "",
            selectedPhotoUri = "",
            duplicateCandidate = null
        )
    }

    fun save(itemId: String?) {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Pavadinimas yra privalomas." else null
        val quantityError = if (state.quantity.toIntOrNull() == null || state.quantity.toIntOrNull() ?: 0 < 1) {
            "Kiekis turi būti teigiamas skaičius."
        } else null
        val categoryError = if (state.category.isBlank()) "Pasirinkite inventoriaus kategoriją." else null
        val orgUnitError = if (state.mode == "UNIT_OWN" && state.selectedOrgUnitId.isBlank()) {
            "Pasirinkite aktyvų vienetą."
        } else {
            null
        }

        if (nameError != null || quantityError != null || categoryError != null || orgUnitError != null) {
            _uiState.value = state.copy(
                nameError = nameError,
                quantityError = quantityError,
                categoryError = categoryError,
                orgUnitError = orgUnitError,
                formError = "Patikslinkite pažymėtus laukus."
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
            val customFields = state.customFields
                .mapNotNull { field ->
                    val name = field.fieldName.trim()
                    if (name.isBlank() || managedCustomFieldNames.any { it.equals(name, ignoreCase = true) }) {
                        null
                    } else {
                        ItemCustomFieldDto(
                            fieldName = name,
                            fieldValue = field.fieldValue.trim().ifBlank { null }
                        )
                    }
                }
                .let { fields ->
                    fields + listOfNotNull(
                        ItemCustomFieldDto(
                            fieldName = "Mato vienetas",
                            fieldValue = state.unitOfMeasure.trim().ifBlank { "vnt." }
                        ),
                        state.tags.trim().takeIf { it.isNotBlank() }?.let {
                            ItemCustomFieldDto(fieldName = "Žymos", fieldValue = it)
                        },
                        state.statusReason.trim().takeIf { it.isNotBlank() }?.let {
                            ItemCustomFieldDto(fieldName = "Priežastis", fieldValue = it)
                        }
                    )
                }
            val locationId = state.selectedLocationId.ifBlank { null }
            val responsibleUserId = state.selectedResponsibleUserId.ifBlank { null }
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
                    responsibleUserId = responsibleUserId,
                    photoUrl = state.photoUrl.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price,
                    customFields = customFields
                )
                itemRepository.findDuplicateCandidate(
                    name = request.name,
                    type = request.type,
                    category = request.category,
                    custodianId = request.custodianId
                ).onSuccess { duplicate ->
                    if (duplicate != null) {
                        pendingCreateRequest = request
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            duplicateCandidate = duplicate
                        )
                    } else {
                        submitCreateRequest(request)
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        formError = error.message ?: "Nepavyko patikrinti dubliuojamų daiktų."
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
                    responsibleUserId = responsibleUserId,
                    photoUrl = state.photoUrl.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price,
                    customFields = customFields,
                    clearCustodianId = custodianId == null,
                    clearLocationId = locationId == null,
                    clearResponsibleUserId = responsibleUserId == null
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

    private fun submitCreateRequest(request: CreateItemRequestDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                formError = null,
                duplicateCandidate = null
            )
            itemRepository.createItem(request)
                .onSuccess {
                    pendingCreateRequest = null
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSuccess = true,
                        snackbarMessage = "Išsaugota. Galite pridėti kitą daiktą."
                    )
                }
                .onFailure { error ->
                    pendingCreateRequest = null
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        formError = error.message ?: "Nepavyko išsaugoti daikto."
                    )
                }
        }
    }
}

private val managedCustomFieldNames = setOf("Mato vienetas", "Žymos", "Priežastis")

private fun List<ItemCustomFieldDto>.fieldValue(name: String): String? =
    firstOrNull { it.fieldName.equals(name, ignoreCase = true) }?.fieldValue
