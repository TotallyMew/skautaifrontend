package lt.skautai.android

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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
    private var passwordResetToken by mutableStateOf<String?>(null)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        passwordResetToken = passwordResetToken(intent)

        val notificationRoute = notificationRoute(intent)

        setContent {
            SkautuInventoriusTheme {
                val navController = rememberNavController()
                val startDestination by produceState<String?>(initialValue = null) {
                    value = run {
                        val currentToken = tokenManager.token.first()
                        val currentUserType = tokenManager.userType.first()
                        var currentTuntasId = tokenManager.activeTuntasId.first()
                        val currentTuntasName = tokenManager.activeTuntasName.first()
                        var sessionIsValid = currentToken != null
                        if (currentToken != null && currentUserType == "super_admin") {
                            return@run NavRoutes.SuperAdminDashboard.route
                        }
                        val myTuntaiResult = if (currentToken != null) {
                            userRepository.getMyTuntai().onFailure { error ->
                                if (error.message == SESSION_EXPIRED_MESSAGE || error.message == "Vartotojas nerastas.") {
                                    tokenManager.clearAll()
                                    sessionIsValid = false
                                }
                            }
                        } else {
                            null
                        }
                        val cachedTuntai = if (sessionIsValid && myTuntaiResult?.isFailure == true) {
                            tokenManager.cachedMyTuntai()
                        } else {
                            emptyList()
                        }
                        val knownTuntai = myTuntaiResult?.getOrNull() ?: cachedTuntai
                        val hasReliableTuntasList = myTuntaiResult?.isSuccess == true || cachedTuntai.isNotEmpty()
                        val activeTuntai = knownTuntai.filter { it.status == "ACTIVE" }

                        if (currentToken != null &&
                            sessionIsValid &&
                            !currentTuntasId.isNullOrBlank() &&
                            currentTuntasName.isNullOrBlank()
                        ) {
                            activeTuntai
                                .firstOrNull { it.id == currentTuntasId }
                                ?.let { tokenManager.setActiveTuntas(it.id, it.name) }
                        }
                        val activeTuntasStillAvailable = if (
                            sessionIsValid &&
                            hasReliableTuntasList &&
                            !currentTuntasId.isNullOrBlank()
                        ) {
                            activeTuntai.any { it.id == currentTuntasId }
                        } else {
                            true
                        }
                        if (!activeTuntasStillAvailable) {
                            tokenManager.clearActiveTuntas()
                            currentTuntasId = null
                        }
                        if (
                            currentToken != null &&
                            sessionIsValid &&
                            currentTuntasId.isNullOrBlank() &&
                            activeTuntai.size == 1
                        ) {
                            val tuntas = activeTuntai.first()
                            val permissionsResult = userRepository.getMyPermissions(tuntas.id)
                            tokenManager.setActiveTuntas(tuntas.id, tuntas.name)
                            tokenManager.setActiveOrgUnit(null)
                            permissionsResult.onSuccess {
                                tokenManager.savePermissions(it.permissions)
                                tokenManager.saveLeadershipUnitIds(it.leadershipUnitIds)
                                tokenManager.cacheTuntasContext(tuntas.id, it.permissions, it.leadershipUnitIds)
                            }.onFailure {
                                tokenManager.permissionsForTuntas(tuntas.id)?.let { permissions ->
                                    val leadershipUnitIds = tokenManager.leadershipUnitIdsForTuntas(tuntas.id).orEmpty()
                                    tokenManager.savePermissions(permissions.toList())
                                    tokenManager.saveLeadershipUnitIds(leadershipUnitIds)
                                }
                            }
                            currentTuntasId = tuntas.id
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
                        startDestination = startDestination!!,
                        notificationRoute = notificationRoute,
                        passwordResetToken = passwordResetToken,
                        onPasswordResetTokenConsumed = { passwordResetToken = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        passwordResetToken = passwordResetToken(intent)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(permission)
        }
    }

    private fun notificationRoute(intent: android.content.Intent?): String? =
        intent?.getStringExtra(EXTRA_NOTIFICATION_ROUTE)
            ?: intent?.data?.takeIf { it.scheme == "skautai" }?.let { uri ->
                when (uri.host) {
                    "reservation" -> uri.lastPathSegment?.let { NavRoutes.ReservationDetail.createRoute(it) }
                    else -> null
                }
            }

    private fun passwordResetToken(intent: Intent?): String? =
        intent?.data
            ?.takeIf { it.scheme == "skautai" && it.host == "reset-password" }
            ?.getQueryParameter("token")

    private companion object {
        const val EXTRA_NOTIFICATION_ROUTE = "notification_route"
    }
}
