package com.example.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates distance between two GPS coordinates using Haversine formula (pure math, fast & offline)
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Alias for haversine calculation
     */
    fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double = calculateDistanceMeters(lat1, lon1, lat2, lon2)

    /**
     * Checks whether a new GPS point is realistic (accuracy filter and max realistic speed filter)
     */
    fun isValidGpsPoint(
        accuracyMeters: Float,
        previousLat: Double?,
        previousLon: Double?,
        previousTimeMillis: Long?,
        currentLat: Double,
        currentLon: Double,
        currentTimeMillis: Long
    ): Boolean {
        // Filter out very poor accuracy fixes (> 35 meters)
        if (accuracyMeters > 35f) {
            return false
        }

        if (previousLat == null || previousLon == null || previousTimeMillis == null) {
            return true
        }

        val timeDiffMillis = currentTimeMillis - previousTimeMillis
        if (timeDiffMillis < 0L) {
            return false
        }

        val distanceMeters = calculateDistanceMeters(previousLat, previousLon, currentLat, currentLon)
        val timeDiffSeconds = timeDiffMillis / 1000.0

        if (timeDiffSeconds <= 0.0) {
            return distanceMeters < 1.0
        }

        val calculatedSpeedMps = distanceMeters / timeDiffSeconds

        // Vehicle maximum realistic speed filter (e.g. 70 m/s ~ 250 km/h) to eliminate GPS teleport jumps
        if (calculatedSpeedMps > 70.0) {
            return false
        }

        return true
    }
}
