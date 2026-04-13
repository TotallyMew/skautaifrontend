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

                LaunchedEffect(token) {
                    if (!isSuperAdminDeepLink && token != null) {
                        navController.navigate(NavRoutes.InventoryList.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    startDestination = if (isSuperAdminDeepLink)
                        NavRoutes.SuperAdminLogin.route
                    else
                        NavRoutes.Login.route
                )
            }
        }
    }
}