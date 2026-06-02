package com.example.transportapp.presentation.driver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.data.session.SessionManager
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.ui.components.GlassCard
import com.example.transportapp.ui.theme.Accent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DriverScreen(
    onSwitchToPassenger: () -> Unit,
    onLogout: () -> Unit,
    sessionManager: SessionManager,
    viewModel: DriverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { sessionManager.refreshActivity() }

    var showRideMap        by remember { mutableStateOf(false) }
    var pendingRideRequest by remember { mutableStateOf<Ride?>(null) }
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showLockDialog     by remember { mutableStateOf(false) }

    // ── Permissions ──────────────────────────────────────────────────────────
    val locationPerms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val notifPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberMultiplePermissionsState(listOf(Manifest.permission.POST_NOTIFICATIONS))
    else null

    LaunchedEffect(Unit) {
        locationPerms.launchMultiplePermissionRequest()
        notifPerms?.launchMultiplePermissionRequest()
    }
    LaunchedEffect(locationPerms.allPermissionsGranted) {
        if (locationPerms.allPermissionsGranted) viewModel.startDriverTracking()
    }
    LaunchedEffect(uiState.activeRide) {
        if (uiState.activeRide == null) showRideMap = false
    }

    val SUPPORT_NUMBER = "923094817162"
    val onContactSupport: () -> Unit = {
        sessionManager.refreshActivity()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$SUPPORT_NUMBER")
            })
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:03094817162") })
        }
    }

    // ── Fix #3: Vehicle Availability Confirmation Dialog ─────────────────────
    pendingRideRequest?.let { pendingRide ->
        AlertDialog(
            onDismissRequest = { pendingRideRequest = null },
            containerColor = Color(0xFF0F1E3D),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF2979FF), Color(0xFF1565C0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (pendingRide.vehicleType.name) {
                            "BIKE"    -> Icons.Default.TwoWheeler
                            "RICKSHAW"-> Icons.Default.ElectricRickshaw
                            "PREMIUM" -> Icons.Default.Stars
                            else      -> Icons.Default.DirectionsCar
                        },
                        null, tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text("Vehicle Available?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Passenger requested a ${pendingRide.vehicleType.name}.",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFB0BEC5)
                    )
                    Spacer(Modifier.height(14.dp))
                    // Ride detail card
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF152040)).padding(14.dp)
                    ) {
                        RideDetailRow(Icons.Default.Person, "Passenger", pendingRide.userName)
                        Spacer(Modifier.height(6.dp))
                        RideDetailRow(Icons.Default.RadioButtonChecked, "Pickup", pendingRide.pickupAddress)
                        Spacer(Modifier.height(6.dp))
                        RideDetailRow(Icons.Default.LocationOn, "Drop-off", pendingRide.dropAddress)
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(0.1f))
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fare", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                            Text("Rs. ${pendingRide.fare.toInt()}",
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFFFC107))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Is your ${pendingRide.vehicleType.name} available right now?",
                        fontSize = 13.sp, color = Color(0xFFB0BEC5)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ride = pendingRide
                        pendingRideRequest = null
                        viewModel.acceptRide(ride)
                        sessionManager.refreshActivity()
                        Toast.makeText(context, "Ride accepted! Head to pickup.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Yes, Available & Accept", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRideRequest = null },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("No, Skip This Ride", color = Color(0xFFB0BEC5), fontSize = 14.sp)
                }
            }
        )
    }

    // ── Logout Confirmation Dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF0F1E3D),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFD32F2F).copy(0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFEF5350),
                        modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Logout?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "You will be marked offline but your Driver Mode stays active.\n\n" +
                    "Next time you login you will come back directly to the Driver Dashboard.",
                    fontSize = 14.sp, color = Color(0xFFB0BEC5), lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.goOfflineOnly()
                        sessionManager.clearSession()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Logout", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Stay Online", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── 15-day lock dialog ────────────────────────────────────────────────────
    if (showLockDialog) {
        val daysLeft = uiState.driverLockDaysRemaining
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            containerColor = Color(0xFF0F1E3D),
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFF57C00).copy(0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFFFA726),
                        modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Mode Switch Locked", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "You cannot switch to Passenger Mode for $daysLeft more day${if (daysLeft != 1) "s" else ""}.\n\n" +
                    "This 15-day lock is part of the Refa Driver Policy you accepted. " +
                    "After the lock expires you may switch freely.",
                    fontSize = 14.sp, color = Color(0xFFB0BEC5), lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showLockDialog = false }, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Understood")
                }
            }
        )
    }

    // ── Notification banner flag ──────────────────────────────────────────────
    val showNotifBanner = notifPerms != null && !notifPerms.allPermissionsGranted

    // ─── RIDE MAP FULLSCREEN ─────────────────────────────────────────────────
    if (showRideMap && uiState.activeRide != null) {
        val ride = uiState.activeRide!!
        val camState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(ride.pickupLocation.latitude, ride.pickupLocation.longitude), 14f
            )
        }
        LaunchedEffect(uiState.currentLocation) {
            uiState.currentLocation?.let {
                camState.animate(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camState,
                properties = MapProperties(isMyLocationEnabled = locationPerms.allPermissionsGranted),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
            ) {
                Marker(
                    state = MarkerState(LatLng(ride.pickupLocation.latitude, ride.pickupLocation.longitude)),
                    title = "Pickup",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                ride.passengerLocation?.let {
                    Marker(
                        state = MarkerState(LatLng(it.latitude, it.longitude)),
                        title = "${ride.userName} (Live)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                Marker(
                    state = MarkerState(LatLng(ride.dropLocation.latitude, ride.dropLocation.longitude)),
                    title = "Drop-off",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
            IconButton(
                onClick = { showRideMap = false },
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)
                    .size(44.dp).clip(CircleShape).background(Color.White)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black) }
            GlassCard(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) {
                ActiveTripPanel(
                    ride = ride,
                    onUpdateStatus = { viewModel.updateStatus(it); sessionManager.refreshActivity() }
                )
            }
        }
        return
    }

    // ─── MAIN DRIVER DASHBOARD ───────────────────────────────────────────────
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    Scaffold(
        containerColor = if (isTablet) Color(0xFFECF0F8) else Color(0xFFF4F7FC),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Text("Driver Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                            color = Color.White)
                        Text(
                            if (uiState.currentLocation != null) "● Online — GPS active" else "○ Acquiring GPS...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.currentLocation != null) Color(0xFF4CAF50) else Color(0xFF90A4AE)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onContactSupport) {
                        Icon(Icons.Default.SupportAgent, "Support", tint = Color.White)
                    }
                    TextButton(onClick = {
                        if (uiState.driverLockDaysRemaining > 0) showLockDialog = true
                        else { viewModel.toggleDriverMode(false); onSwitchToPassenger() }
                    }) {
                        Text("Passenger", color = Color(0xFF64B5F6), fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    // ── Logout button ──────────────────────────────────────
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color(0xFFEF5350))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onContactSupport,
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(20.dp))
                    Text("SOS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        LazyColumn(
            modifier = Modifier
                .then(if (isTablet) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DriverStatsCard(
                    completedTrips = uiState.completedTrips,
                    totalEarnings = uiState.totalEarnings,
                    driverName = uiState.user?.name ?: "Driver"
                )
            }

            // Notification permission banner
            if (showNotifBanner) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFF1565C0),
                                modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Enable Notifications", fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0), fontSize = 13.sp)
                                Text("Get real-time alerts for new ride requests",
                                    fontSize = 12.sp, color = Color.DarkGray)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { notifPerms?.launchMultiplePermissionRequest() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp)) {
                                Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Location permission prompt
            if (!locationPerms.allPermissionsGranted) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOff, null, tint = Color(0xFFF57C00),
                                modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Location needed", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                                Text("Grant permission to receive ride requests", fontSize = 13.sp)
                            }
                            Button(onClick = { locationPerms.launchMultiplePermissionRequest() },
                                shape = RoundedCornerShape(10.dp)) { Text("Grant") }
                        }
                    }
                }
            }

            // Active ride panel
            if (uiState.activeRide != null) {
                item {
                    ActiveRideCard(
                        ride = uiState.activeRide!!,
                        onUpdateStatus = { viewModel.updateStatus(it); sessionManager.refreshActivity() },
                        onViewMap = { showRideMap = true }
                    )
                }
            }

            // Available ride requests
            item {
                Text(
                    if (uiState.activeRide == null)
                        "Nearby Requests (${uiState.availableRides.size})"
                    else
                        "Other Requests (paused while on a ride)",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E)
                )
            }

            if (uiState.activeRide == null) {
                if (uiState.availableRides.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(40.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Accent, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Waiting for nearby passengers...", color = Color.Gray,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(uiState.availableRides) { ride ->
                        RideRequestCard(ride = ride, onAccept = { pendingRideRequest = ride })
                    }
                }
            }

            if (uiState.recentTrips.isNotEmpty()) {
                item { Text("Recent Trips", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                items(uiState.recentTrips.take(5)) { ride ->
                    Card(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ride.dropAddress, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(ride.status.name, fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${ride.fare.toInt()}", fontWeight = FontWeight.ExtraBold,
                                    color = Accent, fontSize = 16.sp)
                                if (ride.status == RideStatus.COMPLETED) {
                                    Text("Credited", fontSize = 10.sp, color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        if (uiState.isProcessing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)),
                contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1E3D))) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(12.dp))
                        Text("Accepting ride...", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        } // closes Box centering wrapper
    }
}

// ─── Active ride card (in dashboard list) ─────────────────────────────────────

@Composable
private fun ActiveRideCard(ride: Ride, onUpdateStatus: (RideStatus) -> Unit, onViewMap: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Active Ride", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                StatusBadge(ride.status)
            }
            Spacer(Modifier.height(12.dp))
            TripDetailRow(Icons.Default.Person, "Passenger", ride.userName, Color(0xFFB0BEC5))
            TripDetailRow(Icons.Default.RadioButtonChecked, "Pickup", ride.pickupAddress, Color(0xFFB0BEC5))
            TripDetailRow(Icons.Default.LocationOn, "Drop-off", ride.dropAddress, Color(0xFFB0BEC5))
            TripDetailRow(Icons.Default.DirectionsCar, "Vehicle", ride.vehicleType.name, Color(0xFFB0BEC5))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(0.1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fare", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                Text("Rs. ${ride.fare.toInt()}", fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp, color = Color(0xFFFFC107))
            }
            Spacer(Modifier.height(14.dp))
            val (btnText, nextStatus, btnColor) = when (ride.status) {
                RideStatus.CONFIRMED -> Triple("I Have Arrived", RideStatus.ARRIVING, Color(0xFF1565C0))
                RideStatus.ARRIVING  -> Triple("Start Trip", RideStatus.ONGOING, Color(0xFF2E7D32))
                RideStatus.ONGOING   -> Triple("Complete Trip & Collect Fare", RideStatus.COMPLETED, Color(0xFFFFC107))
                else                 -> Triple("Waiting...", ride.status, Color.Gray)
            }
            if (ride.status != RideStatus.SEARCHING) {
                Button(
                    onClick = { onUpdateStatus(nextStatus) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor)
                ) {
                    Text(btnText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                        color = if (nextStatus == RideStatus.COMPLETED) Color.Black else Color.White)
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onViewMap,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f))
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("View Route on Map", color = Color.White)
            }
        }
    }
}

// ─── Request card shown while no active ride ──────────────────────────────────

@Composable
private fun RideRequestCard(ride: Ride, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape)
                        .background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center) {
                        Icon(
                            when (ride.vehicleType.name) {
                                "BIKE"    -> Icons.Default.TwoWheeler
                                "RICKSHAW"-> Icons.Default.ElectricRickshaw
                                "PREMIUM" -> Icons.Default.Stars
                                else      -> Icons.Default.DirectionsCar
                            },
                            null, tint = Accent, modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(ride.vehicleType.name, fontWeight = FontWeight.ExtraBold,
                        color = Accent, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Rs. ${ride.fare.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Fare", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(ride.userName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RadioButtonChecked, null, tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(ride.pickupAddress, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = Color.DarkGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFEF5350),
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(ride.dropAddress, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = Color.DarkGray)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B2A))
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Accept Request", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DriverStatsCard(completedTrips: Int, totalEarnings: Double, driverName: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))))
            .padding(20.dp)) {
            Column {
                Text("Welcome, $driverName", color = Color.White.copy(0.8f), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("You are online and ready", color = Color.White.copy(0.5f), fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatItem(Modifier.weight(1f), "Trips Done", completedTrips.toString(), Icons.Default.CheckCircle)
                    StatItem(Modifier.weight(1f), "Wallet Earned", "Rs. ${totalEarnings.toInt()}", Icons.Default.AccountBalanceWallet)
                }
            }
        }
    }
}

@Composable
private fun StatItem(modifier: Modifier, label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(0.15f)).padding(12.dp)) {
        Icon(icon, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun TripDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, labelColor: Color = Color.Gray
) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = labelColor)
            Text(value, fontWeight = FontWeight.Medium, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = Color.White)
        }
    }
}

@Composable
private fun RideDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", fontSize = 12.sp, color = Color(0xFF90A4AE))
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusBadge(status: RideStatus) {
    val (bg, text) = when (status) {
        RideStatus.SEARCHING -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00))
        RideStatus.CONFIRMED -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        RideStatus.ARRIVING  -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        RideStatus.ONGOING   -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        else                 -> Pair(Color(0xFFEEEEEE), Color.Gray)
    }
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(status.name, color = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActiveTripPanel(ride: Ride, onUpdateStatus: (RideStatus) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Active Ride: ${ride.userName}", fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("From: ${ride.pickupAddress}", fontSize = 13.sp, color = Color(0xFFB0BEC5),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("To: ${ride.dropAddress}", fontSize = 13.sp, color = Color(0xFFB0BEC5),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Fare: Rs. ${ride.fare.toInt()}", fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp, color = Color(0xFFFFC107))
        Spacer(Modifier.height(12.dp))
        val (btnText, nextStatus, color) = when (ride.status) {
            RideStatus.CONFIRMED -> Triple("I Have Arrived", RideStatus.ARRIVING, Color(0xFF1565C0))
            RideStatus.ARRIVING  -> Triple("Start Trip", RideStatus.ONGOING, Color(0xFF2E7D32))
            RideStatus.ONGOING   -> Triple("Complete Trip", RideStatus.COMPLETED, Color(0xFFFFC107))
            else                 -> Triple("Waiting...", ride.status, Color.Gray)
        }
        if (ride.status != RideStatus.SEARCHING) {
            Button(onClick = { onUpdateStatus(nextStatus) }, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text(btnText, fontWeight = FontWeight.Bold,
                    color = if (nextStatus == RideStatus.COMPLETED) Color.Black else Color.White)
            }
        }
    }
}
