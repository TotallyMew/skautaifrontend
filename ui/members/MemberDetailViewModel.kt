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

sealed interface MemberDetailUiState {
    data object Loading : MemberDetailUiState
    data class Success(val member: MemberDto) : MemberDetailUiState
    data class Error(val message: String) : MemberDetailUiState
}

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberDetailUiState>(MemberDetailUiState.Loading)
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    fun loadMember(userId: String) {
        viewModelScope.launch {
            _uiState.value = MemberDetailUiState.Loading
            memberRepository.getMember(userId)
                .onSuccess { member ->
                    _uiState.value = MemberDetailUiState.Success(member)
                }
                .onFailure { error ->
                    _uiState.value = MemberDetailUiState.Error(
                        error.message ?: "Klaida gaunant nario informaciją"
                    )
                }
        }
    }
}