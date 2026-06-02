package com.example.transportapp.presentation.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.data.session.SessionManager
import com.example.transportapp.domain.model.Driver
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.VehicleType
import com.example.transportapp.ui.components.GlassCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// ─── Screen entry point ───────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDriverDashboard: () -> Unit,
    sessionManager: SessionManager,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh session on every user interaction
    LaunchedEffect(Unit) { sessionManager.refreshActivity() }

    // Map visibility — shown only when user picks a destination OR during active ride (view button)
    var showMapPicker by remember { mutableStateOf(false) }
    var showMapForRide by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("500") }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Error toast
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Trip completed dialog trigger
    LaunchedEffect(uiState.currentRide?.status) {
        if (uiState.currentRide?.status == RideStatus.COMPLETED) {
            showCompletionDialog = true
        }
        // Close map view if ride ends
        if (uiState.currentRide == null) showMapForRide = false
    }

    val permissionState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    // Show the system permission dialog on first launch
    LaunchedEffect(Unit) { permissionState.launchMultiplePermissionRequest() }

    // KEY FIX: When permissions flip to granted (first install, login after long gap),
    // restart the location observer so requestLocationUpdates is called AFTER the OS
    // has recorded the grant. This eliminates "stuck on Getting your location" without
    // requiring the user to restart the app.
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.retryLocation()
        }
    }

    // Support contact helper
    val onContactSupport: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=923094817162")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:03094817162") })
        }
        sessionManager.refreshActivity()
    }

    // ─── MAP PICKER (destination selection) ──────────────────────────────────
    if (showMapPicker) {
        MapPickerView(
            permissionsGranted = permissionState.allPermissionsGranted,
            userLocation = uiState.userLocation,
            onDestinationSelected = { latLng ->
                viewModel.onDestinationSelected(
                    com.example.transportapp.domain.model.LatLng(latLng.latitude, latLng.longitude)
                )
                showMapPicker = false
                sessionManager.refreshActivity()
            },
            onBack = { showMapPicker = false }
        )
        return
    }

    // ─── MAP RIDE VIEW (during active ride) ──────────────────────────────────
    if (showMapForRide && uiState.currentRide != null) {
        ActiveRideMapView(
            ride = uiState.currentRide!!,
            driver = uiState.assignedDriver,
            userLocation = uiState.userLocation,
            permissionsGranted = permissionState.allPermissionsGranted,
            onBack = { showMapForRide = false }
        )
        return
    }

    // ─── MAIN DASHBOARD ──────────────────────────────────────────────────────
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2A4A))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .then(if (isTablet) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // ── TOP BAR ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Hello, ${uiState.user?.name?.split(" ")?.firstOrNull() ?: "Passenger"}!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                uiState.pickupAddress,
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            sessionManager.refreshActivity()
                            onNavigateToProfile()
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.12f))
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                    }
                }

                // ── WALLET CARD ───────────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF1976D2))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Refa Wallet", color = Color.White.copy(0.75f), fontSize = 13.sp)
                                    Text(
                                        "Rs. ${uiState.walletBalance.toInt()}",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    null,
                                    tint = Color.White.copy(0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    sessionManager.refreshActivity()
                                    showTopUpDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.4f))
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Money", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── ACTIVE RIDE BANNER (if ongoing) ──────────────────────────
                AnimatedVisibility(visible = uiState.currentRide != null) {
                    uiState.currentRide?.let { ride ->
                        ActiveRideBanner(
                            ride = ride,
                            driver = uiState.assignedDriver,
                            onViewMap = { showMapForRide = true },
                            onCancel = viewModel::cancelRide,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // ── BOOK A RIDE SECTION (when no active ride) ─────────────────
                AnimatedVisibility(visible = uiState.currentRide == null) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            "Where to?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        // Destination selector card
                        AnimatedVisibility(visible = uiState.destinationLocation == null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sessionManager.refreshActivity()
                                        showMapPicker = true
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Color.White.copy(0.6f))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Tap to set destination on map",
                                        color = Color.White.copy(0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // Destination + ride selection (after destination is set)
                        AnimatedVisibility(
                            visible = uiState.destinationLocation != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                // Destination row
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFEF5350))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Destination", color = Color.White.copy(0.5f), fontSize = 11.sp)
                                            Text(
                                                uiState.destinationAddress.ifBlank { "Loading..." },
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = viewModel::clearDestination) {
                                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.5f))
                                        }
                                    }
                                }

                                // Distance badge
                                if (uiState.distanceKm > 0.0) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(0.08f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "%.1f km away".format(uiState.distanceKm),
                                            color = Color(0xFF64B5F6),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Vehicle type selection
                                if (uiState.vehicleFares.isNotEmpty()) {
                                    Text("Choose Ride", color = Color.White.copy(0.7f), fontSize = 13.sp)
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(VehicleType.entries) { type ->
                                            VehicleCard(
                                                type = type,
                                                fare = uiState.vehicleFares[type],
                                                isSelected = type == uiState.selectedVehicleType,
                                                onClick = { viewModel.onVehicleTypeSelected(type) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                } else {
                                    // Fares still calculating
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Calculating fares...", color = Color.White.copy(0.6f))
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }

                                // Confirm button
                                val locationReady = uiState.userLocation != null
                                Button(
                                    onClick = {
                                        sessionManager.refreshActivity()
                                        viewModel.requestRide()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !uiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (locationReady) Color.White else Color.White.copy(0.3f)
                                    )
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.Black,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        val label = if (!locationReady)
                                            "Getting your GPS..."
                                        else {
                                            val fare = uiState.vehicleFares[uiState.selectedVehicleType]
                                            if (fare != null)
                                                "Confirm ${uiState.selectedVehicleType.name}  •  Rs. ${fare.toInt()}"
                                            else
                                                "Confirm ${uiState.selectedVehicleType.name}"
                                        }
                                        Text(
                                            label,
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── QUICK ACTIONS ─────────────────────────────────────────────
                AnimatedVisibility(visible = uiState.currentRide == null) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Quick Actions", color = Color.White.copy(0.5f), fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            QuickAction(
                                icon = Icons.Default.History,
                                label = "My Trips",
                                modifier = Modifier.weight(1f)
                            ) {
                                sessionManager.refreshActivity(); onNavigateToHistory()
                            }
                            QuickAction(
                                icon = Icons.Default.SupportAgent,
                                label = "Support",
                                modifier = Modifier.weight(1f),
                                onClick = onContactSupport
                            )
                            if (uiState.user?.isDriverMode == true) {
                                QuickAction(
                                    icon = Icons.Default.DirectionsCar,
                                    label = "Driver Mode",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    sessionManager.refreshActivity(); onNavigateToDriverDashboard()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── TOP-UP DIALOG ────────────────────────────────────────────────────────
    if (showTopUpDialog) {
        AlertDialog(
            onDismissRequest = { showTopUpDialog = false },
            title = { Text("Add Money to Wallet", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Current balance: Rs. ${uiState.walletBalance.toInt()}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = topUpAmount,
                        onValueChange = { topUpAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (Rs.)") },
                        prefix = { Text("Rs. ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("200", "500", "1000", "2000")) { preset ->
                            AssistChip(
                                onClick = { topUpAmount = preset },
                                label = { Text("Rs. $preset") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = topUpAmount.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.topUpWallet(amount)
                        showTopUpDialog = false
                        Toast.makeText(context, "Rs. ${amount.toInt()} added to wallet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Add Money") }
            },
            dismissButton = {
                TextButton(onClick = { showTopUpDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─── TRIP COMPLETED DIALOG ────────────────────────────────────────────────
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text("Trip Completed!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Fare deducted: Rs. ${uiState.currentRide?.fare?.toInt() ?: "—"}",
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Remaining balance: Rs. ${uiState.walletBalance.toInt()}",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCompletionDialog = false }) { Text("Done") }
            }
        )
    }
}

// ─── MAP PICKER FULL SCREEN ───────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapPickerView(
    permissionsGranted: Boolean,
    userLocation: com.example.transportapp.domain.model.LatLng?,
    onDestinationSelected: (LatLng) -> Unit,
    onBack: () -> Unit
) {
    var tappedLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(24.8607, 67.0011),
            14f
        )
    }

    // Only centre the camera ONCE when the picker first opens (or when we first get a
    // GPS fix). After that we leave the camera wherever the user has panned — we must
    // NOT animate on every GPS tick or the map will keep jumping back to the user's
    // current position while they are trying to browse and pick a destination.
    var hasInitializedCamera by remember { mutableStateOf(userLocation != null) }
    LaunchedEffect(userLocation) {
        if (userLocation != null && !hasInitializedCamera) {
            hasInitializedCamera = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(userLocation.latitude, userLocation.longitude), 15f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionsGranted),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = permissionsGranted,
                zoomControlsEnabled = false
            ),
            onMapClick = { latLng -> tappedLocation = latLng }
        ) {
            tappedLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
        }

        // Instruction + Confirm row at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (tappedLocation == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, null, tint = Color(0xFF1565C0))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tap anywhere on the map to set your destination",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFE53935))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Destination set! Tap Confirm to proceed.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { tappedLocation?.let { onDestinationSelected(it) } },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Confirm Destination", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ─── ACTIVE RIDE MAP VIEW ─────────────────────────────────────────────────────

@Composable
private fun ActiveRideMapView(
    ride: com.example.transportapp.domain.model.Ride,
    driver: Driver?,
    userLocation: com.example.transportapp.domain.model.LatLng?,
    permissionsGranted: Boolean,
    onBack: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(ride.pickupLocation.latitude, ride.pickupLocation.longitude), 14f
        )
    }

    LaunchedEffect(driver?.location) {
        driver?.location?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionsGranted),
            uiSettings = MapUiSettings(myLocationButtonEnabled = permissionsGranted, zoomControlsEnabled = false)
        ) {
            Marker(
                state = MarkerState(LatLng(ride.pickupLocation.latitude, ride.pickupLocation.longitude)),
                title = "Pickup",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
            Marker(
                state = MarkerState(LatLng(ride.dropLocation.latitude, ride.dropLocation.longitude)),
                title = "Drop-off",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
            driver?.let { d ->
                Marker(
                    state = MarkerState(LatLng(d.location.latitude, d.location.longitude)),
                    title = "Driver: ${d.name}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
        }

        // Mini status panel
        GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(driver?.name ?: "Assigning driver...", fontWeight = FontWeight.Bold)
                    Text(ride.status.name, color = Color.Gray, fontSize = 13.sp)
                }
                Text(
                    "Rs. ${ride.fare.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

// ─── ACTIVE RIDE BANNER (dashboard view) ─────────────────────────────────────

/**
 * Formats a raw phone number string to WhatsApp international format (digits only, no '+').
 * Pakistan local numbers starting with "0" are converted to "92...".
 */
private fun formatPhoneForWhatsApp(raw: String): String {
    val digits = raw.trim().replace(Regex("[\\s\\-().]+"), "")
    return when {
        digits.startsWith("+") -> digits.removePrefix("+")
        digits.startsWith("0") -> "92" + digits.removePrefix("0")
        else -> digits
    }
}

@Composable
private fun ActiveRideBanner(
    ride: com.example.transportapp.domain.model.Ride,
    driver: Driver?,
    onViewMap: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val statusColor = when (ride.status) {
        RideStatus.SEARCHING -> Color(0xFFFFA000)
        RideStatus.CONFIRMED -> Color(0xFF2196F3)
        RideStatus.ARRIVING  -> Color(0xFF4CAF50)
        RideStatus.ONGOING   -> Color(0xFF9C27B0)
        else                 -> Color.Gray
    }
    val statusText = when (ride.status) {
        RideStatus.SEARCHING -> "Searching for a driver..."
        RideStatus.CONFIRMED -> "Driver is on the way!"
        RideStatus.ARRIVING  -> "Driver has arrived!"
        RideStatus.ONGOING   -> "Trip in progress"
        else                 -> ride.status.name
    }

    // Show WhatsApp contact button only when a driver with a phone number is assigned
    val driverPhone = driver?.phoneNumber?.trim() ?: ""
    val showContactDriver = driverPhone.isNotBlank() && ride.status != RideStatus.SEARCHING

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(statusText, color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(
                        driver?.let { "${it.name} • ${it.vehicle?.model ?: "Vehicle"}" }
                            ?: "Assigning driver...",
                        color = Color.White.copy(0.7f),
                        fontSize = 13.sp
                    )
                }
                Text("Rs. ${ride.fare.toInt()}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = Color.White.copy(0.15f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onViewMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.4f))
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View Map")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(0.5f)),
                    enabled = ride.status == RideStatus.SEARCHING
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
            }

            // WhatsApp contact button — visible once a driver is assigned
            if (showContactDriver) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val phone = formatPhoneForWhatsApp(driverPhone)
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phone")
                                }
                            )
                        } catch (_: Exception) {
                            // WhatsApp not installed — fall back to regular call
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$driverPhone")
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Contact Driver on WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

// ─── VEHICLE CARD ─────────────────────────────────────────────────────────────

@Composable
private fun VehicleCard(
    type: VehicleType,
    fare: Double?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color.White else Color.White.copy(0.08f)
    val fg = if (isSelected) Color.Black else Color.White

    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = when (type) {
                    VehicleType.BIKE -> Icons.Default.TwoWheeler
                    VehicleType.RICKSHAW -> Icons.Default.ElectricRickshaw
                    VehicleType.CAR -> Icons.Default.DirectionsCar
                    else -> Icons.Default.Stars
                },
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(type.name, color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                if (fare != null) "Rs. ${fare.toInt()}" else "...",
                fontSize = 11.sp,
                color = fg.copy(0.7f)
            )
        }
    }
}

// ─── QUICK ACTION BUTTON ──────────────────────────────────────────────────────

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White.copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
