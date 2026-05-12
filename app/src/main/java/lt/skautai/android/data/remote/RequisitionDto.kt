package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class RequisitionItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("requestType") val requestType: String = "NEW_ITEM",
    @SerializedName("existingItemId") val existingItemId: String? = null,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemDescription") val itemDescription: String?,
    @SerializedName("quantityRequested") val quantityRequested: Int,
    @SerializedName("quantityApproved") val quantityApproved: Int?,
    @SerializedName("rejectionReason") val rejectionReason: String?,
    @SerializedName("notes") val notes: String?
)

data class RequisitionDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("createdByUserId") val createdByUserId: String,
    @SerializedName("requestingUnitId") val requestingUnitId: String?,
    @SerializedName("requestingUnitName") val requestingUnitName: String?,
    @SerializedName("status") val status: String,
    @SerializedName("unitReviewStatus") val unitReviewStatus: String,
    @SerializedName("unitReviewedByUserId") val unitReviewedByUserId: String?,
    @SerializedName("unitReviewedAt") val unitReviewedAt: String?,
    @SerializedName("topLevelReviewStatus") val topLevelReviewStatus: String,
    @SerializedName("topLevelReviewedByUserId") val topLevelReviewedByUserId: String?,
    @SerializedName("topLevelReviewedAt") val topLevelReviewedAt: String?,
    @SerializedName("purchasedAt") val purchasedAt: String? = null,
    @SerializedName("addedToInventoryAt") val addedToInventoryAt: String? = null,
    @SerializedName("reviewLevel") val reviewLevel: String,
    @SerializedName("lastAction") val lastAction: String,
    @SerializedName("neededByDate") val neededByDate: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<RequisitionItemDto>,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class RequisitionListDto(
    @SerializedName("requests") val requests: List<RequisitionDto>,
    @SerializedName("total") val total: Int
)

data class CreateRequisitionItemDto(
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemDescription") val itemDescription: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("requestType") val requestType: String = "NEW_ITEM",
    @SerializedName("existingItemId") val existingItemId: String? = null
)

data class CreateRequisitionDto(
    @SerializedName("requestingUnitId") val requestingUnitId: String? = null,
    @SerializedName("neededByDate") val neededByDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<CreateRequisitionItemDto>
)

data class RequisitionReviewDto(
    @SerializedName("action") val action: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null
)

data class RequisitionMarkPurchasedDto(
    @SerializedName("notes") val notes: String? = null
)

data class AddRequisitionItemToInventoryDto(
    @SerializedName("requisitionItemId") val requisitionItemId: String,
    @SerializedName("action") val action: String,
    @SerializedName("existingItemId") val existingItemId: String? = null,
    @SerializedName("custodianId") val custodianId: String? = null,
    @SerializedName("type") val type: String = "COLLECTIVE",
    @SerializedName("category") val category: String = "TOOLS",
    @SerializedName("condition") val condition: String = "GOOD",
    @SerializedName("purchaseDate") val purchaseDate: String? = null,
    @SerializedName("purchasePrice") val purchasePrice: Double? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AddRequisitionToInventoryDto(
    @SerializedName("items") val items: List<AddRequisitionItemToInventoryDto>
)
