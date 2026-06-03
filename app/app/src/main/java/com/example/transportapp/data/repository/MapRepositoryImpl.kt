package com.example.transportapp.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import android.util.Log
import com.example.transportapp.data.remote.DirectionsApiService
import com.example.transportapp.data.remote.DirectionsResponse
import com.example.transportapp.domain.repository.MapRepository
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.PolyUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject
import kotlin.coroutines.resume

class MapRepositoryImpl @Inject constructor(
    private val db: FirebaseDatabase,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val directionsApiService: DirectionsApiService,
    @param:ApplicationContext private val context: Context
) : MapRepository {

    private val API_KEY = "YOUR_MAPS_API_KEY_HERE"
    
    override fun getNearbyDrivers(location: com.example.transportapp.domain.model.LatLng): Flow<List<com.example.transportapp.domain.model.Driver>> = callbackFlow {
        val driversRef = db.getReference("drivers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drivers = snapshot.children.mapNotNull { it.getValue(com.example.transportapp.domain.model.Driver::class.java) }
                    .filter { it.isOnline }
                    .filter { driver ->
                        val driverLocation = driver.location
                        val distanceKm = haversineKm(
                            location.latitude,
                            location.longitude,
                            driverLocation.latitude,
                            driverLocation.longitude
                        )
                        distanceKm <= 20.0 || (driverLocation.latitude == 0.0 && driverLocation.longitude == 0.0)
                    }
                trySend(drivers)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MapRepo", "Cloud sync failed: ${error.message}")
                close(error.toException())
            }
        }
        driversRef.addValueEventListener(listener)
        awaitClose { driversRef.removeEventListener(listener) }
    }

    override fun getDriver(driverId: String): Flow<com.example.transportapp.domain.model.Driver?> = callbackFlow {
        val driverRef = db.getReference("drivers").child(driverId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val driver = snapshot.getValue(com.example.transportapp.domain.model.Driver::class.java)
                    trySend(driver)
                } catch (e: Exception) {
                    Log.e("MapRepo", "Error parsing driver data", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        driverRef.addValueEventListener(listener)
        awaitClose { driverRef.removeEventListener(listener) }
    }

    @SuppressLint("MissingPermission")
    override fun getUserLocation(): Flow<com.example.transportapp.domain.model.LatLng> = callbackFlow {
        // Step 1 — emit a cached location immediately (zero-delay) if one exists
        launch {
            try {
                val lastLoc = fusedLocationProviderClient.lastLocation.await()
                if (lastLoc != null) {
                    trySend(com.example.transportapp.domain.model.LatLng(lastLoc.latitude, lastLoc.longitude, lastLoc.bearing))
                } else {
                    // Step 2 — no cache (first install / fresh boot): actively request a
                    // single fresh fix. This is faster than waiting for the first
                    // requestLocationUpdates callback and solves the "stuck on
                    // Getting your location" on first launch without a restart.
                    val freshLoc = fusedLocationProviderClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .await()
                    if (freshLoc != null) {
                        trySend(com.example.transportapp.domain.model.LatLng(freshLoc.latitude, freshLoc.longitude, freshLoc.bearing))
                    }
                }
            } catch (e: Exception) {
                Log.d("MapRepo", "Initial location fetch: ${e.message}")
            }
        }

        // Step 3 — ongoing real-time updates every 2 s
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    trySend(com.example.transportapp.domain.model.LatLng(it.latitude, it.longitude, it.bearing))
                }
            }
        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("MapRepo", "Failed to request location updates", e)
        }

        awaitClose { fusedLocationProviderClient.removeLocationUpdates(locationCallback) }
    }

    override suspend fun updateDriverLocation(driverId: String, location: com.example.transportapp.domain.model.LatLng, bearing: Float) {
        val updates = mapOf(
            "location" to location,
            "bearing" to bearing,
            "isOnline" to true
        )
        try {
            db.getReference("drivers").child(driverId).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e("MapRepo", "Failed to update driver location", e)
        }
    }

    override suspend fun saveDriver(driver: com.example.transportapp.domain.model.Driver) {
        db.getReference("drivers").child(driver.id).setValue(driver).await()
    }

    override suspend fun getAddressFromLocation(latLng: com.example.transportapp.domain.model.LatLng): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            withTimeoutOrNull(5000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                            }
                            override fun onError(errorMessage: String?) {
                                continuation.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    addresses?.firstOrNull()?.getAddressLine(0)
                }
            }
        } catch (e: Exception) {
            Log.e("MapRepo", "Geocoder failed", e)
            null
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return earthRadiusKm * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }

    override suspend fun getDirections(origin: com.example.transportapp.domain.model.LatLng, destination: com.example.transportapp.domain.model.LatLng): List<com.example.transportapp.domain.model.LatLng> = withContext(Dispatchers.IO) {
        try {
            val response: DirectionsResponse = directionsApiService.getDirections(
                origin = "${origin.latitude},${origin.longitude}",
                destination = "${destination.latitude},${destination.longitude}",
                apiKey = API_KEY
            )
            val points = response.routes.firstOrNull()?.overviewPolyline?.points ?: return@withContext emptyList()
            val decodedPath = PolyUtil.decode(points)
            decodedPath.map { com.example.transportapp.domain.model.LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
