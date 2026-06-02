package com.example.transportapp.presentation.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LoginEvent {
    object NavigateToHome   : LoginEvent()
    object NavigateToDriver : LoginEvent()
}

data class LoginUiState(
    val isLoginMode : Boolean = true,
    val name        : String  = "",
    val email       : String  = "",
    val password    : String  = "",
    val isLoading   : Boolean = false,
    val error       : String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<LoginEvent> = _navChannel.receiveAsFlow()

    fun onNameChange(value: String)     { _uiState.update { it.copy(name = value,     error = null) } }
    fun onEmailChange(value: String)    { _uiState.update { it.copy(email = value,    error = null) } }
    fun onPasswordChange(value: String) { _uiState.update { it.copy(password = value, error = null) } }
    fun toggleMode()                    { _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null) } }
    fun clearError()                    { _uiState.update { it.copy(error = null) } }

    fun performAuth() {
        val s = _uiState.value
        if (s.isLoginMode) signIn(s.email.trim(), s.password)
        else               signUp(s.email.trim(), s.password, s.name.trim())
    }

    private fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithEmail(email, password)
                .onSuccess { uid ->
                    /**
                     * Fix #2: Use a DIRECT Firestore read via getIsDriverMode() instead of
                     * collecting the getCurrentUser() Flow. The Flow's first emission after login
                     * can arrive BEFORE the Firestore snapshot fires, making isDriverMode appear
                     * false and routing a driver incorrectly to the passenger Home screen.
                     *
                     * getIsDriverMode() does a single .get().await() which always returns the
                     * current server value — fast (~200–400 ms) and 100% reliable.
                     */
                    val isDriver = authRepository.getIsDriverMode(uid)

                    /**
                     * Fix #3 (15-day lock enforcement): Also fetch the Firestore-persisted lock
                     * expiry timestamp. This survives app uninstalls and logouts because it lives
                     * in Firestore, NOT in SharedPreferences (which are wiped on uninstall and
                     * cleared by clearSession() on logout).
                     *
                     * Scenario A — Reinstall: SharedPreferences gone, Firestore lock intact.
                     *   → lockUntil > now  →  forceDriver = true  →  driver dashboard. ✅
                     *
                     * Scenario B — Logout → Login: session cleared, Firestore lock intact.
                     *   → lockUntil > now  →  forceDriver = true  →  driver dashboard. ✅
                     *
                     * Scenario C — isDriverMode somehow cleared in Firestore while lock is
                     *   still active (edge case / manual edit): lock takes priority and the
                     *   flag is silently restored to true. ✅
                     */
                    val lockUntil = authRepository.getDriverLockUntil(uid)
                    val isLockedAsDriver = lockUntil > System.currentTimeMillis()

                    // If the 15-day lock is active the user MUST stay in driver mode.
                    val forceDriver = isDriver || isLockedAsDriver

                    // Silently repair isDriverMode in Firestore if it was incorrectly false
                    // while the lock is still active (best-effort, non-blocking).
                    if (isLockedAsDriver && !isDriver) {
                        try { authRepository.toggleDriverMode(uid, true) } catch (_: Exception) {}
                    }

                    if (forceDriver) saveDriverFcmToken(uid)

                    _uiState.update { it.copy(isLoading = false) }
                    _navChannel.send(
                        if (forceDriver) LoginEvent.NavigateToDriver else LoginEvent.NavigateToHome
                    )
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = friendlyError(e.message)) }
                }
        }
    }

    private fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signUpWithEmail(email, password, name)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navChannel.send(LoginEvent.NavigateToHome)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = friendlyError(e.message)) }
                }
        }
    }

    private fun saveDriverFcmToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                authRepository.saveFcmToken(userId, token)
                Log.d("LoginVM", "FCM token saved for driver $userId")
            } catch (e: Exception) {
                Log.e("LoginVM", "FCM token save failed (non-fatal)", e)
            }
        }
    }

    private fun friendlyError(raw: String?): String = when {
        raw == null                                  -> "An error occurred. Please try again."
        raw.contains("password",    ignoreCase=true) -> "Incorrect password. Please try again."
        raw.contains("no user",     ignoreCase=true) ||
        raw.contains("user not found", ignoreCase=true) -> "No account found with this email."
        raw.contains("email",       ignoreCase=true) &&
        raw.contains("already",     ignoreCase=true) -> "This email is already registered."
        raw.contains("network",     ignoreCase=true) ||
        raw.contains("connection",  ignoreCase=true) -> "No internet connection."
        else -> raw
    }
}
