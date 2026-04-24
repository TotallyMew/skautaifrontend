package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class BendrasRequestItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int
)

data class BendrasRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("requestedByUserId") val requestedByUserId: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemDescription") val itemDescription: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("neededByDate") val neededByDate: String?,
    @SerializedName("requestingUnitId") val requestingUnitId: String?,
    @SerializedName("requestingUnitName") val requestingUnitName: String?,
    @SerializedName("needsDraugininkasApproval") val needsDraugininkasApproval: Boolean,
    @SerializedName("draugininkasStatus") val draugininkasStatus: String?,
    @SerializedName("draugininkasReviewedByUserId") val draugininkasReviewedByUserId: String?,
    @SerializedName("draugininkasRejectionReason") val draugininkasRejectionReason: String?,
    @SerializedName("topLevelStatus") val topLevelStatus: String,
    @SerializedName("topLevelReviewedByUserId") val topLevelReviewedByUserId: String?,
    @SerializedName("topLevelRejectionReason") val topLevelRejectionReason: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<BendrasRequestItemDto>,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class BendrasRequestListDto(
    @SerializedName("requests") val requests: List<BendrasRequestDto>,
    @SerializedName("total") val total: Int
)

data class CreateBendrasRequestItemDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)

data class CreateBendrasRequestDto(
    @SerializedName("itemDescription") val itemDescription: String? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("requestingUnitId") val requestingUnitId: String? = null,
    @SerializedName("neededByDate") val neededByDate: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<CreateBendrasRequestItemDto> = emptyList()
)

data class ReviewRequestDto(
    @SerializedName("action") val action: String,
    @SerializedName("rejectionReason") val rejectionReason: String?
)
