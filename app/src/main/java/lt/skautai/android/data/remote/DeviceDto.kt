package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.POST

data class RegisterDeviceRequestDto(
    @SerializedName("deviceToken") val deviceToken: String,
    @SerializedName("deviceName") val deviceName: String?
)

interface DeviceApiService {
    @POST("api/devices/register")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: RegisterDeviceRequestDto
    ): Response<MessageResponseDto>

    @HTTP(method = "DELETE", path = "api/devices/register", hasBody = true)
    suspend fun unregisterDevice(
        @Header("Authorization") token: String,
        @Body request: RegisterDeviceRequestDto
    ): Response<MessageResponseDto>
}
