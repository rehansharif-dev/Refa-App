package com.example.transportapp.data.service

import android.util.Log
import com.example.transportapp.data.notification.NotificationHelper
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.VehicleType
import com.example.transportapp.domain.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Handles FCM messages in all app states (foreground, background, killed).
 *
 * Expected data payload (sent by Cloud Function or admin SDK):
 * {
 *   "type": "ride_request",
 *   "rideId": "...",
 *   "passengerName": "...",
 *   "vehicleType": "BIKE|CAR|RICKSHAW|PREMIUM",
 *   "pickup": "...",
 *   "drop": "...",
 *   "fare": "350"
 * }
 */
@AndroidEntryPoint
class RideNotificationService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: refreshed")
        // Persist updated token to Firestore so Cloud Functions can reach this device
        serviceScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update("fcmToken", token)
                    .await()
                Log.d("FCM", "Token saved for user $uid")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to save token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: "general"

        when (type) {
            "ride_request" -> handleRideRequestMessage(data)
            else -> {
                // Notification message or unknown type — show a generic alert
                val title = message.notification?.title ?: data["title"] ?: "Refa"
                val body  = message.notification?.body  ?: data["body"]  ?: "You have a new notification"
                notificationHelper.showGenericNotification(title, body)
            }
        }
    }

    private fun handleRideRequestMessage(data: Map<String, String>) {
        try {
            val rideId        = data["rideId"]        ?: return
            val passengerName = data["passengerName"] ?: "Passenger"
            val vehicleType   = runCatching { VehicleType.valueOf(data["vehicleType"] ?: "CAR") }.getOrDefault(VehicleType.CAR)
            val pickup        = data["pickup"]        ?: ""
            val drop          = data["drop"]          ?: ""
            val fare          = data["fare"]?.toDoubleOrNull() ?: 0.0

            val syntheticRide = Ride(
                id           = rideId,
                userName     = passengerName,
                vehicleType  = vehicleType,
                pickupAddress = pickup,
                dropAddress  = drop,
                fare         = fare,
                status       = RideStatus.SEARCHING,
                pickupLocation = LatLng(),
                dropLocation   = LatLng()
            )
            notificationHelper.showRideRequestNotification(syntheticRide)
        } catch (e: Exception) {
            Log.e("FCM", "Error handling ride_request message", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
