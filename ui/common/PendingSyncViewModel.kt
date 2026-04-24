package lt.skautai.android.ui.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.sync.PendingOperationRepository

@HiltViewModel
class PendingSyncViewModel @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val syncStatus: StateFlow<PendingSyncStatus> = combine(
        pendingOperationRepository.observePendingCount(),
        pendingOperationRepository.observeFailedCount(),
        observeOnline()
    ) { pendingCount, failedCount, isOnline ->
        PendingSyncStatus(
            pendingCount = pendingCount,
            failedCount = failedCount,
            isOffline = !isOnline
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingSyncStatus())

    fun retryFailed() {
        viewModelScope.launch {
            pendingOperationRepository.retryFailed()
        }
    }

    private fun observeOnline() = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun isCurrentlyOnline(): Boolean {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        trySend(isCurrentlyOnline())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isCurrentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(isCurrentlyOnline())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}

data class PendingSyncStatus(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val isOffline: Boolean = false
)
