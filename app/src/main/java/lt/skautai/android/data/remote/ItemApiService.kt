package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ItemApiService {

    @GET("api/items")
    suspend fun getItems(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("ownerType") ownerType: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null
    ): Response<ItemListResponseDto>


    @GET("api/items/{itemId}")
    suspend fun getItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<ItemDto>

    @DELETE("api/items/{itemId}")
    suspend fun deleteItem(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>
}