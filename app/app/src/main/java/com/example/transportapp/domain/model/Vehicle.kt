package com.example.transportapp.domain.model

import com.google.firebase.firestore.PropertyName

data class Vehicle(
    @get:PropertyName("model") @set:PropertyName("model")
    var model: String = "",
    @get:PropertyName("plateNumber") @set:PropertyName("plateNumber")
    var plateNumber: String = "",
    @get:PropertyName("type") @set:PropertyName("type")
    var type: VehicleType = VehicleType.CAR
)
