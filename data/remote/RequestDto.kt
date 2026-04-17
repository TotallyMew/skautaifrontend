package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class BendrasRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("requestedByUserId") val requestedByUserId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("draugoveId") val draugoveId: String?,
    @SerializedName("draugoveName") val draugoveName: String?,
    @SerializedName("needsDraugininkasApproval") val needsDraugininkasApproval: Boolean,
    @SerializedName("draugininkasStatus") val draugininkasStatus: String?,
    @SerializedName("draugininkasReviewedByUserId") val draugininkasReviewedByUserId: String?,
    @SerializedName("draugininkasRejectionReason") val draugininkasRejectionReason: String?,
    @SerializedName("topLevelStatus") val topLevelStatus: String,
    @SerializedName("topLevelReviewedByUserId") val topLevelReviewedByUserId: String?,
    @SerializedName("topLevelRejectionReason") val topLevelRejectionReason: String?,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class BendrasRequestListDto(
    @SerializedName("requests") val requests: List<BendrasRequestDto>,
    @SerializedName("total") val total: Int
)

data class CreateBendrasRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("draugoveId") val draugoveId: String?,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("notes") val notes: String?
)

data class ReviewRequestDto(
    @SerializedName("action") val action: String,
    @SerializedName("rejectionReason") val rejectionReason: String?
)