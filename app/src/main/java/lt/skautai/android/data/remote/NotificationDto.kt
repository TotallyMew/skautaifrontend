package lt.skautai.android.data.remote

data class NotificationDto(
    val id: String,
    val tuntasId: String? = null,
    val title: String,
    val body: String,
    val resource: String? = null,
    val entityId: String? = null,
    val data: Map<String, String> = emptyMap(),
    val readAt: String? = null,
    val createdAt: String
)

data class NotificationListDto(
    val notifications: List<NotificationDto>,
    val total: Int,
    val unreadCount: Int
)

