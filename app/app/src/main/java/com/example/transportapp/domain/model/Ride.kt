package com.example.transportapp.domain.model

import com.google.firebase.firestore.PropertyName

data class Ride(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("userName") @set:PropertyName("userName")
    var userName: String = "",

    @get:PropertyName("userPhone") @set:PropertyName("userPhone")
    var userPhone: String = "",

    @get:PropertyName("driverId") @set:PropertyName("driverId")
    var driverId: String? = null,

    @get:PropertyName("driverName") @set:PropertyName("driverName")
    var driverName: String = "",

    @get:PropertyName("pickupLocation") @set:PropertyName("pickupLocation")
    var pickupLocation: LatLng = LatLng(),

    @get:PropertyName("pickupAddress") @set:PropertyName("pickupAddress")
    var pickupAddress: String = "",

    @get:PropertyName("dropLocation") @set:PropertyName("dropLocation")
    var dropLocation: LatLng = LatLng(),

    @get:PropertyName("dropAddress") @set:PropertyName("dropAddress")
    var dropAddress: String = "",

    @get:PropertyName("vehicleType") @set:PropertyName("vehicleType")
    var vehicleType: VehicleType = VehicleType.CAR,

    @get:PropertyName("fare") @set:PropertyName("fare")
    var fare: Double = 0.0,

    @get:PropertyName("status") @set:PropertyName("status")
    var status: RideStatus = RideStatus.SEARCHING,

    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis(),

    @get:PropertyName("passengerLocation") @set:PropertyName("passengerLocation")
    var passengerLocation: LatLng? = null
)
