package com.example.transportapp.presentation.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for DriverPolicyScreen.
 *
 * Responsibility: persist the 15-day driver lock expiry to FIRESTORE when the
 * user accepts the policy. Writing to Firestore (not SharedPreferences) ensures
 * the lock survives app uninstalls and explicit logouts — which both wipe
 * SharedPreferences but leave Firestore untouched.
 *
 * How it works:
 *   1. On policy accept the screen calls [recordDriverLockInFirestore].
 *   2. This method checks whether a lock already exists in Firestore (idempotent).
 *   3. If not, it writes lockUntil = now + 15 days to the user's Firestore document.
 *   4. It also ensures isDriverMode = true is set in Firestore.
 *
 * The LoginViewModel then reads this timestamp on every subsequent login and
 * routes the user to the Driver Dashboard if the lock is still active.
 */
@HiltViewModel
class DriverPolicyViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userId = MutableStateFlow("")

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _userId.value = user?.id ?: ""
            }
        }
    }

    /**
     * Saves the 15-day driver lock to Firestore.
     *
     * Safe to call multiple times — only sets the lock if it has not been set
     * before (i.e., existing = 0L), so a driver who somehow lands on the policy
     * screen again won't have their lock extended.
     */
    fun recordDriverLockInFirestore() {
        val uid = _userId.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = authRepository.getDriverLockUntil(uid)
                if (existing == 0L) {
                    // First activation — set a fresh 15-day lock from now
                    val lockUntilMs = System.currentTimeMillis() + (15L * 24L * 60L * 60L * 1_000L)
                    authRepository.saveDriverLockUntil(uid, lockUntilMs)
                }
                // Ensure isDriverMode is true in Firestore regardless
                authRepository.toggleDriverMode(uid, true)
            } catch (_: Exception) {
                // Non-fatal: lock will be enforced by login check on next session
            } finally {
                _isSaving.value = false
            }
        }
    }
}
