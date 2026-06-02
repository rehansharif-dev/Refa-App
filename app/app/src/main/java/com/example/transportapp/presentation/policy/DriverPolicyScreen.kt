package com.example.transportapp.presentation.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.data.session.SessionManager

private val NavyDeep   = Color(0xFF040D21)
private val NavyMid    = Color(0xFF0A1A3A)
private val BlueVibrant= Color(0xFF2979FF)
private val CardBg     = Color(0xFF0F1E3D)
private val TextWhite  = Color.White
private val TextSoft   = Color(0xFFB0BEC5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPolicyScreen(
    sessionManager: SessionManager,
    daysRemaining: Int,
    onAcceptAndContinue: () -> Unit
) {
    val driverPolicyViewModel: DriverPolicyViewModel = hiltViewModel()
    var accepted by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(NavyDeep, NavyMid, Color(0xFF0D1B2A))))) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header icon ────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(BlueVibrant, Color(0xFF1565C0)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = TextWhite, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Driver Mode Policy", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = TextWhite)
            Spacer(Modifier.height(6.dp))
            Text(
                "Please read and accept all policies before switching to Driver Mode.",
                color = TextSoft, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── Policy Cards ───────────────────────────────────────────────
            PolicyCard(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFFEF5350),
                iconBg = Color(0xFFD32F2F).copy(0.15f),
                title = "15-Day Commitment Lock",
                body = "Once you activate Driver Mode, you CANNOT switch back to Passenger Mode for 15 days. " +
                        "This policy ensures consistent service availability for all passengers. " +
                        "After 15 days, you may switch freely at any time."
            )
            Spacer(Modifier.height(12.dp))
            PolicyCard(
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = Color(0xFFFFC107),
                iconBg = Color(0xFFFFC107).copy(0.12f),
                title = "Instant Fare Payment",
                body = "Your fare is credited directly to your Refa wallet immediately after each " +
                        "ride is completed. No waiting — you are paid the moment the passenger's trip ends."
            )
            Spacer(Modifier.height(12.dp))
            PolicyCard(
                icon = Icons.Default.VerifiedUser,
                iconColor = Color(0xFF4CAF50),
                iconBg = Color(0xFF4CAF50).copy(0.12f),
                title = "Safety & Conduct",
                body = "All drivers must maintain a 4.0+ rating and treat passengers respectfully. " +
                        "Violation of traffic laws, any bad behaviour, or illegal activity will result " +
                        "in immediate account suspension and strict legal action."
            )
            Spacer(Modifier.height(12.dp))
            PolicyCard(
                icon = Icons.Default.SupportAgent,
                iconColor = Color(0xFF64B5F6),
                iconBg = Color(0xFF2979FF).copy(0.12f),
                title = "Support & Availability",
                body = "During the 15-day lock period you are expected to be available as a driver. " +
                        "Emergency exceptions require contacting Refa Support via the SOS button."
            )

            // ── Lock period reminder ───────────────────────────────────────
            if (daysRemaining > 0) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF57C00).copy(0.12f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HourglassTop, null, tint = Color(0xFFFFA726),
                        modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Lock Period Active", fontWeight = FontWeight.Bold, color = Color(0xFFFFA726),
                            fontSize = 13.sp)
                        Text(
                            "$daysRemaining day${if (daysRemaining != 1) "s" else ""} before you can switch to Passenger",
                            fontSize = 12.sp, color = TextSoft
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Checkbox acceptance ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = { accepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BlueVibrant,
                        uncheckedColor = TextSoft,
                        checkmarkColor = TextWhite
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "I have read and fully agree to all Refa Driver Policies listed above.",
                    color = TextSoft, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Accept button ──────────────────────────────────────────────
            Button(
                onClick = {
                    // Save lock expiry to Firestore FIRST — this is the server-side
                    // enforcement that survives uninstalls, logouts, and device changes.
                    // The local sessionManager call below is kept for backward compat
                    // (drives the days-remaining display on same device without re-login).
                    driverPolicyViewModel.recordDriverLockInFirestore()
                    sessionManager.setPolicyPending(false)
                    sessionManager.recordDriverSwitchTimestamp()
                    onAcceptAndContinue()
                },
                enabled = accepted,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueVibrant,
                    disabledContainerColor = Color(0xFF1E3A6E)
                )
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp),
                    tint = if (accepted) TextWhite else TextSoft)
                Spacer(Modifier.width(10.dp))
                Text(
                    "I Understand & Accept Policy",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    color = if (accepted) TextWhite else TextSoft
                )
            }

            if (!accepted) {
                Spacer(Modifier.height(8.dp))
                Text("Please tick the checkbox above to continue", color = TextSoft.copy(0.6f),
                    fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PolicyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite)
            Spacer(Modifier.height(5.dp))
            Text(body, fontSize = 13.sp, color = TextSoft, lineHeight = 18.sp)
        }
    }
}
