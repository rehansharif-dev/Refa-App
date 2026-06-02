package com.example.transportapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RefaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Existing channel — kept for LocationService foreground notification
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LOCATION,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows while driver GPS is active" }
        )

        // HIGH importance — heads-up alert for incoming ride requests
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RIDE_REQUESTS,
                "Ride Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts driver when a new ride request arrives nearby"
                enableVibration(true)
                enableLights(true)
            }
        )

        // General app channel for misc alerts
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "General Refa app notifications" }
        )

        // HIGH importance — passenger ride-status alerts (driver accepted, arrived, etc.)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RIDE_STATUS,
                "Ride Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies passengers when their driver accepts, arrives, or completes a ride"
                enableVibration(true)
                enableLights(true)
            }
        )
    }

    companion object {
        const val CHANNEL_LOCATION      = "location_channel"
        const val CHANNEL_RIDE_REQUESTS = "ride_requests"
        const val CHANNEL_GENERAL       = "general"
        const val CHANNEL_RIDE_STATUS   = "ride_status"
    }
}
