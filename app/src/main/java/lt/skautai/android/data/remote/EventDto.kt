package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class EventRoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("role") val role: String,
    @SerializedName("targetGroup") val targetGroup: String?,
    @SerializedName("assignedByUserId") val assignedByUserId: String?,
    @SerializedName("assignedAt") val assignedAt: String
)

data class StovyklaDetailsDto(
    @SerializedName("id") val id: String,
    @SerializedName("registrationDeadline") val registrationDeadline: String?,
    @SerializedName("expectedParticipants") val expectedParticipants: Int?,
    @SerializedName("actualParticipants") val actualParticipants: Int?
)

data class EventDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("locationId") val locationId: String?,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("createdByUserId") val createdByUserId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("eventRoles") val eventRoles: List<EventRoleDto>,
    @SerializedName("stovyklaDetails") val stovyklaDetails: StovyklaDetailsDto?
)

data class EventListDto(
    @SerializedName("events") val events: List<EventDto>,
    @SerializedName("total") val total: Int
)

data class CreateEventRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("registrationDeadline") val registrationDeadline: String? = null,
    @SerializedName("expectedParticipants") val expectedParticipants: Int? = null
)

data class UpdateEventRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("notes") val notes: String? = null
)
