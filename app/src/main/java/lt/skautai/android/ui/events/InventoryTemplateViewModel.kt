package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateInventoryTemplateRequestDto
import lt.skautai.android.data.remote.InventoryTemplateDto
import lt.skautai.android.data.remote.InventoryTemplateItemRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.UpdateInventoryTemplateRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository

data class InventoryTemplateEditorItem(
    val itemId: String? = null,
    val itemName: String = "",
    val quantity: String = "1",
    val category: String = "",
    val notes: String = ""
)

data class InventoryTemplateEditorState(
    val templateId: String? = null,
    val name: String = "",
    val eventType: String = "STOVYKLA",
    val items: List<InventoryTemplateEditorItem> = listOf(InventoryTemplateEditorItem())
)

data class InventoryTemplateUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val templates: List<InventoryTemplateDto> = emptyList(),
    val inventoryItems: List<ItemDto> = emptyList(),
    val filterEventType: String? = null,
    val editor: InventoryTemplateEditorState? = null,
    val deleteTarget: InventoryTemplateDto? = null,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class InventoryTemplateViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryTemplateUiState())
    val uiState: StateFlow<InventoryTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        loadInventoryItems()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.getInventoryTemplates(_uiState.value.filterEventType)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        templates = response.templates
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Nepavyko gauti sablonu"
                    )
                }
        }
    }

    fun loadInventoryItems() {
        viewModelScope.launch {
            itemRepository.getItems(status = "ACTIVE")
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        inventoryItems = items.sortedBy { it.name.lowercase() }
                    )
                }
        }
    }

    fun setFilter(eventType: String?) {
        _uiState.value = _uiState.value.copy(filterEventType = eventType)
        loadTemplates()
    }

    fun startCreate() {
        _uiState.value = _uiState.value.copy(
            editor = InventoryTemplateEditorState(eventType = _uiState.value.filterEventType ?: "STOVYKLA")
        )
    }

    fun startEdit(template: InventoryTemplateDto) {
        _uiState.value = _uiState.value.copy(
            editor = InventoryTemplateEditorState(
                templateId = template.id,
                name = template.name,
                eventType = template.eventType ?: "STOVYKLA",
                items = template.items.map {
                    InventoryTemplateEditorItem(
                        itemId = it.itemId,
                        itemName = it.itemName,
                        quantity = it.quantity.toString(),
                        category = it.category.orEmpty(),
                        notes = it.notes.orEmpty()
                    )
                }.ifEmpty { listOf(InventoryTemplateEditorItem()) }
            )
        )
    }

    fun closeEditor() {
        _uiState.value = _uiState.value.copy(editor = null)
    }

    fun onNameChange(value: String) = updateEditor { it.copy(name = value) }
    fun onEventTypeChange(value: String) = updateEditor { it.copy(eventType = value) }

    fun onItemChange(index: Int, item: InventoryTemplateEditorItem) {
        updateEditor { editor ->
            editor.copy(items = editor.items.mapIndexed { i, current -> if (i == index) item else current })
        }
    }

    fun addItemRow() {
        updateEditor { it.copy(items = it.items + InventoryTemplateEditorItem()) }
    }

    fun removeItemRow(index: Int) {
        updateEditor { editor ->
            val next = editor.items.filterIndexed { i, _ -> i != index }
            editor.copy(items = next.ifEmpty { listOf(InventoryTemplateEditorItem()) })
        }
    }

    fun requestDelete(template: InventoryTemplateDto) {
        _uiState.value = _uiState.value.copy(deleteTarget = template)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(deleteTarget = null)
    }

    fun saveTemplate() {
        val editor = _uiState.value.editor ?: return
        val name = editor.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Iveskite sablono pavadinima")
            return
        }
        val items = editor.items
            .mapNotNull { row ->
                val itemName = row.itemName.trim()
                if (itemName.isBlank()) return@mapNotNull null
                InventoryTemplateItemRequestDto(
                    itemId = row.itemId,
                    itemName = itemName,
                    quantity = row.quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    category = row.category.trim().takeIf { it.isNotBlank() && it != "CUSTOM" },
                    notes = row.notes.trim().ifBlank { null }
                )
            }
        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Pridekite bent viena daikta")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = if (editor.templateId == null) {
                eventRepository.createInventoryTemplate(
                    CreateInventoryTemplateRequestDto(
                        name = name,
                        eventType = editor.eventType,
                        items = items
                    )
                )
            } else {
                eventRepository.updateInventoryTemplate(
                    editor.templateId,
                    UpdateInventoryTemplateRequestDto(
                        name = name,
                        eventType = editor.eventType,
                        items = items
                    )
                )
            }
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        editor = null,
                        message = "Sablonas issaugotas"
                    )
                    loadTemplates()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Nepavyko issaugoti sablono"
                    )
                }
        }
    }

    fun deleteTemplate() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            eventRepository.deleteInventoryTemplate(target.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        deleteTarget = null,
                        message = "Sablonas istrintas"
                    )
                    loadTemplates()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        deleteTarget = null,
                        error = error.message ?: "Nepavyko istrinti sablono"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun updateEditor(update: (InventoryTemplateEditorState) -> InventoryTemplateEditorState) {
        val editor = _uiState.value.editor ?: return
        _uiState.value = _uiState.value.copy(editor = update(editor))
    }
}
