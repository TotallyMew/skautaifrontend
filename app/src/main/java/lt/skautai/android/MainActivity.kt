package lt.skautai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.ui.common.AppNavGraph
import lt.skautai.android.ui.theme.SkautuInventoriusTheme
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent?.data
        val isSuperAdminDeepLink = uri?.scheme == "skautai" &&
                uri.host == "superadmin" &&
                uri.queryParameterNames.isEmpty()

        setContent {
            SkautuInventoriusTheme {
                val navController = rememberNavController()
                val startDestination by produceState<String?>(initialValue = null, isSuperAdminDeepLink) {
                    value = if (isSuperAdminDeepLink) {
                        NavRoutes.SuperAdminLogin.route
                    } else {
                        val currentToken = tokenManager.token.first()
                        val currentTuntasId = tokenManager.activeTuntasId.first()
                        val currentTuntasName = tokenManager.activeTuntasName.first()
                        var sessionIsValid = currentToken != null
                        val myTuntaiResult = if (currentToken != null) {
                            userRepository.getMyTuntai().onFailure { error ->
                                if (error.message == SESSION_EXPIRED_MESSAGE || error.message == "Vartotojas n?rastas.") {
                                    tokenManager.clearAll()
                                    sessionIsValid = false
                                }
                            }
                        } else {
                            null
                        }
                        if (currentToken != null &&
                            sessionIsValid &&
                            !currentTuntasId.isNullOrBlank() &&
                            currentTuntasName.isNullOrBlank()
                        ) {
                            myTuntaiResult
                                ?.getOrNull()
                                ?.firstOrNull { it.id == currentTuntasId && it.status == "ACTIVE" }
                                ?.let { tokenManager.setActiveTuntas(it.id, it.name) }
                        }
                        val activeTuntasStillAvailable = if (sessionIsValid && !currentTuntasId.isNullOrBlank()) {
                            myTuntaiResult
                                ?.getOrNull()
                                ?.any { it.id == currentTuntasId && it.status == "ACTIVE" }
                                ?: true
                        } else {
                            true
                        }
                        if (!activeTuntasStillAvailable) {
                            tokenManager.clearActiveTuntas()
                        }
                        when {
                            !sessionIsValid || currentToken == null -> NavRoutes.Login.route
                            currentTuntasId.isNullOrBlank() || !activeTuntasStillAvailable -> NavRoutes.TuntasSelect.route
                            else -> NavRoutes.Home.route
                        }
                    }
                }

                if (startDestination == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppNavGraph(
                        navController = navController,
                        tokenManager = tokenManager,
                        startDestination = startDestination!!
                    )
                }
            }
        }
    }
}
