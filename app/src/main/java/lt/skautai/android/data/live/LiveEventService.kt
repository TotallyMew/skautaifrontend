package lt.skautai.android.data.live

import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.Constants
import lt.skautai.android.util.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Named

@Singleton
class LiveEventService @Inject constructor(
    private val tokenManager: TokenManager,
    private val itemRepository: ItemRepository,
    private val reservationRepository: ReservationRepository,
    private val requestRepository: RequestRepository,
    private val requisitionRepository: RequisitionRepository,
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val memberRepository: MemberRepository,
    private val organizationalUnitRepository: OrganizationalUnitRepository,
    private val liveRefreshBus: LiveRefreshBus,
    @Named("liveEventOkHttpClient") private val client: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var connectionJob: Job? = null
    private var networkRefreshJob: Job? = null
    private val pendingRefreshResources = mutableSetOf<String>()
    private val lastNetworkRefreshAt = mutableMapOf<String, Long>()

    fun start() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch {
            combine(tokenManager.token, tokenManager.activeTuntasId) { token, tuntasId ->
                token to tuntasId
            }
                .distinctUntilChanged()
                .collectLatest { (token, tuntasId) ->
                    if (token.isNullOrBlank() || tuntasId.isNullOrBlank()) return@collectLatest
                    connectUntilCancelled(token, tuntasId)
                }
        }
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
    }

    private suspend fun connectUntilCancelled(token: String, tuntasId: String) {
        while (currentCoroutineContext().isActive) {
            runCatching {
                openEventStream(token, tuntasId)
            }
            delay(RECONNECT_DELAY_MILLIS)
        }
    }

    private suspend fun openEventStream(token: String, tuntasId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(Constants.BASE_URL.trimEnd('/') + "/api/live/events")
            .header("Authorization", "Bearer $token")
            .header("X-Tuntas-Id", tuntasId)
            .build()
        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) return@use
                val reader = response.body?.charStream()?.buffered() ?: return@use
                var dataLine: String? = null
                while (currentCoroutineContext().isActive) {
                    val line = reader.readLine() ?: break
                    when {
                        line.startsWith("data:") -> dataLine = line.removePrefix("data:").trim()
                        line.isBlank() && !dataLine.isNullOrBlank() -> {
                            val event = runCatching {
                                gson.fromJson(dataLine, LiveEvent::class.java)
                            }.getOrNull()
                            dataLine = null
                            if (event != null) {
                                scope.launch { handleEvent(event) }
                            }
                        }
                    }
                }
            }
        } finally {
            call.cancel()
        }
    }

    private suspend fun handleEvent(event: LiveEvent) {
        liveRefreshBus.emit(event)
        val resources = when (event.resource) {
            "general" -> CORE_REFRESH_RESOURCES
            else -> setOf(event.resource)
        }
        synchronized(pendingRefreshResources) {
            pendingRefreshResources.addAll(resources)
            if (networkRefreshJob?.isActive == true) return
            networkRefreshJob = scope.launch {
                delay(NETWORK_REFRESH_DEBOUNCE_MILLIS)
                val resourcesToRefresh = synchronized(pendingRefreshResources) {
                    pendingRefreshResources.toSet().also { pendingRefreshResources.clear() }
                }
                refreshResources(resourcesToRefresh)
            }
        }
    }

    private suspend fun refreshResources(resources: Set<String>) {
        val now = System.currentTimeMillis()
        resources.forEach { resource ->
            val lastRefresh = lastNetworkRefreshAt[resource] ?: 0L
            if (now - lastRefresh < NETWORK_REFRESH_MIN_INTERVAL_MILLIS) return@forEach
            lastNetworkRefreshAt[resource] = now
            when (resource) {
                "items" -> itemRepository.refreshItems()
                "reservations" -> reservationRepository.refreshReservations()
                "bendras_requests" -> requestRepository.refreshRequests()
                "requisitions" -> requisitionRepository.refreshRequests()
                "events" -> eventRepository.refreshEvents()
                "locations" -> locationRepository.refreshLocations()
                "members" -> memberRepository.refreshMembers()
                "organizational_units" -> organizationalUnitRepository.refreshUnits()
            }
        }
    }

    private companion object {
        const val RECONNECT_DELAY_MILLIS = 5_000L
        const val NETWORK_REFRESH_DEBOUNCE_MILLIS = 1_500L
        const val NETWORK_REFRESH_MIN_INTERVAL_MILLIS = 15_000L
        val CORE_REFRESH_RESOURCES = setOf("items", "reservations", "bendras_requests", "requisitions", "events")
    }
}
