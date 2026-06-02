package com.example.transportapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.notification.NotificationHelper
import com.example.transportapp.domain.model.*
import com.example.transportapp.domain.repository.AuthRepository
import com.example.transportapp.domain.repository.MapRepository
import com.example.transportapp.domain.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mapRepository: MapRepository,
    private val rideRepository: RideRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""
    private var rideUpdateJob: Job? = null
    private var driverTrackingJob: Job? = null
    private var activeRideHistoryJob: Job? = null

    // Tracks the last seen ride status so we only notify on genuine transitions
    private var lastNotifiedStatus: RideStatus? = null

    // Separate job handle so we can cancel + restart location without touching other observers
    private var locationJob: Job? = null

    init {
        observeUser()
        // Location observation starts here but may silently fail if permissions not yet granted.
        // HomeScreen calls retryLocation() the moment permissions are granted, which restarts it.
        observeUserLocation()
    }

    /**
     * Called by HomeScreen when location permissions are granted (or re-granted).
     * Cancels any existing location coroutine and starts a fresh one so that
     * FusedLocationProviderClient.requestLocationUpdates is called AFTER the OS has
     * recorded the permission grant — eliminating the "stuck on Getting your location"
     * bug that happens on first install.
     */
    fun retryLocation() {
        locationJob?.cancel()
        observeUserLocation()
    }

    // ── User ─────────────────────────────────────────────────────────────────

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                if (user == null) {
                    currentUserId = ""
                    activeRideHistoryJob?.cancel()
                    rideUpdateJob?.cancel()
                    driverTrackingJob?.cancel()
                    lastNotifiedStatus = null
                    _uiState.update { HomeUiState() }
                    return@collect
                }
                currentUserId = user.id
                _uiState.update { it.copy(user = user, walletBalance = user.walletBalance) }
                observeActiveRideHistory(user.id)
            }
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private fun observeUserLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            mapRepository.getUserLocation().collect { loc ->
                val wasNull = _uiState.value.userLocation == null
                _uiState.update { it.copy(userLocation = loc) }

                val address = mapRepository.getAddressFromLocation(loc)
                _uiState.update { it.copy(pickupAddress = address ?: "Current Location") }

                val dest = _uiState.value.destinationLocation
                if (dest != null && _uiState.value.currentRide == null) {
                    if (wasNull || _uiState.value.vehicleFares.isEmpty()) {
                        computeAndSetFares(loc, dest)
                    }
                    refreshRoute(loc, dest)
                }
            }
        }
    }

    // ── Map interaction ───────────────────────────────────────────────────────

    fun onDestinationSelected(latLng: LatLng) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    destinationLocation = latLng,
                    destinationAddress = "Loading address...",
                    vehicleFares = emptyMap()
                )
            }
            val address = mapRepository.getAddressFromLocation(latLng)
            _uiState.update { it.copy(destinationAddress = address ?: "Selected Destination") }

            val origin = _uiState.value.userLocation
            if (origin != null) {
                computeAndSetFares(origin, latLng)
                refreshRoute(origin, latLng)
            }
        }
    }

    fun clearDestination() {
        _uiState.update {
            it.copy(
                destinationLocation = null,
                destinationAddress = "",
                routePoints = emptyList(),
                vehicleFares = emptyMap(),
                distanceKm = 0.0
            )
        }
    }

    private fun computeAndSetFares(origin: LatLng, destination: LatLng) {
        val distanceKm = haversineKm(origin, destination)
        val fares = VehicleType.entries.associateWith { fareForDistance(distanceKm, it) }
        val straight = listOf(origin, destination)
        _uiState.update { state ->
            state.copy(
                vehicleFares = fares,
                distanceKm = distanceKm,
                routePoints = if (state.routePoints.isEmpty()) straight else state.routePoints
            )
        }
    }

    private fun refreshRoute(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            val route = mapRepository.getDirections(origin, destination)
            if (route.isNotEmpty()) {
                val dist = routeDistanceKm(route)
                val fares = VehicleType.entries.associateWith { fareForDistance(dist, it) }
                _uiState.update { it.copy(routePoints = route, vehicleFares = fares, distanceKm = dist) }
            }
        }
    }

    // ── Vehicle selection ─────────────────────────────────────────────────────

    fun onVehicleTypeSelected(type: VehicleType) {
        _uiState.update { it.copy(selectedVehicleType = type) }
    }

    // ── Ride request ─────────────────────────────────────────────────────────

    fun requestRide() {
        val state = _uiState.value
        val destination = state.destinationLocation ?: run {
            _uiState.update { it.copy(errorMessage = "Please select a destination first") }
            return
        }
        val riderId = currentUserId.ifBlank { state.user?.id ?: "" }.also {
            if (it.isBlank()) {
                _uiState.update { s -> s.copy(errorMessage = "Profile still loading, please wait a moment") }
                return
            }
        }
        if (state.currentRideId != null) {
            _uiState.update { it.copy(errorMessage = "You already have an active ride") }
            return
        }
        val origin = state.userLocation ?: run {
            _uiState.update { it.copy(errorMessage = "Getting your GPS location, please wait...") }
            return
        }

        val fare = state.vehicleFares[state.selectedVehicleType]
            ?: fareForDistance(haversineKm(origin, destination), state.selectedVehicleType)

        if (state.walletBalance < fare) {
            _uiState.update {
                it.copy(errorMessage = "Low balance: Rs.${fare.toInt()} needed. Top up from Profile.")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        lastNotifiedStatus = null  // reset notification tracker for new ride

        viewModelScope.launch {
            val ride = Ride(
                userId = riderId,
                userName = state.user?.name ?: "Passenger",
                userPhone = state.user?.phoneNumber ?: "",
                pickupLocation = origin,
                pickupAddress = state.pickupAddress.ifBlank { "Pickup" },
                dropLocation = destination,
                dropAddress = state.destinationAddress.ifBlank { "Destination" },
                vehicleType = state.selectedVehicleType,
                fare = fare,
                status = RideStatus.SEARCHING,
                timestamp = System.currentTimeMillis(),
                passengerLocation = origin
            )

            rideRepository.requestRide(ride).onSuccess { rideId ->
                _uiState.update { it.copy(currentRideId = rideId, currentRide = ride.copy(id = rideId), isLoading = false) }
                observeRideUpdates(rideId)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Booking failed. Check your internet.") }
            }
        }
    }

    // ── Wallet top-up ─────────────────────────────────────────────────────────

    fun topUpWallet(amount: Double) {
        val userId = currentUserId.ifBlank { return }
        if (amount <= 0) return
        viewModelScope.launch {
            val newBalance = _uiState.value.walletBalance + amount
            authRepository.updateWalletBalance(userId, newBalance)
            _uiState.update { it.copy(walletBalance = newBalance) }
        }
    }

    // ── Cancel ride ───────────────────────────────────────────────────────────

    fun cancelRide() {
        val rideId = _uiState.value.currentRideId ?: return
        lastNotifiedStatus = null

        // ── Optimistic UI update — happens instantly, no network wait ──────────
        // Cancel all observers first so Firestore can't push the CANCELLED status
        // back onto the screen after we've already cleared it.
        rideUpdateJob?.cancel()
        driverTrackingJob?.cancel()
        _uiState.update {
            it.copy(
                currentRide = null, currentRideId = null, assignedDriver = null,
                destinationLocation = null, destinationAddress = "",
                routePoints = emptyList(), vehicleFares = emptyMap(),
                distanceKm = 0.0
            )
        }

        // Fire-and-forget: write CANCELLED to Firestore in the background.
        // The user sees the home screen immediately regardless of network speed.
        viewModelScope.launch {
            runCatching { rideRepository.cancelRide(rideId) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Ride observation ──────────────────────────────────────────────────────

    private fun observeActiveRideHistory(userId: String) {
        activeRideHistoryJob?.cancel()
        activeRideHistoryJob = viewModelScope.launch {
            rideRepository.getRideHistory(userId).collect { rides ->
                val activeRide = rides
                    .filter { it.status != RideStatus.COMPLETED && it.status != RideStatus.CANCELLED }
                    .maxByOrNull { it.timestamp }

                if (activeRide != null) {
                    val currentId = _uiState.value.currentRideId
                    if (currentId != activeRide.id) {
                        _uiState.update {
                            it.copy(
                                currentRideId = activeRide.id,
                                currentRide = activeRide,
                                destinationLocation = activeRide.dropLocation,
                                destinationAddress = activeRide.dropAddress
                            )
                        }
                        observeRideUpdates(activeRide.id)
                    }
                }
            }
        }
    }

    private fun observeRideUpdates(rideId: String) {
        rideUpdateJob?.cancel()
        rideUpdateJob = viewModelScope.launch {

            /**
             * FIX — repeated-notification + stuck-card bugs:
             *
             * Root cause: After ride.status == COMPLETED, the code did delay(4000)
             * then cleared the UI state (currentRide = null) and reset lastNotifiedStatus
             * to null. But rideUpdateJob was NOT cancelled, so the Firestore snapshot
             * listener kept running. The next Firestore emission for the same (still-COMPLETED)
             * document then saw:
             *   • previousStatus = null  (state was cleared)
             *   • lastNotifiedStatus = null  (explicitly reset)
             * → condition `newStatus != previousStatus && newStatus != lastNotifiedStatus`
             *   evaluated TRUE → notification fired again → delay(4000) started AGAIN →
             *   state was restored (currentRide = ride) → card reappeared → loop ∞
             *
             * Fix: a single `completionHandled` boolean declared OUTSIDE the collect lambda
             * (but inside the launch). It is set to TRUE before the delay. Every subsequent
             * Firestore emission returns at the very first line, preventing:
             *   1. The notification firing a second time.
             *   2. The state being restored after clearing.
             *   3. The indefinite delay loop.
             *
             * Similarly for CANCELLED: state is cleared then `return@collect` terminates
             * the current emission. The job itself is cancelled via rideUpdateJob?.cancel()
             * which propagates at the next suspension point, stopping the listener entirely.
             */
            var completionHandled = false

            rideRepository.getRideUpdates(rideId).collect { ride ->

                // ── Completion guard — MUST be the very first check ─────────────
                // Once completionHandled = true, no further Firestore re-emissions
                // for this ride should touch the UI or trigger notifications.
                if (completionHandled) return@collect

                val previousStatus = _uiState.value.currentRide?.status
                _uiState.update { it.copy(currentRide = ride) }

                // ── Passenger push notification on status change ──────────────
                // Fire a local notification whenever the ride status advances to a
                // meaningful milestone. Guard: only notify on genuine transitions
                // (not on every re-emit of the same status).
                val newStatus = ride.status
                if (newStatus != previousStatus && newStatus != lastNotifiedStatus) {
                    when (newStatus) {
                        RideStatus.CONFIRMED,
                        RideStatus.ARRIVING,
                        RideStatus.ONGOING,
                        RideStatus.COMPLETED -> {
                            if (notificationHelper.areNotificationsEnabled()) {
                                notificationHelper.showPassengerRideNotification(
                                    rideId = rideId,
                                    driverName = ride.driverName,
                                    status = newStatus
                                )
                            }
                            lastNotifiedStatus = newStatus
                        }
                        else -> { /* SEARCHING / CANCELLED — no notification needed */ }
                    }
                }

                val driverId = ride.driverId
                if (driverId != null && _uiState.value.assignedDriver == null) {
                    startDriverTracking(driverId)
                }

                // ── Cancellation: instant UI reset, stop observer ──────────────
                if (ride.status == RideStatus.CANCELLED) {
                    driverTrackingJob?.cancel()
                    lastNotifiedStatus = null
                    _uiState.update {
                        it.copy(
                            currentRide = null, currentRideId = null, assignedDriver = null,
                            destinationLocation = null, destinationAddress = "",
                            routePoints = emptyList(), vehicleFares = emptyMap(),
                            distanceKm = 0.0
                        )
                    }
                    // Cancel the ride update listener. CancellationException propagates at
                    // the next suspension point (the flow's await), which is correct.
                    rideUpdateJob?.cancel()
                    return@collect
                }

                // ── Completion: show card briefly, then reset to home screen ───
                if (ride.status == RideStatus.COMPLETED) {
                    // Set the flag BEFORE delay() so that any Firestore re-emission
                    // during the 4-second wait hits `if (completionHandled) return@collect`
                    // at the top of the next collect invocation and does nothing.
                    completionHandled = true
                    driverTrackingJob?.cancel()
                    delay(4000)
                    lastNotifiedStatus = null
                    _uiState.update {
                        it.copy(
                            currentRide = null, currentRideId = null, assignedDriver = null,
                            destinationLocation = null, destinationAddress = "",
                            routePoints = emptyList(), vehicleFares = emptyMap(),
                            distanceKm = 0.0
                        )
                    }
                    // Job stays alive but every future emission returns early — no harm.
                    return@collect
                }

                val loc = _uiState.value.userLocation
                if (loc != null && ride.status == RideStatus.ONGOING) {
                    rideRepository.updateRideLocation(rideId, loc)
                }
            }
        }
    }

    private fun startDriverTracking(driverId: String) {
        driverTrackingJob?.cancel()
        driverTrackingJob = viewModelScope.launch {
            mapRepository.getDriver(driverId).collect { driver ->
                _uiState.update { it.copy(assignedDriver = driver) }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun haversineKm(a: LatLng, b: LatLng): Double {
        val R = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2); val sinLon = sin(dLon / 2)
        val c = 2 * atan2(
            sqrt(sinLat * sinLat + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinLon * sinLon),
            sqrt(1 - sinLat * sinLat - cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinLon * sinLon)
        )
        return R * c
    }

    private fun routeDistanceKm(pts: List<LatLng>) =
        pts.zipWithNext { a, b -> haversineKm(a, b) }.sum()

    private fun fareForDistance(km: Double, type: VehicleType): Double {
        val base = when (type) { VehicleType.BIKE -> 120.0; VehicleType.RICKSHAW -> 200.0; VehicleType.CAR -> 350.0; else -> 650.0 }
        val perKm = when (type) { VehicleType.BIKE -> 25.0; VehicleType.RICKSHAW -> 40.0; VehicleType.CAR -> 70.0; else -> 130.0 }
        val distance = km.coerceAtLeast(1.0)
        return (base + distance * perKm).let { Math.round(it / 10.0) * 10.0 }
    }
}

data class HomeUiState(
    val user: User? = null,
    val userLocation: LatLng? = null,
    val pickupAddress: String = "Getting your location...",
    val destinationLocation: LatLng? = null,
    val destinationAddress: String = "",
    val distanceKm: Double = 0.0,
    val routePoints: List<LatLng> = emptyList(),
    val assignedDriver: Driver? = null,
    val selectedVehicleType: VehicleType = VehicleType.CAR,
    val vehicleFares: Map<VehicleType, Double> = emptyMap(),
    val currentRideId: String? = null,
    val currentRide: Ride? = null,
    val walletBalance: Double = 0.0,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
