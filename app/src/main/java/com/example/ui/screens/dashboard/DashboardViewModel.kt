package com.example.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TripTimerApplication
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.Trip
import com.example.domain.model.TripStatistics
import com.example.ui.components.ChartBarData
import com.example.utils.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val statistics: StateFlow<TripStatistics> = tripRepository.getTripStatistics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TripStatistics())

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val allTrips: StateFlow<List<Trip>> = tripRepository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly / Recent Trips Bar Chart Data
    val recentTripsChartData: StateFlow<List<ChartBarData>> = tripRepository.allTrips.map { trips ->
        val completed = trips.filter { it.isCompleted }.take(7).reversed()
        val unit = settings.value.distanceUnit

        completed.mapIndexed { index, trip ->
            val dist = if (unit == DistanceUnit.KILOMETERS) trip.totalDistanceMeters / 1000.0 else trip.totalDistanceMeters / 1609.344
            val label = "#${trip.tripNumber}"
            ChartBarData(
                label = label,
                value = dist.toFloat(),
                displayValue = String.format(Locale.US, "%.1f %s", dist, unit.unitSymbol)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as TripTimerApplication
                return DashboardViewModel(
                    tripRepository = app.tripRepository,
                    settingsRepository = app.settingsRepository
                ) as T
            }
        }
    }
}
