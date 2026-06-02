package com.example.transportapp.domain.repository

import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.VehicleType
import kotlinx.coroutines.flow.Flow

interface RideRepository {
    suspend fun requestRide(ride: Ride): Result<String>
    fun getRideUpdates(rideId: String): Flow<Ride>
    /**
     * Fix #1 + #3: vehicleType filter so each driver ONLY sees rides matching
     * their vehicle type, reducing Firestore data and speeding up matching.
     */
    fun getAvailableRides(vehicleType: VehicleType? = null): Flow<List<Ride>>
    suspend fun acceptRide(rideId: String, driverId: String, driverName: String = ""): Result<Unit>
    suspend fun updateRideStatus(rideId: String, status: RideStatus)
    suspend fun updateRideLocation(rideId: String, location: com.example.transportapp.domain.model.LatLng)
    suspend fun cancelRide(rideId: String)
    fun getRideHistory(userId: String): Flow<List<Ride>>
    fun getDriverRideHistory(driverId: String): Flow<List<Ride>>
}
