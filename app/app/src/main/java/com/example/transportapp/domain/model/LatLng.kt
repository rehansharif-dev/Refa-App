package com.example.transportapp.domain.model

import com.google.firebase.firestore.PropertyName

data class LatLng(
    @get:PropertyName("latitude") @set:PropertyName("latitude")
    var latitude: Double = 0.0,
    @get:PropertyName("longitude") @set:PropertyName("longitude")
    var longitude: Double = 0.0,
    @get:PropertyName("bearing") @set:PropertyName("bearing")
    var bearing: Float = 0.0f
)
