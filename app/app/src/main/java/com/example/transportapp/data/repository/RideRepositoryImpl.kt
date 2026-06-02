package com.example.transportapp.data.repository

import android.util.Log
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.VehicleType
import com.example.transportapp.domain.repository.RideRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RideRepository {

    override suspend fun requestRide(ride: Ride): Result<String> {
        return try {
            val documentRef = firestore.collection("rides").add(ride).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRideUpdates(rideId: String): Flow<Ride> = callbackFlow {
        // MetadataChanges.INCLUDE delivers cached data instantly before waiting for server,
        // giving passengers near-zero latency when the driver accepts.
        val subscription = firestore.collection("rides").document(rideId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.toObject(Ride::class.java)?.let {
                    trySend(it.copy(id = snapshot.id))
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Fix #1 + #3: vehicleType filter — if provided, only rides matching the
     * driver's vehicle type are returned. This halves the Firestore data
     * transferred (most deployments have 2-4 vehicle types) and makes the
     * "Accept Request" flow meaningful — the driver only sees rides they can
     * actually fulfil.
     *
     * MetadataChanges.INCLUDE delivers any locally-cached matching rides
     * instantly while the server round-trip completes, making the list appear
     * immediately after going online.
     */
    override fun getAvailableRides(vehicleType: VehicleType?): Flow<List<Ride>> = callbackFlow {
        val baseQuery = firestore.collection("rides")
            .whereEqualTo("status", RideStatus.SEARCHING.name)

        // Apply vehicleType filter at query level when known — reduces network data
        val query = if (vehicleType != null) {
            baseQuery.whereEqualTo("vehicleType", vehicleType.name)
        } else {
            baseQuery
        }

        val subscription = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val rides = snapshot?.documents?.mapNotNull {
                it.toObject(Ride::class.java)?.copy(id = it.id)
            } ?: emptyList()
            trySend(rides)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Fix #1: Atomic transaction for acceptRide — prevents two drivers from
     * simultaneously accepting the same ride, and reduces the number of
     * Firestore writes needed to confirm a booking.
     */
    override suspend fun acceptRide(rideId: String, driverId: String, driverName: String): Result<Unit> {
        return try {
            val rideRef = firestore.collection("rides").document(rideId)
            firestore.runTransaction { transaction ->
                val snap = transaction.get(rideRef)
                val currentStatus = snap.getString("status")
                if (currentStatus == RideStatus.SEARCHING.name) {
                    transaction.update(
                        rideRef,
                        mapOf(
                            "driverId" to driverId,
                            "driverName" to driverName,
                            "status" to RideStatus.CONFIRMED.name
                        )
                    )
                } else {
                    throw Exception("Ride no longer available (status: $currentStatus)")
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RideRepo", "acceptRide failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateRideStatus(rideId: String, status: RideStatus) {
        try {
            firestore.collection("rides").document(rideId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            Log.e("RideRepo", "Failed to update status", e)
        }
    }

    override suspend fun updateRideLocation(rideId: String, location: com.example.transportapp.domain.model.LatLng) {
        try {
            val locationData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "bearing" to location.bearing
            )
            firestore.collection("rides").document(rideId)
                .update("passengerLocation", locationData)
                .await()
        } catch (e: Exception) {
            Log.e("RideRepo", "Failed to update passenger location", e)
        }
    }

    override suspend fun cancelRide(rideId: String) {
        updateRideStatus(rideId, RideStatus.CANCELLED)
    }

    override fun getRideHistory(userId: String): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, _ ->
                val rides = snapshot?.documents?.mapNotNull {
                    it.toObject(Ride::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { subscription.remove() }
    }

    override fun getDriverRideHistory(driverId: String): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, _ ->
                val rides = snapshot?.documents?.mapNotNull {
                    it.toObject(Ride::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(rides)
            }
        awaitClose { subscription.remove() }
    }
}
