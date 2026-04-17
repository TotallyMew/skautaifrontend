package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class ReservationDto(
    @SerializedName("id") val id: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("reservedByUserId") val reservedByUserId: String,
    @SerializedName("approvedByUserId") val approvedByUserId: String?,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class ReservationListDto(
    @SerializedName("reservations") val reservations: List<ReservationDto>,
    @SerializedName("total") val total: Int
)

data class CreateReservationRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String?
)

data class UpdateReservationStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?
)