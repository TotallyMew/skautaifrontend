package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class ReservationDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("reservedByUserId") val reservedByUserId: String,
    @SerializedName("reservedByName") val reservedByName: String?,
    @SerializedName("approvedByUserId") val approvedByUserId: String?,
    @SerializedName("requestingUnitId") val requestingUnitId: String?,
    @SerializedName("requestingUnitName") val requestingUnitName: String?,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("totalItems") val totalItems: Int,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("unitReviewStatus") val unitReviewStatus: String? = null,
    @SerializedName("unitReviewedByUserId") val unitReviewedByUserId: String? = null,
    @SerializedName("unitReviewedAt") val unitReviewedAt: String? = null,
    @SerializedName("topLevelReviewStatus") val topLevelReviewStatus: String? = null,
    @SerializedName("topLevelReviewedByUserId") val topLevelReviewedByUserId: String? = null,
    @SerializedName("topLevelReviewedAt") val topLevelReviewedAt: String? = null,
    @SerializedName("pickupAt") val pickupAt: String? = null,
    @SerializedName("pickupProposalStatus") val pickupProposalStatus: String = "NONE",
    @SerializedName("pickupProposedAt") val pickupProposedAt: String? = null,
    @SerializedName("pickupProposedByUserId") val pickupProposedByUserId: String? = null,
    @SerializedName("pickupRespondedAt") val pickupRespondedAt: String? = null,
    @SerializedName("pickupRespondedByUserId") val pickupRespondedByUserId: String? = null,
    @SerializedName("returnAt") val returnAt: String? = null,
    @SerializedName("returnProposalStatus") val returnProposalStatus: String = "NONE",
    @SerializedName("returnProposedAt") val returnProposedAt: String? = null,
    @SerializedName("returnProposedByUserId") val returnProposedByUserId: String? = null,
    @SerializedName("returnRespondedAt") val returnRespondedAt: String? = null,
    @SerializedName("returnRespondedByUserId") val returnRespondedByUserId: String? = null,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("items") val items: List<ReservationItemDto>
)

data class ReservationItemDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("custodianId") val custodianId: String? = null,
    @SerializedName("custodianName") val custodianName: String? = null,
    @SerializedName("remainingAfterReservation") val remainingAfterReservation: Int?,
    @SerializedName("issuedQuantity") val issuedQuantity: Int = 0,
    @SerializedName("returnedQuantity") val returnedQuantity: Int = 0,
    @SerializedName("markedReturnedQuantity") val markedReturnedQuantity: Int = 0,
    @SerializedName("remainingToIssue") val remainingToIssue: Int = quantity,
    @SerializedName("remainingToReturn") val remainingToReturn: Int = 0,
    @SerializedName("remainingToMarkReturned") val remainingToMarkReturned: Int = 0,
    @SerializedName("remainingToReceive") val remainingToReceive: Int = 0
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

data class UpdateReservationPickupRequestDto(
    @SerializedName("pickupAt") val pickupAt: String?,
    @SerializedName("response") val response: String? = null
)

data class UpdateReservationReturnTimeRequestDto(
    @SerializedName("returnAt") val returnAt: String?,
    @SerializedName("response") val response: String? = null
)

data class ReviewReservationRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String? = null
)

data class ReservationMovementItemRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)

data class ReservationMovementRequestDto(
    @SerializedName("items") val items: List<ReservationMovementItemRequestDto>,
    @SerializedName("notes") val notes: String? = null
)

data class ReservationMovementDto(
    @SerializedName("id") val id: String,
    @SerializedName("reservationId") val reservationId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String?,
    @SerializedName("type") val type: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("performedByUserId") val performedByUserId: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class ReservationMovementListDto(
    @SerializedName("movements") val movements: List<ReservationMovementDto>,
    @SerializedName("total") val total: Int
)
