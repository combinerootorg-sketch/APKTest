package com.example.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TripTimerApplication
import com.example.data.settings.SettingsRepository
import com.example.data.tracking.ActiveTripState
import com.example.data.tracking.TripTrackingEngine
import com.example.domain.model.AppSettings
import com.example.domain.model.GpsStatus
import com.example.domain.model.Trip
import com.example.domain.model.TripStatus
import com.example.service.TripTrackingService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val context: Context,
    private val trackingEngine: TripTrackingEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tripState: StateFlow<ActiveTripState> = trackingEngine.tripState

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _navigateToSummary = MutableSharedFlow<Trip>(extraBufferCapacity = 1)
    val navigateToSummary: SharedFlow<Trip> = _navigateToSummary.asSharedFlow()

    init {
        viewModelScope.launch {
            trackingEngine.tripCompletedEvent.collect { completedTrip ->
                _navigateToSummary.tryEmit(completedTrip)
            }
        }
    }

    fun startTrip() {
        TripTrackingService.startService(context)
    }

    fun pauseTrip() {
        TripTrackingService.pauseService(context)
    }

    fun resumeTrip() {
        TripTrackingService.resumeService(context)
    }

    fun stopTrip() {
        TripTrackingService.stopService(context)
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateKeepScreenAwake(enabled)
        }
    }

    fun setPowerSaving(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePowerSavingDimScreen(enabled)
        }
    }

    fun updateGpsStatus(status: GpsStatus) {
        trackingEngine.updateGpsStatus(status)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as TripTimerApplication
                return HomeViewModel(
                    context = app,
                    trackingEngine = app.trackingEngine,
                    settingsRepository = app.settingsRepository
                ) as T
            }
        }
    }
}
