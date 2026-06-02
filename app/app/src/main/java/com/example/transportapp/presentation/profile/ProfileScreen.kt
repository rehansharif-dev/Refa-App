package com.example.transportapp.presentation.profile

import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.transportapp.domain.model.Vehicle
import com.example.transportapp.domain.model.VehicleType
import com.example.transportapp.ui.theme.Accent

// ── Palette (all within screen scope) ─────────────────────────────────────────
private val NavyDeep    = Color(0xFF040D21)
private val NavyMid     = Color(0xFF0A1A3A)
private val CardBg      = Color(0xFF0F1E3D)
private val BlueVibrant = Color(0xFF2979FF)
private val TextWhite   = Color.White
private val TextSoft    = Color(0xFFB0BEC5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDriverDashboard: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user            by viewModel.user.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val error           by viewModel.error.collectAsState()
    val successMessage  by viewModel.successMessage.collectAsState()
    val context         = LocalContext.current
    val isTablet        = LocalConfiguration.current.screenWidthDp >= 600
    val hPad            = if (isTablet) 40.dp else 20.dp

    var showPhotoMenu         by remember { mutableStateOf(false) }
    var showFullScreenPhoto   by remember { mutableStateOf(false) }
    var showLogoutDialog      by remember { mutableStateOf(false) }
    var showRemovePhotoDialog by remember { mutableStateOf(false) }
    var showAddFundsDialog    by remember { mutableStateOf(false) }
    var showVehicleDialog     by remember { mutableStateOf(false) }
    var showEditNameDialog    by remember { mutableStateOf(false) }
    var topUpAmount           by remember { mutableStateOf("500") }
    var vehicleModel          by remember { mutableStateOf("") }
    var vehiclePlate          by remember { mutableStateOf("") }
    var selectedType          by remember { mutableStateOf(VehicleType.CAR) }
    var editedName            by remember { mutableStateOf("") }
    var driverPhone           by remember { mutableStateOf("") }
    var driverAddress         by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadProfilePicture(it) } }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    // Resolve profile picture — Base64 data URI or URL string
    val picModel: Any? = remember(user?.profilePicture) {
        val pic = user?.profilePicture ?: return@remember null
        if (pic.isBlank()) return@remember null
        if (pic.startsWith("data:")) {
            try { Base64.decode(pic.substringAfter("base64,"), Base64.NO_WRAP) } catch (_: Exception) { null }
        } else pic
    }
    val hasPicture = picModel != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = TextWhite,
                    navigationIconContentColor = TextWhite
                )
            )
        },
        containerColor = NavyDeep
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Background gradient ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(NavyDeep, NavyMid, Color(0xFF0D1B2A)))
                    )
            )

            // ── Main scrollable column — adaptive for phone + tablet ────────
            Column(
                modifier = Modifier
                    .then(if (isTablet) Modifier.widthIn(max = 680.dp) else Modifier.fillMaxWidth())
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = hPad, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Profile Picture ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF1E3A6E), NavyMid))
                        )
                        .border(2.dp, BlueVibrant.copy(0.5f), CircleShape)
                        .clickable { showPhotoMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPicture) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(picModel).crossfade(true).build(),
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, Modifier.size(52.dp), TextSoft)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Name (tap to edit) ───────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editedName = user?.name ?: ""
                        showEditNameDialog = true
                    }
                ) {
                    Text(
                        text = user?.name ?: "User",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Edit, null, tint = TextSoft, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(user?.email ?: "", color = TextSoft, fontSize = 13.sp)

                Spacer(Modifier.height(20.dp))

                // ── Driver Mode Toggle ───────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (user?.isDriverMode == true)
                                            BlueVibrant.copy(0.2f)
                                        else Color.White.copy(0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (user?.isDriverMode == true)
                                        Icons.Default.DirectionsCar
                                    else Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = if (user?.isDriverMode == true) BlueVibrant else TextSoft,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (user?.isDriverMode == true)
                                        "Driver Mode ACTIVE"
                                    else "Passenger Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 14.sp
                                )
                                Text("Switch to start earning", fontSize = 11.sp, color = TextSoft)
                            }
                        }
                        Switch(
                            checked = user?.isDriverMode ?: false,
                            onCheckedChange = { enabled ->
                                if (enabled && user?.vehicle == null) {
                                    vehicleModel = ""; vehiclePlate = ""
                                    driverPhone = ""; driverAddress = ""
                                    selectedType = VehicleType.CAR
                                    showVehicleDialog = true
                                } else {
                                    viewModel.toggleDriverMode(enabled) {
                                        if (enabled) onNavigateToDriverDashboard()
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BlueVibrant,
                                uncheckedThumbColor = TextSoft,
                                uncheckedTrackColor = Color.White.copy(0.15f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Stat Cards ───────────────────────────────────────────────
                // FIX: StatCard no longer uses GlassCard (white bg).
                // Now uses dark CardBg → text stays explicitly white/grey, always visible.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Rating",
                        value = "${"%.1f".format(user?.rating ?: 5.0)} ★",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFFC107)
                    )
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddFundsDialog = true },
                        label = "Wallet",
                        value = "Rs. ${user?.walletBalance?.toInt() ?: 0}",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = Color(0xFF4CAF50)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Payment Methods ──────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Payment Methods",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        PaymentMethodItem(
                            label = "JazzCash",
                            icon = Icons.Default.MobileFriendly,
                            isSelected = true
                        )
                    }
                }

                // FIX: replaced Spacer(Modifier.weight(1f)) — weight is illegal in
                // a vertically-scrollable Column. Using a fixed spacer instead.
                Spacer(Modifier.height(28.dp))

                // ── Logout Button ────────────────────────────────────────────
                // FIX: removed padding(bottom=16.dp) from button modifier — that
                // padding was shrinking the button's effective touch area. Height
                // is now the full 52.dp, bottom spacing via Spacer above/below.
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F).copy(0.18f),
                        contentColor = Color(0xFFEF5350)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFFEF5350).copy(0.4f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Loading overlay ──────────────────────────────────────────────
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueVibrant)
                }
            }
        }
    }

    // ─── Full Screen Photo Dialog ──────────────────────────────────────────────
    if (showFullScreenPhoto && hasPicture) {
        Dialog(
            onDismissRequest = { showFullScreenPhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullScreenPhoto = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(picModel).crossfade(true).build(),
                    contentDescription = "Full screen profile picture",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullScreenPhoto = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    // ─── Photo Bottom Sheet ────────────────────────────────────────────────────
    if (showPhotoMenu) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoMenu = false },
            containerColor = CardBg,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    "Profile Photo",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = TextWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                Spacer(Modifier.height(4.dp))
                PhotoMenuRow(Icons.Default.RemoveRedEye, "View Profile Picture", TextSoft, hasPicture) {
                    showPhotoMenu = false
                    showFullScreenPhoto = true
                }
                PhotoMenuRow(Icons.Default.PhotoCamera, "Update Profile Picture", BlueVibrant, true) {
                    showPhotoMenu = false
                    photoPickerLauncher.launch("image/*")
                }
                if (hasPicture) {
                    PhotoMenuRow(Icons.Default.Delete, "Remove Photo", Color(0xFFEF5350), true) {
                        showPhotoMenu = false
                        showRemovePhotoDialog = true
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ─── Remove Photo Confirm ──────────────────────────────────────────────────
    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoDialog = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove Photo?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Your profile picture will be removed.", color = TextSoft, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { viewModel.removeProfilePicture(); showRemovePhotoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePhotoDialog = false }) {
                    Text("Cancel", color = TextSoft)
                }
            }
        )
    }

    // ─── Logout Confirm ────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F).copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = { Text("Logout?", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    "You will be signed out of your Refa account.",
                    fontSize = 14.sp,
                    color = TextSoft
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Logout", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSoft, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── Edit Name Dialog ──────────────────────────────────────────────────────
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Edit Name", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Full Name", color = TextSoft) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = Color(0xFF152040),
                        unfocusedContainerColor = Color(0xFF152040),
                        focusedBorderColor = BlueVibrant,
                        unfocusedBorderColor = Color.White.copy(0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateName(editedName); showEditNameDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueVibrant),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSoft)
                }
            }
        )
    }

    // ─── Add Funds Dialog ──────────────────────────────────────────────────────
    if (showAddFundsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showAddFundsDialog = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Top Up Wallet", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Balance: Rs. ${user?.walletBalance?.toInt() ?: 0}",
                        fontSize = 13.sp,
                        color = TextSoft
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topUpAmount,
                        onValueChange = { topUpAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount (Rs.)", color = TextSoft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF152040),
                            unfocusedContainerColor = Color(0xFF152040),
                            focusedBorderColor = BlueVibrant,
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("200", "500", "1000", "2000")) { preset ->
                            AssistChip(
                                onClick = { topUpAmount = preset },
                                label = { Text("Rs.$preset", color = TextWhite, fontSize = 12.sp) },
                                enabled = !isLoading,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.White.copy(0.1f)
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = Color.White.copy(0.2f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = topUpAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addFunds(amount)
                            showAddFundsDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueVibrant),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Add Funds") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFundsDialog = false }, enabled = !isLoading) {
                    Text("Cancel", color = TextSoft)
                }
            }
        )
    }

    // ─── Vehicle Registration Dialog ───────────────────────────────────────────
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showVehicleDialog = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            title = {
                Text(
                    "Register Your Vehicle",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TextWhite
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Vehicle Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSoft)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = { Text("Model (e.g. Honda 125)", color = TextSoft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = TextSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF152040),
                            unfocusedContainerColor = Color(0xFF152040),
                            focusedBorderColor = BlueVibrant,
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Plate (e.g. LEC-123)", color = TextSoft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Numbers, null, tint = TextSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF152040),
                            unfocusedContainerColor = Color(0xFF152040),
                            focusedBorderColor = BlueVibrant,
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Vehicle Type:", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSoft)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VehicleType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name, fontSize = 11.sp) },
                                enabled = !isLoading,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BlueVibrant,
                                    selectedLabelColor = TextWhite,
                                    containerColor = Color.White.copy(0.08f),
                                    labelColor = TextSoft
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    Spacer(Modifier.height(14.dp))
                    Text("Contact Info", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSoft)
                    Text(
                        "Used by passengers to reach you on WhatsApp.",
                        fontSize = 11.sp,
                        color = TextSoft.copy(0.7f),
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = driverPhone,
                        onValueChange = { driverPhone = it },
                        label = { Text("WhatsApp Number (03xxxxxxxxx)", color = TextSoft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF152040),
                            unfocusedContainerColor = Color(0xFF152040),
                            focusedBorderColor = BlueVibrant,
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = driverAddress,
                        onValueChange = { driverAddress = it },
                        label = { Text("Your Address / Area", color = TextSoft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        maxLines = 2,
                        leadingIcon = { Icon(Icons.Default.Home, null, tint = TextSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color(0xFF152040),
                            unfocusedContainerColor = Color(0xFF152040),
                            focusedBorderColor = BlueVibrant,
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            vehicleModel.isBlank() || vehiclePlate.isBlank() ->
                                Toast.makeText(context, "Enter vehicle model & plate", Toast.LENGTH_SHORT).show()
                            driverPhone.isBlank() ->
                                Toast.makeText(context, "Enter your WhatsApp number", Toast.LENGTH_SHORT).show()
                            driverAddress.isBlank() ->
                                Toast.makeText(context, "Enter your address", Toast.LENGTH_SHORT).show()
                            else -> viewModel.registerAndGoOnline(
                                vehicle  = Vehicle(vehicleModel.trim(), vehiclePlate.trim(), selectedType),
                                phoneNumber = driverPhone.trim(),
                                address  = driverAddress.trim(),
                                onSuccess = {
                                    showVehicleDialog = false
                                    onNavigateToDriverDashboard()
                                }
                            )
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueVibrant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading)
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    else
                        Text("Register & Go Online", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVehicleDialog = false }, enabled = !isLoading) {
                    Text("Cancel", color = TextSoft)
                }
            }
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun PhotoMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint.copy(alpha), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha)
        )
    }
}

// ─── Stat Card ─────────────────────────────────────────────────────────────────
// Background is dark (CardBg = 0xFF0F1E3D).
// Text colors are explicitly white/grey — never rely on MaterialTheme defaults here.

@Composable
fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            // FIX: explicit color = Color.White — always visible on dark CardBg
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = Color.White,
                maxLines = 1
            )
            // FIX: explicit color = TextSoft — always visible on dark CardBg
            Text(
                text = label,
                color = TextSoft,
                fontSize = 11.sp
            )
        }
    }
}

// ─── Payment Method Row ────────────────────────────────────────────────────────
// Background is dark (CardBg). Text must be explicitly white.

@Composable
fun PaymentMethodItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (label == "JazzCash") Color(0xFFE91E63).copy(0.15f)
                    else Color.White.copy(0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (label == "JazzCash") Color(0xFFE91E63) else TextSoft,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        // FIX: explicit color = Color.White — always visible on dark background
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
