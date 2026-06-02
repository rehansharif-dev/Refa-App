package com.example.transportapp.presentation.driver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.notification.NotificationHelper
import com.example.transportapp.data.service.LocationService
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.User
import com.example.transportapp.domain.model.VehicleType
import com.example.transportapp.domain.repository.AuthRepository
import com.example.transportapp.domain.repository.MapRepository
import com.example.transportapp.domain.repository.RideRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mapRepository: MapRepository,
    private val rideRepository: RideRepository,
    private val notificationHelper: NotificationHelper,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    private var rideObservationJob: Job? = null
    private var driverHistoryJob: Job? = null
    private var availableRidesJob: Job? = null
    private var isTrackingStarted = false

    private var previousRideIds = emptySet<String>()
    private var firstRideLoad = true

    init {
        observeUser()
        observeLocation()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val prevVehicleType = currentUser?.vehicle?.type
                currentUser = user
                _uiState.update { it.copy(user = user) }

                if (user == null) {
                    driverHistoryJob?.cancel(); driverHistoryJob = null
                    availableRidesJob?.cancel(); availableRidesJob = null
                    stopLocationService()
                    return@collect
                }

                // Check Firestore-persisted 15-day driver lock whenever the user loads.
                // This restores driverLockDaysRemaining after reinstall/logout so the
                // DriverScreen can correctly block the "Switch to Passenger" button.
                checkDriverLock(user.id)

                if (user.isDriverMode) {
                    observeDriverHistory(user.id)
                    refreshAndSaveFcmToken(user.id)

                    /**
                     * Fix #1 + #3: Start (or restart) the available-rides subscription
                     * with the driver's vehicle type as a Firestore filter.
                     * Only restart if the vehicle type has changed to avoid redundant
                     * re-subscriptions on every Firestore user document update.
                     */
                    val vehicleType = user.vehicle?.type
                    if (vehicleType != prevVehicleType || availableRidesJob == null || availableRidesJob?.isActive == false) {
                        startObservingAvailableRides(vehicleType)
                    }
                }
            }
        }
    }

    /**
     * Reads the Firestore-persisted lock expiry and updates [driverLockDaysRemaining]
     * in the UI state. Called on every user load so the state is always fresh —
     * even after reinstall or logout, where SharedPreferences would have been cleared.
     */
    private fun checkDriverLock(userId: String) {
        viewModelScope.launch {
            val lockUntil = authRepository.getDriverLockUntil(userId)
            val now = System.currentTimeMillis()
            val days = if (lockUntil > now) {
                ((lockUntil - now) / (24L * 60L * 60L * 1_000L)).toInt() + 1
            } else {
                0
            }
            _uiState.update { it.copy(driverLockDaysRemaining = days) }
        }
    }

    /**
     * Fix #1 + #3: Subscribe to rides filtered by the driver's vehicle type.
     * By passing vehicleType to getAvailableRides(), Firestore returns only rides
     * matching this driver's vehicle — drivers no longer receive irrelevant requests.
     * This also speeds up the list update since the payload is smaller.
     *
     * The "Vehicle Available?" dialog in DriverScreen is still shown before
     * accepting (confirming the driver is ready), but the list is pre-filtered
     * so only matching rides appear.
     */
    private fun startObservingAvailableRides(vehicleType: VehicleType?) {
        availableRidesJob?.cancel()
        firstRideLoad = true
        previousRideIds = emptySet()

        availableRidesJob = viewModelScope.launch {
            rideRepository.getAvailableRides(vehicleType).collect { rides ->
                val myId = currentUser?.id ?: ""
                // Additional client-side guard: never show the driver their own passenger rides
                val filtered = if (myId.isNotBlank()) rides.filter { it.userId != myId } else rides

                // Detect newly arrived rides for FCM-style local notification
                val currentIds = filtered.map { it.id }.toSet()
                if (!firstRideLoad && _uiState.value.activeRide == null) {
                    val brandNewIds = currentIds - previousRideIds
                    brandNewIds.forEach { newId ->
                        filtered.find { it.id == newId }?.let { newRide ->
                            if (notificationsAllowed()) {
                                notificationHelper.showRideRequestNotification(newRide)
                            }
                        }
                    }
                }
                previousRideIds = currentIds
                firstRideLoad = false

                _uiState.update { it.copy(availableRides = filtered) }
            }
        }
    }

    private fun refreshAndSaveFcmToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                authRepository.saveFcmToken(userId, token)
                Log.d("DriverVM", "FCM token refreshed and saved")
            } catch (e: Exception) {
                Log.e("DriverVM", "FCM token refresh failed", e)
            }
        }
    }

    fun startDriverTracking() {
        if (isTrackingStarted) return
        val user = currentUser ?: return
        if (!user.isDriverMode) return
        startLocationService()
        isTrackingStarted = true
    }

    private fun observeLocation() {
        viewModelScope.launch {
            mapRepository.getUserLocation().collect { loc ->
                _uiState.update { it.copy(currentLocation = loc) }
            }
        }
    }

    private fun startLocationService() {
        try {
            context.startForegroundService(
                Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_START }
            )
        } catch (_: Exception) { }
    }

    private fun stopLocationService() {
        context.startService(
            Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
        )
        isTrackingStarted = false
    }

    private fun notificationsAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationHelper.areNotificationsEnabled()
        }
    }

    private fun observeDriverHistory(driverId: String) {
        driverHistoryJob?.cancel()
        driverHistoryJob = viewModelScope.launch {
            rideRepository.getDriverRideHistory(driverId).collect { rides ->
                val completed = rides.filter { it.status == RideStatus.COMPLETED }
                val inProgress = rides.firstOrNull {
                    it.status == RideStatus.CONFIRMED || it.status == RideStatus.ARRIVING || it.status == RideStatus.ONGOING
                }
                if (inProgress != null && _uiState.value.activeRide == null) {
                    _uiState.update { it.copy(activeRide = inProgress) }
                    startRideObservation(inProgress.id)
                }
                _uiState.update {
                    it.copy(recentTrips = rides.take(5), completedTrips = completed.size, totalEarnings = completed.sumOf { r -> r.fare })
                }
            }
        }
    }

    private fun startRideObservation(rideId: String) {
        rideObservationJob?.cancel()
        rideObservationJob = viewModelScope.launch {
            rideRepository.getRideUpdates(rideId).collect { ride ->
                _uiState.update { it.copy(activeRide = ride) }
                if (ride.status == RideStatus.COMPLETED || ride.status == RideStatus.CANCELLED) {
                    _uiState.update { it.copy(activeRide = null) }
                    rideObservationJob?.cancel()
                }
            }
        }
    }

    fun toggleDriverMode(isEnabled: Boolean) {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            authRepository.toggleDriverMode(userId, isEnabled)
            if (!isEnabled) {
                stopLocationService()
                driverHistoryJob?.cancel(); driverHistoryJob = null
                availableRidesJob?.cancel(); availableRidesJob = null
            }
        }
    }

    /**
     * Fix #2: Logout marks the driver offline in RTDB only.
     * isDriverMode stays TRUE in Firestore so the next login routes back to Driver Dashboard.
     */
    fun goOfflineOnly() {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            authRepository.goOfflineOnly(userId)
            stopLocationService()
            driverHistoryJob?.cancel(); driverHistoryJob = null
            availableRidesJob?.cancel(); availableRidesJob = null
            authRepository.signOut()
        }
    }

    /**
     * Fix #3: acceptRide now uses a Firestore transaction in the repository,
     * ensuring only one driver can claim a ride even under race conditions.
     * Shows a user-friendly error toast if the ride was already taken.
     */
    fun acceptRide(ride: Ride) {
        val driverId = currentUser?.id ?: return
        notificationHelper.cancelRideNotification(ride.id)
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            rideRepository.acceptRide(ride.id, driverId, currentUser?.name ?: "")
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false, activeRide = ride.copy(status = RideStatus.CONFIRMED)) }
                    startRideObservation(ride.id)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false) }
                    // Show toast if ride was already accepted by another driver
                    Toast.makeText(context, "Could not accept: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun updateStatus(status: RideStatus) {
        val ride = _uiState.value.activeRide ?: return
        val driverId = currentUser?.id ?: return
        viewModelScope.launch {
            rideRepository.updateRideStatus(ride.id, status)
            if (status == RideStatus.COMPLETED) {
                authRepository.transferFare(passengerId = ride.userId, driverId = driverId, fare = ride.fare)
                _uiState.update { it.copy(totalEarnings = it.totalEarnings + ride.fare, activeRide = null) }
                rideObservationJob?.cancel()
            } else if (status == RideStatus.CANCELLED) {
                _uiState.update { it.copy(activeRide = null) }
                rideObservationJob?.cancel()
            }
        }
    }
}

data class DriverUiState(
    val user: User? = null,
    val currentLocation: com.example.transportapp.domain.model.LatLng? = null,
    val availableRides: List<Ride> = emptyList(),
    val activeRide: Ride? = null,
    val recentTrips: List<Ride> = emptyList(),
    val completedTrips: Int = 0,
    val totalEarnings: Double = 0.0,
    val isProcessing: Boolean = false,
    /** Days remaining in the Firestore-persisted 15-day driver lock (0 = unlocked). */
    val driverLockDaysRemaining: Int = 0
)
