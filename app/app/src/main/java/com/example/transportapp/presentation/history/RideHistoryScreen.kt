package com.example.transportapp.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.model.RideStatus
import com.example.transportapp.domain.model.VehicleType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(
    onBack: () -> Unit,
    viewModel: RideHistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    val pastRides = history.filter {
        it.status == RideStatus.COMPLETED || it.status == RideStatus.CANCELLED
    }
    val completedRides = pastRides.filter { it.status == RideStatus.COMPLETED }
    val totalSpent     = completedRides.sumOf { it.fare }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (pastRides.isEmpty()) {
            // ── Empty state ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History, null,
                        modifier = Modifier.size(if (isTablet) 96.dp else 72.dp),
                        tint = Color.Gray.copy(0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No trips yet", fontWeight = FontWeight.Bold, fontSize = if (isTablet) 22.sp else 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Your completed rides will appear here.", color = Color.Gray, fontSize = if (isTablet) 16.sp else 14.sp)
                }
            }
        } else if (isTablet) {
            // ── TABLET: LazyVerticalGrid — 2-column ride cards ───────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .widthIn(max = 900.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary bar — full span
                    item(span = { GridItemSpan(2) }) {
                        SummaryBar(
                            completedCount = completedRides.size,
                            cancelledCount = pastRides.size - completedRides.size,
                            totalSpent = totalSpent,
                            isTablet = true
                        )
                    }

                    // Ride cards — 2 per row on tablet
                    items(pastRides, key = { it.id }) { ride ->
                        RideHistoryCard(ride = ride, isTablet = true)
                    }
                }
            }
        } else {
            // ── PHONE: LazyColumn — single column ────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryBar(
                        completedCount = completedRides.size,
                        cancelledCount = pastRides.size - completedRides.size,
                        totalSpent = totalSpent,
                        isTablet = false
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(pastRides, key = { it.id }) { ride ->
                    RideHistoryCard(ride = ride, isTablet = false)
                }
            }
        }
    }
}

// ─── Summary Bar ───────────────────────────────────────────────────────────────

@Composable
private fun SummaryBar(
    completedCount: Int,
    cancelledCount: Int,
    totalSpent: Double,
    isTablet: Boolean
) {
    val chipPad = if (isTablet) 14.dp else 10.dp
    val iconSize = if (isTablet) 24.dp else 20.dp
    val valueSp = if (isTablet) 17.sp else 15.sp
    val labelSp = if (isTablet) 12.sp else 10.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(Modifier.weight(1f), Icons.Default.CheckCircle, Color(0xFF4CAF50),
            "Completed", "$completedCount", chipPad, iconSize, valueSp, labelSp)
        SummaryChip(Modifier.weight(1f), Icons.Default.AccountBalanceWallet, Color(0xFF2196F3),
            "Total Spent", "Rs. ${totalSpent.toInt()}", chipPad, iconSize, valueSp, labelSp)
        SummaryChip(Modifier.weight(1f), Icons.Default.Cancel, Color(0xFFEF5350),
            "Cancelled", "$cancelledCount", chipPad, iconSize, valueSp, labelSp)
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    chipPad: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    valueSp: androidx.compose.ui.unit.TextUnit,
    labelSp: androidx.compose.ui.unit.TextUnit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(chipPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(iconSize))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = valueSp)
            Text(label, color = Color.Gray, fontSize = labelSp)
        }
    }
}

// ─── Ride History Card ──────────────────────────────────────────────────────────

@Composable
private fun RideHistoryCard(ride: Ride, isTablet: Boolean) {
    val sdf = SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(ride.timestamp))
    val isCompleted = ride.status == RideStatus.COMPLETED

    val statusColor  = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val statusLabel  = if (isCompleted) "Completed" else "Cancelled"
    val statusBg     = statusColor.copy(alpha = 0.12f)

    val vehicleIcon: ImageVector = when (ride.vehicleType) {
        VehicleType.BIKE     -> Icons.Default.TwoWheeler
        VehicleType.RICKSHAW -> Icons.Default.ElectricRickshaw
        else                 -> Icons.Default.DirectionsCar
    }
    val vehicleLabel = ride.vehicleType.name.lowercase().replaceFirstChar { it.uppercase() }

    val avatarSize  = if (isTablet) 48.dp else 42.dp
    val fareSp      = if (isTablet) 19.sp  else 17.sp
    val titleSp     = if (isTablet) 17.sp  else 15.sp
    val captionSp   = if (isTablet) 13.sp  else 11.sp
    val routeSp     = if (isTablet) 14.sp  else 13.sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp)) {

            // ── Top row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(vehicleIcon, null,
                        modifier = Modifier.size(avatarSize * 0.5f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(vehicleLabel, fontWeight = FontWeight.Bold, fontSize = titleSp)
                    Text(dateStr, fontSize = captionSp, color = Color.Gray)
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = captionSp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.Gray.copy(0.12f))
            Spacer(Modifier.height(14.dp))

            // ── Route ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Box(Modifier.width(2.dp).height(22.dp).background(Color.Gray.copy(0.3f)))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFEF5350)))
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ride.pickupAddress.ifBlank { "Pickup Location" },
                        fontSize = routeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(ride.dropAddress.ifBlank { "Destination" },
                        fontSize = routeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.Gray.copy(0.12f))
            Spacer(Modifier.height(12.dp))

            // ── Bottom row ───────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = ride.driverName.ifBlank {
                            if (ride.driverId != null) "Driver assigned" else "No driver"
                        },
                        fontSize = captionSp, color = Color.Gray,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Rs. ${ride.fare.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fareSp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
