package com.example.ui.screens.history

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
import com.example.domain.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val context: Context,
    private val tripRepository: TripRepository,
    private val exportManager: TripExportManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val trips: StateFlow<List<Trip>> = combine(
        tripRepository.allTrips,
        _searchQuery
    ) { allTrips, query ->
        val completed = allTrips.filter { it.isCompleted }
        if (query.isBlank()) {
            completed
        } else {
            completed.filter {
                it.tripNumber.toString().contains(query, ignoreCase = true) ||
                        it.title.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
        }
    }

    fun deleteAllTrips() {
        viewModelScope.launch {
            tripRepository.deleteAllTrips()
        }
    }

    fun shareTrip(trip: Trip) {
        viewModelScope.launch {
            val points = tripRepository.getPointsForTripList(trip.id)
            val jsonFile = exportManager.exportTripsToJson(listOf(trip to points))
            val shareIntent = exportManager.createShareIntent(jsonFile, "application/json").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Trip #${trip.tripNumber}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun exportAllCsv() {
        viewModelScope.launch {
            val all = trips.value
            if (all.isNotEmpty()) {
                val csvFile = exportManager.exportTripsToCsv(all)
                val shareIntent = exportManager.createShareIntent(csvFile, "text/csv").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export Trips CSV").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    fun exportAllJson() {
        viewModelScope.launch {
            val all = trips.value
            if (all.isNotEmpty()) {
                val tripsWithPoints = all.map { trip ->
                    trip to tripRepository.getPointsForTripList(trip.id)
                }
                val jsonFile = exportManager.exportTripsToJson(tripsWithPoints)
                val shareIntent = exportManager.createShareIntent(jsonFile, "application/json").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export Trips JSON").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as TripTimerApplication
                return HistoryViewModel(
                    context = app,
                    tripRepository = app.tripRepository,
                    exportManager = app.exportManager,
                    settingsRepository = app.settingsRepository
                ) as T
            }
        }
    }
}
