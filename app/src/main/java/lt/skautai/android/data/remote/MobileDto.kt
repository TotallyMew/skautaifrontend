package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

data class MobileHomeSummaryDto(
    @SerializedName("activeUnitId") val activeUnitId: String? = null,
    @SerializedName("activeUnitName") val activeUnitName: String? = null,
    @SerializedName("availableUnits") val availableUnits: List<OrganizationalUnitDto> = emptyList(),
    @SerializedName("activeUnitItemCount") val activeUnitItemCount: Int = 0,
    @SerializedName("activeUnitFromSharedCount") val activeUnitFromSharedCount: Int = 0,
    @SerializedName("sharedInventoryCount") val sharedInventoryCount: Int = 0,
    @SerializedName("sharedPendingApprovalCount") val sharedPendingApprovalCount: Int = 0,
    @SerializedName("personalLendingCount") val personalLendingCount: Int = 0,
    @SerializedName("requisitionCount") val requisitionCount: Int = 0,
    @SerializedName("myRequisitionCount") val myRequisitionCount: Int = 0,
    @SerializedName("assignedRequisitionCount") val assignedRequisitionCount: Int = 0,
    @SerializedName("sharedRequestCount") val sharedRequestCount: Int = 0,
    @SerializedName("myReservationCount") val myReservationCount: Int = 0,
    @SerializedName("assignedReservationCount") val assignedReservationCount: Int = 0,
    @SerializedName("trackedReservationCount") val trackedReservationCount: Int = 0,
    @SerializedName("activeReservations") val activeReservations: List<ReservationDto> = emptyList(),
    @SerializedName("tasks") val tasks: List<MyTaskDto> = emptyList(),
    @SerializedName("taskTotalCount") val taskTotalCount: Int = 0
)

data class MobileCacheStateResourceDto(
    @SerializedName("resource") val resource: String,
    @SerializedName("maxUpdatedAt") val maxUpdatedAt: String?,
    @SerializedName("total") val total: Int,
    @SerializedName("versionKey") val versionKey: String
)

data class MobileCacheStateDto(
    @SerializedName("resources") val resources: List<MobileCacheStateResourceDto>
)

interface MobileApiService {
    @GET("api/mobile/home-summary")
    suspend fun getHomeSummary(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Header("X-Org-Unit-Id") activeOrgUnitId: String?
    ): Response<MobileHomeSummaryDto>

    @GET("api/mobile/cache-state")
    suspend fun getCacheState(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<MobileCacheStateDto>
}
