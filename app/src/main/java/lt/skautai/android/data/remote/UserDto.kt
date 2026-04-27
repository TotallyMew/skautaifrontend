package lt.skautai.android.data.remote

data class UserTuntasDto(
    val id: String,
    val name: String,
    val krastas: String,
    val contactEmail: String,
    val status: String = "ACTIVE"
)

data class MyProfileDto(
    val userId: String,
    val name: String,
    val surname: String,
    val email: String,
    val phone: String?,
    val createdAt: String,
    val updatedAt: String
)

data class UpdateMyProfileRequestDto(
    val name: String,
    val surname: String,
    val email: String,
    val phone: String?
)

data class ChangeMyPasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)
