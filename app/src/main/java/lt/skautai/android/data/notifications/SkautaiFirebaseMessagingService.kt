package lt.skautai.android.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lt.skautai.android.MainActivity
import lt.skautai.android.R
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository

@AndroidEntryPoint
class SkautaiFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var fcmTokenRegistrar: FcmTokenRegistrar
    @Inject lateinit var itemRepository: ItemRepository
    @Inject lateinit var reservationRepository: ReservationRepository
    @Inject lateinit var requestRepository: RequestRepository
    @Inject lateinit var requisitionRepository: RequisitionRepository
    @Inject lateinit var eventRepository: EventRepository
    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var organizationalUnitRepository: OrganizationalUnitRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        fcmTokenRegistrar.registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val resource = message.data["resource"]
        if (!resource.isNullOrBlank()) {
            scope.launch { refreshResource(resource) }
        }

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
            showNotification(
                title = title ?: getString(R.string.app_name),
                body = body ?: "",
                data = message.data
            )
        }
    }

    private suspend fun refreshResource(resource: String) {
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

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannel()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            notificationRoute(data)?.let { route ->
                putExtra(EXTRA_NOTIFICATION_ROUTE, route)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            data.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun notificationRoute(data: Map<String, String>): String? =
        when (data["resource"]) {
            "reservations" -> data["reservationId"]?.let { "reservation_detail/$it" }
            "bendras_requests" -> data["requestId"]?.let { "shared_request_detail/$it" }
            "requisitions" -> data["requestId"]?.let { "request_detail/$it" }
            "announcement" -> "notifications"
            else -> null
        }

    private companion object {
        const val EXTRA_NOTIFICATION_ROUTE = "notification_route"
    }
}
