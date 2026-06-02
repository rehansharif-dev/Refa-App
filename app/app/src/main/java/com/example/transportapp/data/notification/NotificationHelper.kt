package com.example.transportapp.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.transportapp.MainActivity
import com.example.transportapp.R
import com.example.transportapp.RefaApp
import com.example.transportapp.domain.model.Ride
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Shows a high-priority heads-up notification when a new ride request arrives.
     * Tapping it opens the Driver Dashboard.
     */
    fun showRideRequestNotification(ride: Ride) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_driver_dashboard", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ride.id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val vehicleEmoji = when (ride.vehicleType.name) {
            "BIKE"    -> "🏍"
            "RICKSHAW"-> "🛺"
            "PREMIUM" -> "✨"
            else      -> "🚗"
        }

        val notification = NotificationCompat.Builder(context, RefaApp.CHANNEL_RIDE_REQUESTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$vehicleEmoji New Ride Request!")
            .setContentText("${ride.userName} • Rs. ${ride.fare.toInt()} • ${ride.vehicleType.name}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Passenger: ${ride.userName}\n" +
                        "Vehicle: ${ride.vehicleType.name}\n" +
                        "Pickup: ${ride.pickupAddress}\n" +
                        "Drop-off: ${ride.dropAddress}\n" +
                        "Fare: Rs. ${ride.fare.toInt()}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        notificationManager.notify(ride.id.hashCode(), notification)
    }

    /** Cancels a ride-request notification (e.g. driver accepted/ride expired). */
    fun cancelRideNotification(rideId: String) {
        notificationManager.cancel(rideId.hashCode())
    }

    /**
     * Shows a heads-up notification to the PASSENGER when their ride status changes.
     * Called from HomeViewModel whenever the Firestore ride document updates.
     *
     * Status → message mapping:
     *   CONFIRMED → driver accepted, show driver name
     *   ARRIVING  → driver is at pickup point
     *   ONGOING   → trip has started
     *   COMPLETED → trip finished
     */
    fun showPassengerRideNotification(
        rideId: String,
        driverName: String,
        status: com.example.transportapp.domain.model.RideStatus
    ) {
        val (title, body) = when (status) {
            com.example.transportapp.domain.model.RideStatus.CONFIRMED ->
                Pair(
                    "🎉 Driver Found!",
                    if (driverName.isNotBlank()) "$driverName is on the way to pick you up."
                    else "Your driver is on the way to pick you up."
                )
            com.example.transportapp.domain.model.RideStatus.ARRIVING ->
                Pair(
                    "📍 Driver Has Arrived!",
                    if (driverName.isNotBlank()) "$driverName is waiting at your pickup point."
                    else "Your driver is waiting at the pickup point."
                )
            com.example.transportapp.domain.model.RideStatus.ONGOING ->
                Pair("🚗 Trip Started!", "Sit back and enjoy your ride.")
            com.example.transportapp.domain.model.RideStatus.COMPLETED ->
                Pair("✅ Trip Completed!", "Thank you for riding with Refa. Have a great day!")
            else -> return  // No notification for SEARCHING or CANCELLED
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, rideId.hashCode() + status.ordinal, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RefaApp.CHANNEL_RIDE_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        notificationManager.notify(rideId.hashCode() + status.ordinal, notification)
    }

    /** Generic info notification (used for FCM data payloads). */
    fun showGenericNotification(title: String, body: String, notifId: Int = System.currentTimeMillis().toInt()) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RefaApp.CHANNEL_RIDE_REQUESTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()
        notificationManager.notify(notifId, notification)
    }

    fun areNotificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()
}
