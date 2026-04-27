package lt.skautai.android.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ApiErrorTest {

    @Test
    fun `translates known backend error to Lithuanian`() {
        val message = parseApiError("""{"error":"Insufficient permissions"}""", 403, "Nepavyko atlikti veiksmo.")

        assertEquals("Neturite teisių atlikti šį veiksmą.", message)
    }

    @Test
    fun `uses fallback for unknown backend error`() {
        val message = parseApiError("""{"error":"Completely unknown"}""", 400, "Nepavyko atnaujinti profilio.")

        assertEquals("Completely unknown", message)
    }

    @Test
    fun `uses http fallback when body is empty`() {
        val message = parseApiError(null, 401, "Nepavyko gauti profilio.")

        assertEquals(SESSION_EXPIRED_MESSAGE, message)
    }

    @Test
    fun `prefers error over message and details`() {
        val response = Response.error<Unit>(
            400,
            """{"error":"Invalid invite code","message":"ignored","details":"ignored"}"""
                .toResponseBody("application/json".toMediaType())
        )

        assertEquals("Neteisingas pakvietimo kodas.", response.errorMessage("Nepavyko priimti pakvietimo."))
    }
}
