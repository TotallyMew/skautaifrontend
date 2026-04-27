package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.ui.common.isScoutReadOnlyMember
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface MemberListUiState {
    data object Loading : MemberListUiState
    data class Success(
        val members: List<MemberDto>,
        val isReadOnly: Boolean = false
    ) : MemberListUiState
    data class Error(val message: String) : MemberListUiState
}

@HiltViewModel
class MemberListViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberListUiState>(MemberListUiState.Loading)
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()
    private var isReadOnlyForCurrentUser: Boolean = false

    init {
        loadCurrentUserAccessMode()
        observeCachedMembers()
        loadMembers()
    }

    private fun loadCurrentUserAccessMode() {
        viewModelScope.launch {
            val currentUserId = tokenManager.userId.first()
            isReadOnlyForCurrentUser = currentUserId
                ?.let { memberRepository.getMember(it).getOrNull() }
                ?.let(::isScoutReadOnlyMember)
                ?: false
        }
    }

    private fun observeCachedMembers() {
        viewModelScope.launch {
            memberRepository.observeMembers().collect { memberList ->
                if (memberList.members.isNotEmpty()) {
                    _uiState.value = MemberListUiState.Success(
                        members = memberList.members,
                        isReadOnly = isReadOnlyForCurrentUser
                    )
                } else if (_uiState.value !is MemberListUiState.Loading) {
                    _uiState.value = MemberListUiState.Success(
                        members = emptyList(),
                        isReadOnly = isReadOnlyForCurrentUser
                    )
                }
            }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            memberRepository.refreshMembers()
                .onFailure { error ->
                    if (_uiState.value is MemberListUiState.Loading) {
                        _uiState.value = MemberListUiState.Error(
                            error.message ?: "Klaida gaunant narius"
                        )
                    }
                }
        }
    }
}
