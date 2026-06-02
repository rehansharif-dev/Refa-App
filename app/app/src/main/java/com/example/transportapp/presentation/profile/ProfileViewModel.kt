package com.example.transportapp.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.domain.model.User
import com.example.transportapp.domain.model.Vehicle
import com.example.transportapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { _user.value = it }
        }
    }

    fun toggleDriverMode(isEnabled: Boolean, onSuccess: () -> Unit = {}) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.toggleDriverMode(currentUser.id, isEnabled)
                .onSuccess { _isLoading.value = false; onSuccess() }
                .onFailure { e -> _isLoading.value = false; _error.value = e.message ?: "Operation failed" }
        }
    }

    /**
     * Register driver with vehicle info AND contact details (phone number + address).
     * Contact details are saved to both Firestore and Realtime Database so passengers
     * can contact the driver via WhatsApp in an emergency.
     */
    fun registerAndGoOnline(vehicle: Vehicle, phoneNumber: String, address: String, onSuccess: () -> Unit) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authRepository.registerDriverAndGoOnline(currentUser.id, vehicle)
                .onSuccess {
                    // Save contact details after driver is registered so RTDB entry exists
                    authRepository.updateDriverContactDetails(currentUser.id, phoneNumber, address)
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { e -> _isLoading.value = false; _error.value = e.message ?: "Registration failed" }
        }
    }

    fun addFunds(amount: Double) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            authRepository.updateWalletBalance(currentUser.id, currentUser.walletBalance + amount)
        }
    }

    fun updateName(newName: String) {
        val currentUser = _user.value ?: return
        if (newName.isBlank()) { _error.value = "Name cannot be empty"; return }
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.updateUserName(currentUser.id, newName.trim())
                .onSuccess { _isLoading.value = false; _successMessage.value = "Name updated!" }
                .onFailure { e -> _isLoading.value = false; _error.value = e.message ?: "Update failed" }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.uploadProfilePicture(currentUser.id, uri)
                .onSuccess { _isLoading.value = false; _successMessage.value = "Photo updated!" }
                .onFailure { e -> _isLoading.value = false; _error.value = "Upload failed: ${e.message}" }
        }
    }

    fun removeProfilePicture() {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.removeProfilePicture(currentUser.id)
                .onSuccess { _isLoading.value = false; _successMessage.value = "Photo removed!" }
                .onFailure { e -> _isLoading.value = false; _error.value = e.message ?: "Could not remove photo" }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLogout()
        }
    }

    fun clearError() { _error.value = null }
    fun clearSuccess() { _successMessage.value = null }
}
