package lt.skautai.android.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.MyTaskDto
import lt.skautai.android.data.repository.MyTaskRepository

sealed interface MyTasksUiState {
    data object Loading : MyTasksUiState
    data class Success(val tasks: List<MyTaskDto>) : MyTasksUiState
    data object Empty : MyTasksUiState
    data class Error(val message: String) : MyTasksUiState
}

@HiltViewModel
class MyTasksViewModel @Inject constructor(
    private val myTaskRepository: MyTaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<MyTasksUiState>(MyTasksUiState.Loading)
    val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value !is MyTasksUiState.Success) {
                _uiState.value = MyTasksUiState.Loading
            }
            myTaskRepository.getMyTasks()
                .onSuccess { response ->
                    _uiState.value = if (response.tasks.isEmpty()) {
                        MyTasksUiState.Empty
                    } else {
                        MyTasksUiState.Success(response.tasks)
                    }
                }
                .onFailure { error ->
                    _uiState.value = MyTasksUiState.Error(
                        error.message ?: "Nepavyko gauti mano užduočių."
                    )
                }
        }
    }
}
