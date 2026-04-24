package lt.skautai.android.util

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class NetworkErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: ConnectException) {
            throw IOException("Nepavyko prisijungti. Patikrinkite interneto ryšį.")
        } catch (e: UnknownHostException) {
            throw IOException("Nepavyko prisijungti. Patikrinkite interneto ryšį.")
        } catch (e: SocketTimeoutException) {
            throw IOException("Nepavyko prisijungti. Skrytis baigėsi.")
        }
    }
}
