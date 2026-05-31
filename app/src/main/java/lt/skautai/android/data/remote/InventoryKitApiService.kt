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

interface InventoryKitApiService {
    @GET("api/inventory-kits")
    suspend fun getKits(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<InventoryKitListDto>

    @GET("api/inventory-kits/{kitId}")
    suspend fun getKit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("kitId") kitId: String
    ): Response<InventoryKitDto>

    @POST("api/inventory-kits")
    suspend fun createKit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateInventoryKitRequestDto
    ): Response<InventoryKitDto>

    @PUT("api/inventory-kits/{kitId}")
    suspend fun updateKit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("kitId") kitId: String,
        @Body request: UpdateInventoryKitRequestDto
    ): Response<InventoryKitDto>

    @DELETE("api/inventory-kits/{kitId}")
    suspend fun deleteKit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("kitId") kitId: String
    ): Response<Unit>
}
