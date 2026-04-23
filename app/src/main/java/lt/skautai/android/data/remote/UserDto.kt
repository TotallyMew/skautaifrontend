package lt.skautai.android.data.remote

data class UserTuntasDto(
    val id: String,
    val name: String,
    val krastas: String,
    val contactEmail: String,
    val status: String = "ACTIVE"
)
