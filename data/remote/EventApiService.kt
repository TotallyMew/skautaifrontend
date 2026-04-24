package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface EventApiService {

    @GET("api/events")
    suspend fun getEvents(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): Response<EventListDto>

    @GET("api/events/{id}")
    suspend fun getEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventDto>

    @POST("api/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateEventRequestDto
    ): Response<EventDto>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateEventRequestDto
    ): Response<EventDto>

    @DELETE("api/events/{id}")
    suspend fun cancelEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/events/{id}/roles")
    suspend fun assignEventRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: AssignEventRoleRequestDto
    ): Response<EventRoleDto>

    @DELETE("api/events/{id}/roles/{roleId}")
    suspend fun removeEventRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("roleId") roleId: String
    ): Response<Unit>

    @GET("api/events/{id}/inventory-plan")
    suspend fun getInventoryPlan(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventInventoryPlanDto>

    @POST("api/events/{id}/inventory-buckets")
    suspend fun createInventoryBucket(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryBucketRequestDto
    ): Response<EventInventoryBucketDto>

    @POST("api/events/{id}/inventory-items")
    suspend fun createInventoryItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryItemRequestDto
    ): Response<EventInventoryItemDto>

    @POST("api/events/{id}/inventory-items/bulk")
    suspend fun createInventoryItemsBulk(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryItemsBulkRequestDto
    ): Response<EventInventoryItemListDto>

    @PUT("api/events/{id}/inventory-items/{inventoryItemId}")
    suspend fun updateInventoryItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("inventoryItemId") inventoryItemId: String,
        @Body request: UpdateEventInventoryItemRequestDto
    ): Response<EventInventoryItemDto>

    @POST("api/events/{id}/inventory-allocations")
    suspend fun createInventoryAllocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryAllocationRequestDto
    ): Response<EventInventoryAllocationDto>

    @GET("api/events/{id}/purchases")
    suspend fun getPurchases(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventPurchaseListDto>

    @POST("api/events/{id}/purchases")
    suspend fun createPurchase(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventPurchaseRequestDto
    ): Response<EventPurchaseDto>

    @POST("api/events/{id}/purchases/{purchaseId}/invoice")
    suspend fun attachPurchaseInvoice(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("purchaseId") purchaseId: String,
        @Body request: AttachEventPurchaseInvoiceRequestDto
    ): Response<EventPurchaseDto>

    @GET("api/events/{id}/purchases/{purchaseId}/invoice/download")
    suspend fun downloadPurchaseInvoice(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("purchaseId") purchaseId: String
    ): Response<okhttp3.ResponseBody>

    @POST("api/events/{id}/purchases/{purchaseId}/complete")
    suspend fun completePurchase(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("purchaseId") purchaseId: String
    ): Response<EventPurchaseDto>

    @POST("api/events/{id}/purchases/{purchaseId}/add-to-inventory")
    suspend fun addPurchaseToInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("purchaseId") purchaseId: String
    ): Response<EventPurchaseDto>
}
