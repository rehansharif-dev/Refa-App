package com.example.transportapp.domain.repository

import android.net.Uri
import com.example.transportapp.domain.model.User
import com.example.transportapp.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    /** Returns the uid of the signed-in user. Used after login to route to Home vs Driver. */
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<Unit>
    suspend fun signOut()
    suspend fun updateWalletBalance(userId: String, newBalance: Double)
    suspend fun toggleDriverMode(userId: String, isEnabled: Boolean): Result<Unit>
    /** Sets driver isOnline=false in RTDB only — does NOT touch isDriverMode in Firestore. */
    suspend fun goOfflineOnly(userId: String)
    suspend fun updateVehicleDetails(userId: String, vehicle: Vehicle): Result<Unit>
    suspend fun registerDriverAndGoOnline(userId: String, vehicle: Vehicle): Result<Unit>
    suspend fun transferFare(passengerId: String, driverId: String, fare: Double): Result<Unit>
    suspend fun getUserWalletBalance(userId: String): Double
    suspend fun updateUserName(userId: String, newName: String): Result<Unit>
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String>
    /** Clears the profile picture from Firestore (and Storage if present). */
    suspend fun removeProfilePicture(userId: String): Result<Unit>
    /** Saves phone number and address for a driver to both Firestore and Realtime Database. */
    suspend fun updateDriverContactDetails(userId: String, phoneNumber: String, address: String): Result<Unit>
    /** Saves the FCM device token to Firestore so Cloud Functions can push to this device. */
    suspend fun saveFcmToken(userId: String, token: String)
    /**
     * Fix #2: Direct single Firestore read (no Flow overhead) to check isDriverMode
     * immediately after login — avoids the race condition where firstOrNull() on the
     * Flow collects before the Firestore snapshot fires.
     */
    suspend fun getIsDriverMode(userId: String): Boolean
    /**
     * Fetch the epoch-ms timestamp when the 15-day driver lock expires from Firestore.
     * Returns 0L if not set (user has never activated driver mode).
     * This is the SERVER-SIDE source of truth — persists across uninstalls and logouts.
     */
    suspend fun getDriverLockUntil(userId: String): Long
    /**
     * Persist the epoch-ms lock expiry to Firestore so it survives uninstalls,
     * logouts, and device changes. Called exactly once when the user accepts
     * the Driver Policy for the first time.
     */
    suspend fun saveDriverLockUntil(userId: String, lockUntilMs: Long)
}
