package com.example.transportapp.domain.model

import com.google.firebase.firestore.PropertyName

data class Driver(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",
    @get:PropertyName("rating") @set:PropertyName("rating")
    var rating: Double = 5.0,
    @get:PropertyName("vehicle") @set:PropertyName("vehicle")
    var vehicle: Vehicle = Vehicle(),
    @get:PropertyName("location") @set:PropertyName("location")
    var location: LatLng = LatLng(),
    @get:PropertyName("bearing") @set:PropertyName("bearing")
    var bearing: Float = 0.0f,
    @get:PropertyName("isOnline") @set:PropertyName("isOnline")
    var isOnline: Boolean = false
)
