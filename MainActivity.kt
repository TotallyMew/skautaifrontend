package lt.skautai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import lt.skautai.android.ui.common.AppNavGraph
import lt.skautai.android.ui.theme.SkautuInventoriusTheme
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isSuperAdminDeepLink = intent?.data?.scheme == "skautai" &&
                intent?.data?.host == "superadmin"

        setContent {
            SkautuInventoriusTheme {
                val navController = rememberNavController()
                val token by tokenManager.token.collectAsState(initial = null)
                val activeTuntasId by tokenManager.activeTuntasId.collectAsState(initial = null)

                LaunchedEffect(Unit) {
                    val currentToken = tokenManager.token.first()
                    val currentTuntasId = tokenManager.activeTuntasId.first()
                    if (!isSuperAdminDeepLink && currentToken != null) {
                        if (currentTuntasId != null) {
                            navController.navigate(NavRoutes.InventoryList.route) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavRoutes.TuntasSelect.route) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                            }
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    tokenManager = tokenManager,
                    startDestination = if (isSuperAdminDeepLink)
                        NavRoutes.SuperAdminLogin.route
                    else
                        NavRoutes.Login.route
                )
            }
        }
    }
}