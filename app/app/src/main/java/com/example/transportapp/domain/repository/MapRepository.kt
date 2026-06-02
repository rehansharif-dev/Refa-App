package com.example.transportapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface MapRepository {
    fun getNearbyDrivers(location: com.example.transportapp.domain.model.LatLng): Flow<List<com.example.transportapp.domain.model.Driver>>
    fun getDriver(driverId: String): Flow<com.example.transportapp.domain.model.Driver?>
    fun getUserLocation(): Flow<com.example.transportapp.domain.model.LatLng>
    suspend fun updateDriverLocation(driverId: String, location: com.example.transportapp.domain.model.LatLng, bearing: Float = 0f)
    suspend fun saveDriver(driver: com.example.transportapp.domain.model.Driver)
    suspend fun getAddressFromLocation(latLng: com.example.transportapp.domain.model.LatLng): String?
    suspend fun getDirections(origin: com.example.transportapp.domain.model.LatLng, destination: com.example.transportapp.domain.model.LatLng): List<com.example.transportapp.domain.model.LatLng>
}
