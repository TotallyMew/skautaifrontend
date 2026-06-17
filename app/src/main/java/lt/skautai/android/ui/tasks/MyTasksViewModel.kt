package lt.skautai.android.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.LeadershipChangeRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MyTaskDto
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.MyTaskRepository

sealed interface MyTasksUiState {
    data object Loading : MyTasksUiState
    data class Success(
        val tasks: List<MyTaskDto>,
        val leadershipChangeRequests: List<LeadershipChangeRequestDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val isSaving: Boolean = false,
        val actionError: String? = null
    ) : MyTasksUiState
    data object Empty : MyTasksUiState
    data class Error(val message: String) : MyTasksUiState
}

@HiltViewModel
class MyTasksViewModel @Inject constructor(
    private val myTaskRepository: MyTaskRepository,
    private val memberRepository: MemberRepository
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
            val tasksResult = myTaskRepository.getMyTasks()
            val leadershipRequests = memberRepository.getLeadershipChangeRequests().getOrNull()?.requests.orEmpty()
            val members = if (leadershipRequests.isNotEmpty()) {
                memberRepository.getMembers().getOrNull()?.members.orEmpty()
            } else {
                emptyList()
            }
            tasksResult
                .onSuccess { response ->
                    _uiState.value = if (response.tasks.isEmpty() && leadershipRequests.isEmpty()) {
                        MyTasksUiState.Empty
                    } else {
                        MyTasksUiState.Success(response.tasks, leadershipRequests, members)
                    }
                }
                .onFailure { error ->
                    _uiState.value = MyTasksUiState.Error(
                        error.message ?: "Nepavyko gauti mano užduočių."
                    )
                }
        }
    }

    fun approveLeadershipChange(requestId: String, successorUserId: String) {
        reviewLeadershipChange(requestId, "APPROVE", successorUserId)
    }

    fun rejectLeadershipChange(requestId: String) {
        reviewLeadershipChange(requestId, "REJECT", null)
    }

    private fun reviewLeadershipChange(requestId: String, action: String, successorUserId: String?) {
        val current = _uiState.value as? MyTasksUiState.Success ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, actionError = null)
            memberRepository.reviewLeadershipChangeRequest(requestId, action, successorUserId)
                .onSuccess {
                    val updatedRequests = current.leadershipChangeRequests.filterNot { it.id == requestId }
                    _uiState.value = current.copy(
                        leadershipChangeRequests = updatedRequests,
                        isSaving = false,
                        actionError = null
                    )
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isSaving = false,
                        actionError = error.message ?: "Nepavyko peržiūrėti vadovo pasikeitimo prašymo."
                    )
                }
        }
    }
}
