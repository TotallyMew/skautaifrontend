package lt.skautai.android.data.remote

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterTuntininkasRequestDto(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val tuntasName: String,
    val tuntasKrastas: String? = null
)

data class RegisterWithInviteRequestDto(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val inviteCode: String
)

data class TokenResponseDto(
    val token: String,
    val refreshToken: String? = null,
    val userId: String,
    val email: String,
    val name: String,
    val type: String = "user",
    val tuntai: List<UserTuntasDto>? = emptyList()
)

data class RefreshTokenRequestDto(
    val refreshToken: String
)

data class ErrorResponseDto(
    val error: String
)

data class TuntasDto(
    val id: String,
    val name: String,
    val krastas: String,
    val status: String,
    val contactEmail: String
)

data class MessageResponseDto(
    val message: String
)
