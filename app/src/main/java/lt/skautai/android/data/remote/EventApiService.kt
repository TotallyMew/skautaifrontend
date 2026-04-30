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

    @GET("api/events/{id}/candidate-members")
    suspend fun getCandidateMembers(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<MemberListDto>

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

    @PUT("api/events/{id}/inventory-buckets/{bucketId}")
    suspend fun updateInventoryBucket(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("bucketId") bucketId: String,
        @Body request: UpdateEventInventoryBucketRequestDto
    ): Response<EventInventoryBucketDto>

    @DELETE("api/events/{id}/inventory-buckets/{bucketId}")
    suspend fun deleteInventoryBucket(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("bucketId") bucketId: String
    ): Response<Unit>

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

    @DELETE("api/events/{id}/inventory-items/{inventoryItemId}")
    suspend fun deleteInventoryItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("inventoryItemId") inventoryItemId: String
    ): Response<Unit>

    @POST("api/events/{id}/inventory-allocations")
    suspend fun createInventoryAllocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryAllocationRequestDto
    ): Response<EventInventoryAllocationDto>

    @PUT("api/events/{id}/inventory-allocations/{allocationId}")
    suspend fun updateInventoryAllocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("allocationId") allocationId: String,
        @Body request: UpdateEventInventoryAllocationRequestDto
    ): Response<EventInventoryAllocationDto>

    @DELETE("api/events/{id}/inventory-allocations/{allocationId}")
    suspend fun deleteInventoryAllocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("allocationId") allocationId: String
    ): Response<Unit>

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

    @PUT("api/events/{id}/purchases/{purchaseId}")
    suspend fun updatePurchase(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("purchaseId") purchaseId: String,
        @Body request: UpdateEventPurchaseRequestDto
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

    @GET("api/events/{id}/reconciliation")
    suspend fun getReconciliation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventReconciliationDto>

    @POST("api/events/{id}/reconciliation/returns")
    suspend fun reconcileReturns(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReconcileEventReturnsRequestDto
    ): Response<EventReconciliationDto>

    @POST("api/events/{id}/reconciliation/purchases")
    suspend fun reconcilePurchases(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReconcileEventPurchasesRequestDto
    ): Response<EventReconciliationDto>

    @POST("api/events/{id}/complete")
    suspend fun completeEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventDto>

    @GET("api/events/{id}/pastovykles")
    suspend fun getPastovyklės(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<PastovykleListDto>

    @POST("api/events/{id}/pastovykles")
    suspend fun createPastovykle(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreatePastovykleRequestDto
    ): Response<PastovykleDto>

    @GET("api/events/{id}/pastovykles/{pid}")
    suspend fun getPastovykle(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String
    ): Response<PastovykleDto>

    @PUT("api/events/{id}/pastovykles/{pid}")
    suspend fun updatePastovykle(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Body request: UpdatePastovykleRequestDto
    ): Response<PastovykleDto>

    @DELETE("api/events/{id}/pastovykles/{pid}")
    suspend fun deletePastovykle(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String
    ): Response<Unit>

    @GET("api/events/{id}/pastovykles/{pid}/inventory")
    suspend fun getPastovykleInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String
    ): Response<PastovykleInventoryListDto>

    @POST("api/events/{id}/pastovykles/{pid}/inventory")
    suspend fun assignPastovykleInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Body request: AssignPastovykleInventoryRequestDto
    ): Response<PastovykleInventoryDto>

    @PUT("api/events/{id}/pastovykles/{pid}/inventory/{invId}")
    suspend fun updatePastovykleInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("invId") inventoryId: String,
        @Body request: UpdatePastovykleInventoryRequestDto
    ): Response<PastovykleInventoryDto>

    @DELETE("api/events/{id}/pastovykles/{pid}/inventory/{invId}")
    suspend fun deletePastovykleInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("invId") inventoryId: String
    ): Response<Unit>

    @GET("api/events/{id}/pastovykles/{pid}/requests")
    suspend fun getPastovykleRequests(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String
    ): Response<EventInventoryRequestListDto>

    @POST("api/events/{id}/pastovykles/{pid}/requests")
    suspend fun createPastovykleRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Body request: CreatePastovykleInventoryRequestRequestDto
    ): Response<EventInventoryRequestDto>

    @POST("api/events/{id}/pastovykles/{pid}/requests/{requestId}/approve")
    suspend fun approvePastovykleRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("requestId") requestId: String
    ): Response<EventInventoryRequestDto>

    @POST("api/events/{id}/pastovykles/{pid}/requests/{requestId}/reject")
    suspend fun rejectPastovykleRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("requestId") requestId: String
    ): Response<EventInventoryRequestDto>

    @POST("api/events/{id}/pastovykles/{pid}/requests/{requestId}/self-provided")
    suspend fun selfProvidePastovykleRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("requestId") requestId: String,
        @Body request: MarkPastovykleInventoryRequestSelfProvidedRequestDto
    ): Response<EventInventoryRequestDto>

    @POST("api/events/{id}/pastovykles/{pid}/requests/{requestId}/fulfill")
    suspend fun fulfillPastovykleRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Path("requestId") requestId: String,
        @Body request: FulfillPastovykleInventoryRequestRequestDto
    ): Response<EventInventoryRequestDto>

    @POST("api/events/{id}/pastovykles/{pid}/assign-from-unit")
    suspend fun assignFromUnitInventory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Path("pid") pastovykleId: String,
        @Body request: AssignUnitInventoryToPastovykleRequestDto
    ): Response<PastovykleInventoryDto>

    @GET("api/events/{id}/inventory-custody")
    suspend fun getInventoryCustody(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventInventoryCustodyListDto>

    @GET("api/events/{id}/inventory-movements")
    suspend fun getInventoryMovements(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventInventoryMovementListDto>

    @POST("api/events/{id}/inventory-movements")
    suspend fun createInventoryMovement(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: CreateEventInventoryMovementRequestDto
    ): Response<EventInventoryMovementDto>
}
