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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.local.entity.PendingOperationEntity
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationType

@HiltViewModel
class PendingSyncViewModel @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val syncStatus: StateFlow<PendingSyncStatus> = combine(
        pendingOperationRepository.observePendingCount(),
        pendingOperationRepository.observeFailedCount(),
        observeOnline(),
        pendingOperationRepository.observeVisibleOperations()
    ) { pendingCount, failedCount, isOnline, operations ->
        PendingSyncStatus(
            pendingCount = pendingCount,
            failedCount = failedCount,
            isOffline = !isOnline,
            operations = operations.map { it.toUiModel() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingSyncStatus())

    fun retryFailed() {
        viewModelScope.launch {
            pendingOperationRepository.retryFailed()
        }
    }

    private fun observeOnline(): Flow<Boolean> = callbackFlow {
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
    val isOffline: Boolean = false,
    val operations: List<PendingSyncOperationUi> = emptyList()
)

data class PendingSyncOperationUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val statusLabel: String,
    val error: String?,
    val createdAt: String
)

private fun PendingOperationEntity.toUiModel(): PendingSyncOperationUi =
    PendingSyncOperationUi(
        id = id,
        title = operationTypeLabel(operationType),
        subtitle = entityTypeLabel(entityType),
        statusLabel = statusLabel(status),
        error = lastError,
        createdAt = createdAt.take(16).replace("T", " ")
    )

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Laukia"
    "SYNCING" -> "Sinchronizuojama"
    "FAILED" -> "Nepavyko"
    else -> status
}

private fun entityTypeLabel(type: String): String = when (type) {
    PendingEntityType.ITEM -> "Inventorius"
    PendingEntityType.RESERVATION -> "Rezervacija"
    PendingEntityType.BENDRAS_REQUEST -> "Paemimo prasymas"
    PendingEntityType.REQUISITION -> "Pirkimo prasymas"
    PendingEntityType.LOCATION -> "Vieta"
    PendingEntityType.MEMBER -> "Narys"
    PendingEntityType.ORGANIZATIONAL_UNIT -> "Vienetas"
    PendingEntityType.EVENT -> "Renginys"
    else -> type
}

private fun operationTypeLabel(type: String): String = when (type) {
    PendingOperationType.ITEM_CREATE -> "Sukurtas daiktas"
    PendingOperationType.ITEM_UPDATE -> "Atnaujintas daiktas"
    PendingOperationType.ITEM_DELETE -> "Istrintas daiktas"
    PendingOperationType.RESERVATION_CREATE -> "Sukurta rezervacija"
    PendingOperationType.RESERVATION_CANCEL -> "Atsaukta rezervacija"
    PendingOperationType.RESERVATION_REVIEW_UNIT -> "Perziureta rezervacija"
    PendingOperationType.RESERVATION_REVIEW_TOP_LEVEL -> "Patvirtinta rezervacija"
    PendingOperationType.RESERVATION_UPDATE_STATUS -> "Atnaujinta rezervacija"
    PendingOperationType.RESERVATION_UPDATE_PICKUP -> "Atnaujintas paemimas"
    PendingOperationType.RESERVATION_UPDATE_RETURN -> "Atnaujintas grazinimas"
    PendingOperationType.RESERVATION_MOVEMENT -> "Registruotas judejimas"
    PendingOperationType.BENDRAS_REQUEST_CREATE -> "Sukurtas paemimo prasymas"
    PendingOperationType.BENDRAS_REQUEST_CANCEL -> "Atsauktas paemimo prasymas"
    PendingOperationType.BENDRAS_REQUEST_REVIEW_UNIT -> "Perziuretas paemimo prasymas"
    PendingOperationType.BENDRAS_REQUEST_REVIEW_TOP_LEVEL -> "Patvirtintas paemimo prasymas"
    PendingOperationType.REQUISITION_CREATE -> "Sukurtas pirkimo prasymas"
    PendingOperationType.REQUISITION_REVIEW_UNIT -> "Perziuretas pirkimo prasymas"
    PendingOperationType.REQUISITION_REVIEW_TOP_LEVEL -> "Patvirtintas pirkimo prasymas"
    PendingOperationType.LOCATION_CREATE -> "Sukurta vieta"
    PendingOperationType.MEMBER_ASSIGN_LEADERSHIP_ROLE -> "Priskirta pareiga"
    PendingOperationType.MEMBER_REMOVE_LEADERSHIP_ROLE -> "Panaikinta pareiga"
    PendingOperationType.MEMBER_STEP_DOWN_LEADERSHIP_ROLE -> "Atsisakyta pareigu"
    PendingOperationType.MEMBER_ASSIGN_RANK -> "Priskirtas laipsnis"
    PendingOperationType.MEMBER_REMOVE_RANK -> "Panaikintas laipsnis"
    PendingOperationType.MEMBER_REMOVE -> "Pasalintas narys"
    PendingOperationType.UNIT_CREATE -> "Sukurtas vienetas"
    PendingOperationType.UNIT_UPDATE -> "Atnaujintas vienetas"
    PendingOperationType.UNIT_DELETE -> "Istrintas vienetas"
    PendingOperationType.UNIT_ASSIGN_MEMBER -> "Pridetas narys"
    PendingOperationType.UNIT_REMOVE_MEMBER -> "Pasalintas narys"
    PendingOperationType.UNIT_LEAVE -> "Paliktas vienetas"
    PendingOperationType.UNIT_MOVE_MEMBER -> "Perkeltas narys"
    PendingOperationType.EVENT_CREATE -> "Sukurtas renginys"
    PendingOperationType.EVENT_UPDATE -> "Atnaujintas renginys"
    PendingOperationType.EVENT_CANCEL -> "Atsauktas renginys"
    else -> type
}
