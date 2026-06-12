package lt.skautai.android.data.live

data class LiveEvent(
    val id: String = "",
    val tuntasId: String = "",
    val actorUserId: String? = null,
    val resource: String = "",
    val action: String = "",
    val path: String = "",
    val occurredAt: String = ""
)
