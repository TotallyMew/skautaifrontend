package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.CreateInventoryKitRequestDto
import lt.skautai.android.data.remote.InventoryKitApiService
import lt.skautai.android.data.remote.InventoryKitDto
import lt.skautai.android.data.remote.InventoryKitListDto
import lt.skautai.android.data.remote.UpdateInventoryKitRequestDto
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TUNTAS_SELECTION_REQUIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import lt.skautai.android.util.userFacingException

@Singleton
class InventoryKitRepository @Inject constructor(
    private val api: InventoryKitApiService,
    private val tokenManager: TokenManager
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception(SESSION_EXPIRED_MESSAGE)

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception(TUNTAS_SELECTION_REQUIRED_MESSAGE)

    suspend fun getKits(includeInactive: Boolean = false): Result<InventoryKitListDto> = try {
        val response = api.getKits("Bearer ${token()}", tuntasId(), includeInactive)
        if (response.isSuccessful) Result.success(response.body() ?: InventoryKitListDto(emptyList(), 0))
        else Result.failure(Exception(response.errorMessage("Nepavyko gauti komplektų.")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getKit(id: String): Result<InventoryKitDto> = try {
        val response = api.getKit("Bearer ${token()}", tuntasId(), id)
        if (response.isSuccessful) Result.success(response.body()!!)
        else Result.failure(Exception(response.errorMessage("Nepavyko gauti komplekto.")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun createKit(request: CreateInventoryKitRequestDto): Result<InventoryKitDto> = try {
        val response = api.createKit("Bearer ${token()}", tuntasId(), request)
        if (response.isSuccessful) Result.success(response.body()!!)
        else Result.failure(Exception(response.errorMessage("Nepavyko sukurti komplekto.")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun updateKit(id: String, request: UpdateInventoryKitRequestDto): Result<InventoryKitDto> = try {
        val response = api.updateKit("Bearer ${token()}", tuntasId(), id, request)
        if (response.isSuccessful) Result.success(response.body()!!)
        else Result.failure(Exception(response.errorMessage("Nepavyko atnaujinti komplekto.")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun deleteKit(id: String): Result<Unit> = try {
        val response = api.deleteKit("Bearer ${token()}", tuntasId(), id)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.errorMessage("Nepavyko pašalinti komplekto.")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }
}
