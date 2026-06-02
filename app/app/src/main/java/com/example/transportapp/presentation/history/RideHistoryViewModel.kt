package com.example.transportapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.domain.model.Ride
import com.example.transportapp.domain.repository.AuthRepository
import com.example.transportapp.domain.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideHistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<Ride>>(emptyList())
    val history: StateFlow<List<Ride>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                user?.let {
                    rideRepository.getRideHistory(it.id).collect { rides ->
                        _history.value = rides.sortedByDescending { ride -> ride.timestamp }
                    }
                }
            }
        }
    }
}
