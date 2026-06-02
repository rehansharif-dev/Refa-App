package com.example.transportapp.data.remote

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<DirectionsRoute>
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: DirectionsPolyline
)

data class DirectionsPolyline(
    @SerializedName("points") val points: String
)
