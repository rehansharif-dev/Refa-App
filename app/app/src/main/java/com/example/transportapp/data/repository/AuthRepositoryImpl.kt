package com.example.transportapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.transportapp.domain.model.User
import com.example.transportapp.domain.model.Vehicle
import com.example.transportapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val db: FirebaseDatabase,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val storage = FirebaseStorage.getInstance()

    // ── User stream ──────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentUser(): Flow<User?> = callbackFlow<FirebaseUser?> {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.flatMapLatest { firebaseUser ->
        if (firebaseUser == null) {
            flowOf(null)
        } else {
            callbackFlow<User?> {
                val sub = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { Log.e("AuthRepo", "Firestore error: ${error.message}"); return@addSnapshotListener }
                        if (snapshot != null && snapshot.exists()) {
                            try { trySend(snapshot.toObject(User::class.java)) }
                            catch (e: Exception) {
                                Log.e("AuthRepo", "Deserialize error", e)
                                trySend(User(id = firebaseUser.uid, email = firebaseUser.email ?: ""))
                            }
                        } else {
                            trySend(User(id = firebaseUser.uid, email = firebaseUser.email ?: "", name = "User", walletBalance = 400.0))
                        }
                    }
                awaitClose { sub.remove() }
            }
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    // ── Auth ─────────────────────────────────────────────────────────────────

    override suspend fun signInWithEmail(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Sign-in failed: no uid")
                Result.success(uid)
            } catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Creation failed")
                firestore.collection("users").document(uid)
                    .set(User(id = uid, email = email, name = name, walletBalance = 400.0)).await()
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun signOut() { auth.signOut() }

    override suspend fun getIsDriverMode(userId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val snap = firestore.collection("users").document(userId).get().await()
                snap.getBoolean("isDriverMode") ?: false
            } catch (e: Exception) {
                Log.e("AuthRepo", "getIsDriverMode failed, defaulting false", e)
                false
            }
        }

    // ── Driver lock (Firestore-persisted, survives uninstalls and logouts) ──────

    override suspend fun getDriverLockUntil(userId: String): Long =
        withContext(Dispatchers.IO) {
            try {
                val snap = firestore.collection("users").document(userId).get().await()
                snap.getLong("driverLockUntil") ?: 0L
            } catch (e: Exception) {
                Log.e("AuthRepo", "getDriverLockUntil failed", e)
                0L
            }
        }

    override suspend fun saveDriverLockUntil(userId: String, lockUntilMs: Long) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId)
                    .set(mapOf("driverLockUntil" to lockUntilMs), SetOptions.merge()).await()
                Log.d("AuthRepo", "Driver lock saved: uid=$userId lockUntil=$lockUntilMs")
            } catch (e: Exception) {
                Log.e("AuthRepo", "saveDriverLockUntil failed", e)
            }
        }
    }

    // ── Wallet ───────────────────────────────────────────────────────────────

    override suspend fun updateWalletBalance(userId: String, newBalance: Double) {
        withContext(Dispatchers.IO) {
            try { firestore.collection("users").document(userId).update("walletBalance", newBalance).await() }
            catch (e: Exception) { Log.e("AuthRepo", "updateWalletBalance failed", e) }
        }
    }

    override suspend fun getUserWalletBalance(userId: String): Double =
        withContext(Dispatchers.IO) {
            try { firestore.collection("users").document(userId).get().await().getDouble("walletBalance") ?: 0.0 }
            catch (e: Exception) { 0.0 }
        }

    override suspend fun transferFare(passengerId: String, driverId: String, fare: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val pRef = firestore.collection("users").document(passengerId)
                val dRef = firestore.collection("users").document(driverId)
                firestore.runTransaction { t: Transaction ->
                    val pBal = t.get(pRef).getDouble("walletBalance") ?: 0.0
                    val dBal = t.get(dRef).getDouble("walletBalance") ?: 0.0
                    t.update(pRef, "walletBalance", (pBal - fare).coerceAtLeast(0.0))
                    t.update(dRef, "walletBalance", dBal + fare)
                }.await()
                Result.success(Unit)
            } catch (e: Exception) { Log.e("AuthRepo", "transferFare failed", e); Result.failure(e) }
        }

    // ── Driver Mode ──────────────────────────────────────────────────────────

    override suspend fun toggleDriverMode(userId: String, isEnabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // ── 15-day driver lock guard ──────────────────────────────────────────
                // Prevents switching from driver to passenger while the Firestore lock
                // is active. Repository-level safety net — also enforced in DriverScreen.
                if (!isEnabled) {
                    val lockUntil = try {
                        firestore.collection("users").document(userId)
                            .get().await().getLong("driverLockUntil") ?: 0L
                    } catch (_: Exception) { 0L }
                    val now = System.currentTimeMillis()
                    if (lockUntil > now) {
                        val daysLeft = ((lockUntil - now) / (24L * 60L * 60L * 1_000L)).toInt() + 1
                        return@withContext Result.failure(
                            Exception("Driver lock active: $daysLeft day${if (daysLeft == 1) "" else "s"} remaining.")
                        )
                    }
                }
                // ─────────────────────────────────────────────────────────────────
                firestore.collection("users").document(userId).update("isDriverMode", isEnabled).await()
                val driverRef = db.getReference("drivers").child(userId)
                if (isEnabled) {
                    val snap = firestore.collection("users").document(userId).get().await()
                    val user = snap.toObject(User::class.java)
                    user?.let {
                        val vehicleMap = it.vehicle?.let { v -> mapOf("model" to v.model, "plateNumber" to v.plateNumber, "type" to v.type.name) }
                        val data = mutableMapOf<String, Any>("id" to it.id, "name" to it.name, "phoneNumber" to it.phoneNumber, "rating" to it.rating, "isOnline" to true)
                        vehicleMap?.let { vm -> data["vehicle"] = vm }
                        driverRef.updateChildren(data).await()
                    }
                } else {
                    driverRef.child("isOnline").setValue(false).await()
                }
                Result.success(Unit)
            } catch (e: Exception) { Log.e("AuthRepo", "toggleDriverMode failed", e); Result.failure(e) }
        }

    override suspend fun goOfflineOnly(userId: String) {
        withContext(Dispatchers.IO) {
            try { db.getReference("drivers").child(userId).child("isOnline").setValue(false).await() }
            catch (e: Exception) { Log.e("AuthRepo", "goOfflineOnly failed", e) }
        }
    }

    override suspend fun updateVehicleDetails(userId: String, vehicle: Vehicle): Result<Unit> =
        withContext(Dispatchers.IO) {
            try { firestore.collection("users").document(userId).update("vehicle", vehicle).await(); Result.success(Unit) }
            catch (e: Exception) { Result.failure(e) }
        }

    override suspend fun registerDriverAndGoOnline(userId: String, vehicle: Vehicle): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val snap = firestore.collection("users").document(userId).get().await()
                val vehicleMap = mapOf("model" to vehicle.model, "plateNumber" to vehicle.plateNumber, "type" to vehicle.type.name)
                firestore.collection("users").document(userId)
                    .set(mapOf<String, Any>("vehicle" to vehicleMap, "isDriverMode" to true), SetOptions.merge()).await()
                val driverData = mapOf<String, Any>(
                    "id" to userId, "name" to (snap.getString("name") ?: "Driver"),
                    "phoneNumber" to (snap.getString("phoneNumber") ?: ""),
                    "rating" to (snap.getDouble("rating") ?: 5.0),
                    "isOnline" to true, "vehicle" to vehicleMap
                )
                db.getReference("drivers").child(userId).updateChildren(driverData).await()
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    // ── Profile ──────────────────────────────────────────────────────────────

    override suspend fun updateUserName(userId: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId).update("name", newName).await()
                db.getReference("drivers").child(userId).child("name").setValue(newName).await()
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    /**
     * Saves driver phone number and address to Firestore (users doc) and
     * Realtime Database (drivers node) so passengers can contact the driver.
     */
    override suspend fun updateDriverContactDetails(userId: String, phoneNumber: String, address: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId)
                    .set(mapOf("phoneNumber" to phoneNumber, "address" to address), SetOptions.merge())
                    .await()
                val driverRef = db.getReference("drivers").child(userId)
                driverRef.child("phoneNumber").setValue(phoneNumber).await()
                driverRef.child("address").setValue(address).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepo", "updateDriverContactDetails failed", e)
                Result.failure(e)
            }
        }

    /**
     * BULLETPROOF profile picture upload — two-strategy approach:
     *
     * Strategy 1 (Firebase Storage):
     *   - Read entire image bytes via ContentResolver (safe for content:// URIs on Android 10+)
     *   - Resize to max 400×400 px and compress to JPEG 80% to reduce payload
     *   - Upload with putBytes() (not putStream/putFile which fail on Android 10+ content URIs)
     *   - Get download URL from task snapshot metadata reference (not a separate ref.downloadUrl
     *     call, which is the cause of the "Object does not exist at location" error)
     *
     * Strategy 2 (Firestore Base64 fallback):
     *   - If Strategy 1 fails for ANY reason (rules, bucket not set up, network, etc.)
     *   - Compress image to JPEG 60% and encode as a Base64 data URI
     *   - Store the data URI string directly in the Firestore "profilePicture" field
     *   - No Firebase Storage dependency — guaranteed to work as long as Firestore works
     *   - The UI detects "data:" prefix and passes the decoded bytes directly to Coil
     */
    override suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Step 1: Read bytes through ContentResolver — works for ALL URI types
                val rawBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Cannot open image. Please try again."))

                // Step 2: Decode Bitmap (handles JPEG, PNG, WebP, HEIC, etc.)
                val srcBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    ?: return@withContext Result.failure(Exception("Cannot decode image format."))

                // Step 3: Scale down to max 400×400 maintaining aspect ratio
                val maxDim = 400
                val (newW, newH) = if (srcBitmap.width >= srcBitmap.height) {
                    Pair(maxDim, (srcBitmap.height.toLong() * maxDim / srcBitmap.width).toInt().coerceAtLeast(1))
                } else {
                    Pair((srcBitmap.width.toLong() * maxDim / srcBitmap.height).toInt().coerceAtLeast(1), maxDim)
                }
                val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, newW, newH, true)

                // Step 4: Compress to JPEG bytes at 80% quality (~15–40 KB for a 400×400 photo)
                val compressedBytes = ByteArrayOutputStream().also { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }.toByteArray()

                // ── Strategy 1: Firebase Storage ────────────────────────────
                try {
                    val ref = storage.reference.child("profile_pictures/$userId.jpg")
                    val taskSnapshot = ref.putBytes(compressedBytes).await()

                    // IMPORTANT: get downloadUrl from the task snapshot's storage reference,
                    // NOT from ref.downloadUrl — this is what causes "Object does not exist".
                    val downloadUrl = taskSnapshot.storage.downloadUrl.await().toString()

                    firestore.collection("users").document(userId)
                        .set(mapOf("profilePicture" to downloadUrl), SetOptions.merge())
                        .await()

                    Log.d("AuthRepo", "Profile picture uploaded via Firebase Storage")
                    return@withContext Result.success(downloadUrl)

                } catch (storageEx: Exception) {
                    Log.w("AuthRepo", "Firebase Storage failed (${storageEx.message}), using Firestore Base64 fallback")
                }

                // ── Strategy 2: Firestore Base64 fallback ────────────────────
                // Re-compress at 60% for a smaller payload (Firestore 1MB doc limit).
                // A 400×400 JPEG at 60% is typically 10–30 KB → ~14–41 KB as Base64, well under 1MB.
                val fallbackBytes = ByteArrayOutputStream().also { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
                }.toByteArray()

                val base64Str = Base64.encodeToString(fallbackBytes, Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$base64Str"

                firestore.collection("users").document(userId)
                    .set(mapOf("profilePicture" to dataUri), SetOptions.merge())
                    .await()

                Log.d("AuthRepo", "Profile picture saved as Base64 in Firestore (${fallbackBytes.size / 1024} KB)")
                Result.success(dataUri)

            } catch (e: Exception) {
                Log.e("AuthRepo", "uploadProfilePicture failed completely", e)
                Result.failure(Exception("Photo upload failed: ${e.message}"))
            }
        }

    /**
     * Removes the profile picture:
     *   - Clears the "profilePicture" field in Firestore (sets to empty string)
     *   - If the stored value is an HTTPS URL (Firebase Storage), deletes the object from Storage too
     *   - If the stored value is a Base64 data URI, just clearing Firestore is enough
     */
    override suspend fun removeProfilePicture(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Fetch current value to decide if we need to delete from Storage
                val snap = firestore.collection("users").document(userId).get().await()
                val current = snap.getString("profilePicture") ?: ""

                // Delete from Firebase Storage only if it's a real Storage URL (not a data URI)
                if (current.startsWith("https://")) {
                    try {
                        storage.getReferenceFromUrl(current).delete().await()
                    } catch (e: Exception) {
                        Log.w("AuthRepo", "Storage delete skipped: ${e.message}")
                        // Non-fatal — continue to clear Firestore regardless
                    }
                }

                // Clear the field in Firestore
                firestore.collection("users").document(userId)
                    .set(mapOf("profilePicture" to ""), SetOptions.merge())
                    .await()

                Log.d("AuthRepo", "Profile picture removed for $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepo", "removeProfilePicture failed", e)
                Result.failure(Exception("Could not remove photo: ${e.message}"))
            }
        }

    // ── FCM ──────────────────────────────────────────────────────────────────

    override suspend fun saveFcmToken(userId: String, token: String) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId)
                    .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
                db.getReference("drivers").child(userId).child("fcmToken").setValue(token).await()
                Log.d("AuthRepo", "FCM token saved for $userId")
            } catch (e: Exception) {
                Log.e("AuthRepo", "saveFcmToken failed", e)
            }
        }
    }
}
