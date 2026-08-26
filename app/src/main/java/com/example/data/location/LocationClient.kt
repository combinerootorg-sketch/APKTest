package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LocationResultWrapper(
    val location: Location? = null,
    val isLocationAvailable: Boolean? = null,
    val isPermissionDenied: Boolean = false,
    val isError: Boolean = false
)

interface LocationClient {
    fun getLocationUpdates(intervalSeconds: Int): Flow<LocationResultWrapper>
}

class FusedLocationClientImpl(private val context: Context) : LocationClient {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "TripTimerGPS"
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalSeconds: Int): Flow<LocationResultWrapper> = callbackFlow {
        val intervalMillis = (intervalSeconds.coerceAtLeast(1) * 1000).toLong()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis((intervalMillis / 2).coerceAtLeast(1000L))
            .setMaxUpdateDelayMillis(intervalMillis)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val lastLocation = result.lastLocation
                if (lastLocation != null) {
                    trySend(
                        LocationResultWrapper(
                            location = lastLocation,
                            isLocationAvailable = true
                        )
                    )
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                val isAvail = availability.isLocationAvailable
                Log.d(TAG, "FusedLocationProvider onLocationAvailability: isLocationAvailable=$isAvail")
                // Pass availability as supporting diagnostic info without terminating active GPS state
                trySend(
                    LocationResultWrapper(
                        location = null,
                        isLocationAvailable = isAvail
                    )
                )
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.w(TAG, "Failed to register location updates: ${e.message}")
                trySend(LocationResultWrapper(location = null, isError = true))
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates: ${e.message}")
            trySend(LocationResultWrapper(location = null, isPermissionDenied = true))
        } catch (e: Exception) {
            Log.e(TAG, "Exception requesting location updates: ${e.message}")
            trySend(LocationResultWrapper(location = null, isError = true))
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(locationCallback)
                Log.d(TAG, "Removed location callback successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error removing location callback: ${e.message}")
            }
        }
    }
}
