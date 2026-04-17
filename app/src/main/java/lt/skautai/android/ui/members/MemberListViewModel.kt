package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.repository.MemberRepository
import javax.inject.Inject

sealed interface MemberListUiState {
    data object Loading : MemberListUiState
    data class Success(val members: List<MemberDto>) : MemberListUiState
    data class Error(val message: String) : MemberListUiState
}

@HiltViewModel
class MemberListViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberListUiState>(MemberListUiState.Loading)
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.value = MemberListUiState.Loading
            memberRepository.getMembers()
                .onSuccess { response ->
                    _uiState.value = MemberListUiState.Success(response.members)
                }
                .onFailure { error ->
                    _uiState.value = MemberListUiState.Error(
                        error.message ?: "Klaida gaunant narius"
                    )
                }
        }
    }
}