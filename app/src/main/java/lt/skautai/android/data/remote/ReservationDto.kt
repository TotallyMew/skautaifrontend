package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class ReservationDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("reservedByUserId") val reservedByUserId: String,
    @SerializedName("approvedByUserId") val approvedByUserId: String?,
    @SerializedName("requestingUnitId") val requestingUnitId: String?,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("totalItems") val totalItems: Int,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("items") val items: List<ReservationItemDto>
)

data class ReservationItemDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int
)

data class ReservationListDto(
    @SerializedName("reservations") val reservations: List<ReservationDto>,
    @SerializedName("total") val total: Int
)

data class ReservationAvailabilityItemDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("reservedQuantity") val reservedQuantity: Int,
    @SerializedName("availableQuantity") val availableQuantity: Int
)

data class ReservationAvailabilityDto(
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("items") val items: List<ReservationAvailabilityItemDto>
)

data class CreateReservationItemRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)

data class CreateReservationRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("items") val items: List<CreateReservationItemRequestDto>,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("requestingUnitId") val requestingUnitId: String? = null,
    @SerializedName("notes") val notes: String?
)

data class UpdateReservationStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?
)
