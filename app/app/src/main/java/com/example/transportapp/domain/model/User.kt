package com.example.transportapp.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    
    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("address") @set:PropertyName("address")
    var address: String = "",
    
    @get:PropertyName("profilePicture") @set:PropertyName("profilePicture")
    var profilePicture: String = "",
    
    @get:PropertyName("walletBalance") @set:PropertyName("walletBalance")
    var walletBalance: Double = 400.0,
    
    @get:PropertyName("rating") @set:PropertyName("rating")
    var rating: Double = 5.0,
    
    @get:PropertyName("isDriverMode") @set:PropertyName("isDriverMode")
    var isDriverMode: Boolean = false,
    
    @get:PropertyName("vehicle") @set:PropertyName("vehicle")
    var vehicle: Vehicle? = null
)
