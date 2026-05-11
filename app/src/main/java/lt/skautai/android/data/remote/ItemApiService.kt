package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ItemApiService {

    @GET("api/items")
    suspend fun getItems(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("custodianId") custodianId: String? = null,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null,
        @Query("sharedOnly") sharedOnly: Boolean = false
    ): Response<ItemListResponseDto>

    @GET("api/items/{itemId}")
    suspend fun getItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemDto>

    @GET("api/items/{itemId}/assignments")
    suspend fun getItemAssignments(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemAssignmentListResponseDto>

    @GET("api/items/{itemId}/condition-log")
    suspend fun getItemConditionLog(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemConditionLogListResponseDto>

    @GET("api/items/{itemId}/transfers")
    suspend fun getItemTransfers(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemTransferListResponseDto>

    @GET("api/items/{itemId}/history")
    suspend fun getItemHistory(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemHistoryListResponseDto>

    @GET("api/items/resolve-qr/{tokenValue}")
    suspend fun resolveQrToken(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("tokenValue") tokenValue: String
    ): Response<ItemQrResolveResponseDto>

    @DELETE("api/items/{itemId}")
    suspend fun deleteItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>

    @POST("api/items")
    suspend fun createItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateItemRequestDto
    ): Response<ItemDto>

    @PUT("api/items/{itemId}")
    suspend fun updateItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateItemRequestDto
    ): Response<ItemDto>

    @POST("api/items/{itemId}/transfer-to-unit")
    suspend fun transferItemToUnit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String,
        @Body request: TransferItemToUnitRequestDto
    ): Response<ItemDto>

    @POST("api/items/{itemId}/return-to-shared")
    suspend fun returnItemToShared(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String,
        @Body request: ReturnItemToSharedRequestDto
    ): Response<ItemDto>

    @POST("api/items/{itemId}/restock")
    suspend fun restockItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String,
        @Body request: RestockItemRequestDto
    ): Response<ItemDto>
}
