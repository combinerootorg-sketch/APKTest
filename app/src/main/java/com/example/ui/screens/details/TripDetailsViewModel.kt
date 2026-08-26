package com.example.ui.screens.details

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TripTimerApplication
import com.example.data.export.TripExportManager
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripDetailsViewModel(
    private val tripId: Long,
    private val context: Context,
    private val tripRepository: TripRepository,
    private val exportManager: TripExportManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val trip: StateFlow<Trip?> = tripRepository.observeTripById(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val points: StateFlow<List<TripPoint>> = tripRepository.getPointsForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gpsDiagnostics: StateFlow<List<GpsDiagnosticEvent>> = tripRepository.getGpsDiagnosticsForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun shareTripJson() {
        viewModelScope.launch {
            val currentTrip = trip.value ?: return@launch
            val currentPoints = points.value
            val jsonFile = exportManager.exportTripsToJson(listOf(currentTrip to currentPoints))
            val shareIntent = exportManager.createShareIntent(jsonFile, "application/json").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Trip #${currentTrip.tripNumber} (JSON)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun shareTripCsv() {
        viewModelScope.launch {
            val currentTrip = trip.value ?: return@launch
            val csvFile = exportManager.exportTripsToCsv(listOf(currentTrip))
            val shareIntent = exportManager.createShareIntent(csvFile, "text/csv").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Trip #${currentTrip.tripNumber} (CSV)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun deleteTrip(onDeleted: () -> Unit) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
            onDeleted()
        }
    }

    companion object {
        fun provideFactory(tripId: Long, context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as TripTimerApplication
                return TripDetailsViewModel(
                    tripId = tripId,
                    context = app,
                    tripRepository = app.tripRepository,
                    exportManager = app.exportManager,
                    settingsRepository = app.settingsRepository
                ) as T
            }
        }
    }
}
