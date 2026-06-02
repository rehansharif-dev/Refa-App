package com.example.transportapp.presentation.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ─── Brand colors ─────────────────────────────────────────────────────────────
private val NavyDeep   = Color(0xFF040D21)
private val NavyMid    = Color(0xFF0A1A3A)
private val NavyAccent = Color(0xFF1565C0)
private val BlueVibrant= Color(0xFF2979FF)
private val AccentGold = Color(0xFFFFC107)
private val TextWhite  = Color(0xFFFFFFFF)
private val TextSoft   = Color(0xFFB0BEC5)
private val CardBg     = Color(0xFF0F1E3D)
private val FieldBg    = Color(0xFF152040)
private val FieldBorder= Color(0xFF1E3A6E)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onDriverLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val hPad = if (isTablet) 48.dp else 28.dp

    // ── Entry animation ────────────────────────────────────────────────────
    val logoAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(800, easing = EaseOut), label = "logo")
    val cardOffset by animateFloatAsState(targetValue = 0f, animationSpec = tween(600, 200, easing = EaseOut), label = "card")

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome   -> onLoginSuccess()
                is LoginEvent.NavigateToDriver -> onDriverLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyMid, Color(0xFF0D1B2A))))
    ) {
        // ── Decorative circles ─────────────────────────────────────────────
        Box(modifier = Modifier.offset((-60).dp, (-60).dp).size(200.dp).clip(CircleShape)
            .background(BlueVibrant.copy(alpha = 0.08f)))
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(40.dp, (-30).dp).size(140.dp)
            .clip(CircleShape).background(AccentGold.copy(alpha = 0.06f)))
        Box(modifier = Modifier.align(Alignment.BottomStart).offset((-40).dp, 60.dp).size(160.dp)
            .clip(CircleShape).background(BlueVibrant.copy(alpha = 0.07f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = hPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo + Brand ───────────────────────────────────────────────
            Box(modifier = Modifier.alpha(logoAlpha)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo circle
                    Box(
                        modifier = Modifier.size(84.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BlueVibrant, NavyAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, tint = TextWhite,
                            modifier = Modifier.size(42.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("REFA", fontWeight = FontWeight.ExtraBold, fontSize = 36.sp,
                        color = TextWhite, letterSpacing = 6.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Ride Smart. Travel Fast.", fontSize = 13.sp,
                        color = TextSoft, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Card ───────────────────────────────────────────────────────
            Box(modifier = Modifier
                .offset(y = cardOffset.dp)
                .then(if (isTablet) Modifier.widthIn(max = 520.dp) else Modifier)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardBg)
                        .border(1.dp, FieldBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Mode toggle tabs ───────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FieldBg)
                            .padding(4.dp)
                    ) {
                        TabButton(label = "Login", selected = uiState.isLoginMode,
                            modifier = Modifier.weight(1f), onClick = { if (!uiState.isLoginMode) viewModel.toggleMode() })
                        TabButton(label = "Sign Up", selected = !uiState.isLoginMode,
                            modifier = Modifier.weight(1f), onClick = { if (uiState.isLoginMode) viewModel.toggleMode() })
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Title ──────────────────────────────────────────────
                    AnimatedContent(
                        targetState = uiState.isLoginMode,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "title"
                    ) { isLogin ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isLogin) "Welcome Back!" else "Create Account",
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = TextWhite
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (isLogin) "Sign in to continue your journey" else "Join thousands of riders today",
                                fontSize = 13.sp, color = TextSoft, textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── Name field (Sign Up only) ──────────────────────────
                    AnimatedVisibility(
                        visible = !uiState.isLoginMode,
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Column {
                            FieldLabel("Full Name")
                            Spacer(Modifier.height(6.dp))
                            RefaTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                placeholder = "Enter your full name",
                                leadingIcon = Icons.Default.Person,
                                imeAction = ImeAction.Next,
                                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // ── Email field ────────────────────────────────────────
                    FieldLabel("Email Address")
                    Spacer(Modifier.height(6.dp))
                    RefaTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "your@email.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(Modifier.height(14.dp))

                    // ── Password field ─────────────────────────────────────
                    FieldLabel("Password")
                    Spacer(Modifier.height(6.dp))
                    RefaTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { focusManager.clearFocus(); viewModel.performAuth() },
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Submit button ──────────────────────────────────────
                    Button(
                        onClick = { focusManager.clearFocus(); viewModel.performAuth() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueVibrant,
                            disabledContainerColor = BlueVibrant.copy(alpha = 0.5f)
                        )
                    ) {
                        AnimatedContent(
                            targetState = uiState.isLoading,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "btn"
                        ) { loading ->
                            if (loading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = TextWhite, strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Please wait...", color = TextWhite,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (uiState.isLoginMode) "Sign In" else "Create Account",
                                        color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, null,
                                        tint = TextWhite, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Toggle mode link ───────────────────────────────────
                    TextButton(onClick = viewModel::toggleMode) {
                        Text(
                            text = if (uiState.isLoginMode) "Don't have an account? " else "Already have an account? ",
                            color = TextSoft, fontSize = 13.sp
                        )
                        Text(
                            text = if (uiState.isLoginMode) "Sign Up" else "Login",
                            color = BlueVibrant, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Error message ──────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.error != null,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(200)) + shrinkVertically()
            ) {
                uiState.error?.let { errMsg ->
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFB71C1C).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFFEF9A9A).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF9A9A),
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(errMsg, color = Color(0xFFEF9A9A), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Feature highlights ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeaturePill(Icons.Default.Speed, "Fast Rides")
                FeaturePill(Icons.Default.Security, "Safe & Secure")
                FeaturePill(Icons.Default.SupportAgent, "24/7 Support")
            }

            Spacer(Modifier.height(28.dp))

            // ── Footer ─────────────────────────────────────────────────────
            Text(
                "By continuing you agree to Refa's Terms of Service",
                color = TextSoft.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Reusable text field ──────────────────────────────────────────────────────

@Composable
private fun RefaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSoft.copy(alpha = 0.5f), fontSize = 14.sp) },
        leadingIcon = {
            Icon(leadingIcon, null, tint = if (value.isNotEmpty()) BlueVibrant else TextSoft,
                modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = TextSoft, modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedBorderColor = BlueVibrant,
            unfocusedBorderColor = FieldBorder,
            cursorColor = BlueVibrant
        )
    )
}

// ─── Small label above fields ─────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth())
}

// ─── Tab button (Login / Sign Up) ─────────────────────────────────────────────

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BlueVibrant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) TextWhite else TextSoft,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ─── Feature pill (bottom of screen) ─────────────────────────────────────────

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(BlueVibrant.copy(alpha = 0.12f))
                .border(1.dp, BlueVibrant.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BlueVibrant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextSoft, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
