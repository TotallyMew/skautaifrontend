package lt.skautai.android.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class UserTuntasDtoTest {
    @Test
    fun `parses tuntas status`() {
        val dto = Gson().fromJson(
            """
                {
                    "id": "tuntas-id",
                    "name": "Test Tuntas",
                    "krastas": "Vilniaus",
                    "contactEmail": "test@example.com",
                    "status": "PENDING"
                }
            """.trimIndent(),
            UserTuntasDto::class.java
        )

        assertEquals("PENDING", dto.status)
    }
}
