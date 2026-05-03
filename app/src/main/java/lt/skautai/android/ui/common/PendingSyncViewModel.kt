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

    fun dismissFailed() {
        viewModelScope.launch {
            pendingOperationRepository.dismissFailed()
        }
    }

    fun dismissOperation(operationId: String) {
        viewModelScope.launch {
            pendingOperationRepository.dismissOperation(operationId)
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
    PendingEntityType.BENDRAS_REQUEST -> "Paėmimo prašymas"
    PendingEntityType.REQUISITION -> "Pirkimo prašymas"
    PendingEntityType.LOCATION -> "Vieta"
    PendingEntityType.MEMBER -> "Narys"
    PendingEntityType.ORGANIZATIONAL_UNIT -> "Vienetas"
    PendingEntityType.EVENT -> "Renginys"
    else -> type
}

private fun operationTypeLabel(type: String): String = when (type) {
    PendingOperationType.ITEM_CREATE -> "Sukurtas daiktas"
    PendingOperationType.ITEM_UPDATE -> "Atnaujintas daiktas"
    PendingOperationType.ITEM_DELETE -> "Ištrintas daiktas"
    PendingOperationType.RESERVATION_CREATE -> "Sukurta rezervacija"
    PendingOperationType.RESERVATION_CANCEL -> "Atšaukta rezervacija"
    PendingOperationType.RESERVATION_REVIEW_UNIT -> "Peržiūrėta rezervacija"
    PendingOperationType.RESERVATION_REVIEW_TOP_LEVEL -> "Patvirtinta rezervacija"
    PendingOperationType.RESERVATION_UPDATE_STATUS -> "Atnaujinta rezervacija"
    PendingOperationType.RESERVATION_UPDATE_PICKUP -> "Atnaujintas paėmimas"
    PendingOperationType.RESERVATION_UPDATE_RETURN -> "Atnaujintas grąžinimas"
    PendingOperationType.RESERVATION_MOVEMENT -> "Registruotas judėjimas"
    PendingOperationType.BENDRAS_REQUEST_CREATE -> "Sukurtas paėmimo prašymas"
    PendingOperationType.BENDRAS_REQUEST_CANCEL -> "Atšauktas paėmimo prašymas"
    PendingOperationType.BENDRAS_REQUEST_REVIEW_UNIT -> "Peržiūrėtas paėmimo prašymas"
    PendingOperationType.BENDRAS_REQUEST_REVIEW_TOP_LEVEL -> "Patvirtintas paėmimo prašymas"
    PendingOperationType.REQUISITION_CREATE -> "Sukurtas pirkimo prašymas"
    PendingOperationType.REQUISITION_CANCEL -> "Atšauktas pirkimo prašymas"
    PendingOperationType.REQUISITION_REVIEW_UNIT -> "Peržiūrėtas pirkimo prašymas"
    PendingOperationType.REQUISITION_REVIEW_TOP_LEVEL -> "Patvirtintas pirkimo prašymas"
    PendingOperationType.LOCATION_CREATE -> "Sukurta vieta"
    PendingOperationType.LOCATION_UPDATE -> "Atnaujinta vieta"
    PendingOperationType.LOCATION_DELETE -> "Ištrinta vieta"
    PendingOperationType.MEMBER_ASSIGN_LEADERSHIP_ROLE -> "Priskirta pareiga"
    PendingOperationType.MEMBER_REMOVE_LEADERSHIP_ROLE -> "Panaikinta pareiga"
    PendingOperationType.MEMBER_STEP_DOWN_LEADERSHIP_ROLE -> "Atsisakyta pareigų"
    PendingOperationType.MEMBER_ASSIGN_RANK -> "Priskirtas laipsnis"
    PendingOperationType.MEMBER_REMOVE_RANK -> "Panaikintas laipsnis"
    PendingOperationType.MEMBER_REMOVE -> "Pašalintas narys"
    PendingOperationType.UNIT_CREATE -> "Sukurtas vienetas"
    PendingOperationType.UNIT_UPDATE -> "Atnaujintas vienetas"
    PendingOperationType.UNIT_DELETE -> "Ištrintas vienetas"
    PendingOperationType.UNIT_ASSIGN_MEMBER -> "Pridėtas narys"
    PendingOperationType.UNIT_REMOVE_MEMBER -> "Pašalintas narys"
    PendingOperationType.UNIT_LEAVE -> "Paliktas vienetas"
    PendingOperationType.UNIT_MOVE_MEMBER -> "Perkeltas narys"
    PendingOperationType.EVENT_CREATE -> "Sukurtas renginys"
    PendingOperationType.EVENT_UPDATE -> "Atnaujintas renginys"
    PendingOperationType.EVENT_CANCEL -> "Atšauktas renginys"
    PendingOperationType.EVENT_ASSIGN_ROLE -> "Priskirta renginio pareiga"
    PendingOperationType.EVENT_REMOVE_ROLE -> "Pašalinta renginio pareiga"
    PendingOperationType.EVENT_CREATE_BUCKET -> "Sukurta plano grupė"
    PendingOperationType.EVENT_UPDATE_BUCKET -> "Atnaujinta plano grupė"
    PendingOperationType.EVENT_DELETE_BUCKET -> "Ištrinta plano grupė"
    PendingOperationType.EVENT_CREATE_ITEM -> "Sukurta plano eilutė"
    PendingOperationType.EVENT_CREATE_ITEMS_BULK -> "Sukurtos plano eilutės"
    PendingOperationType.EVENT_UPDATE_ITEM -> "Atnaujinta plano eilutė"
    PendingOperationType.EVENT_DELETE_ITEM -> "Ištrinta plano eilutė"
    PendingOperationType.EVENT_CREATE_ALLOCATION -> "Sukurtas paskirstymas"
    PendingOperationType.EVENT_UPDATE_ALLOCATION -> "Atnaujintas paskirstymas"
    PendingOperationType.EVENT_DELETE_ALLOCATION -> "Ištrintas paskirstymas"
    PendingOperationType.EVENT_CREATE_PASTOVYKLE -> "Sukurta pastovyklė"
    PendingOperationType.EVENT_UPDATE_PASTOVYKLE -> "Atnaujinta pastovyklė"
    PendingOperationType.EVENT_DELETE_PASTOVYKLE -> "Ištrinta pastovyklė"
    PendingOperationType.EVENT_ASSIGN_PASTOVYKLE_INVENTORY -> "Priskirtas pastovyklės inventorius"
    PendingOperationType.EVENT_UPDATE_PASTOVYKLE_INVENTORY -> "Atnaujintas pastovyklės inventorius"
    PendingOperationType.EVENT_DELETE_PASTOVYKLE_INVENTORY -> "Ištrintas pastovyklės inventorius"
    PendingOperationType.EVENT_CREATE_PASTOVYKLE_REQUEST -> "Sukurtas pastovyklės poreikis"
    PendingOperationType.EVENT_APPROVE_PASTOVYKLE_REQUEST -> "Patvirtintas pastovyklės poreikis"
    PendingOperationType.EVENT_REJECT_PASTOVYKLE_REQUEST -> "Atmestas pastovyklės poreikis"
    PendingOperationType.EVENT_SELF_PROVIDE_PASTOVYKLE_REQUEST -> "Pastovyklės poreikis pažymėtas savo jėgomis"
    PendingOperationType.EVENT_FULFILL_PASTOVYKLE_REQUEST -> "Įvykdytas pastovyklės poreikis"
    PendingOperationType.EVENT_ASSIGN_FROM_UNIT_INVENTORY -> "Pažymėtas inventorius iš vieneto"
    PendingOperationType.EVENT_CREATE_PURCHASE -> "Sukurtas pirkimas"
    PendingOperationType.EVENT_ATTACH_PURCHASE_INVOICE -> "Prisegta pirkimo sąskaita"
    PendingOperationType.EVENT_COMPLETE_PURCHASE -> "Užbaigtas pirkimas"
    PendingOperationType.EVENT_ADD_PURCHASE_TO_INVENTORY -> "Pirkimas pridėtas į inventorių"
    PendingOperationType.EVENT_CREATE_INVENTORY_MOVEMENT -> "Registruotas renginio judėjimas"
    else -> type
}
