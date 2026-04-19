package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface OrganizationalUnitApiService {

    @GET("api/organizational-units")
    suspend fun getUnits(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("type") type: String? = null
    ): Response<OrganizationalUnitListResponseDto>

    @GET("api/organizational-units/{id}")
    suspend fun getUnit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String
    ): Response<OrganizationalUnitDto>

    @POST("api/organizational-units")
    suspend fun createUnit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateOrganizationalUnitRequestDto
    ): Response<OrganizationalUnitDto>

    @PUT("api/organizational-units/{id}")
    suspend fun updateUnit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String,
        @Body request: UpdateOrganizationalUnitRequestDto
    ): Response<OrganizationalUnitDto>

    @DELETE("api/organizational-units/{id}")
    suspend fun deleteUnit(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String
    ): Response<Void>

    @GET("api/organizational-units/{id}/members")
    suspend fun getUnitMembers(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String
    ): Response<UnitMembershipListResponseDto>

    @POST("api/organizational-units/{id}/members")
    suspend fun assignUnitMember(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String,
        @Body request: AssignUnitMemberRequestDto
    ): Response<UnitMembershipDto>

    @DELETE("api/organizational-units/{id}/members/{userId}")
    suspend fun removeUnitMember(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") unitId: String,
        @Path("userId") userId: String
    ): Response<Void>
}
