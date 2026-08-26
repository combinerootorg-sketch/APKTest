package com.example.utils

import com.example.domain.model.DistanceUnit
import com.example.domain.model.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    private const val METERS_PER_KM = 1000.0
    private const val METERS_PER_MILE = 1609.344
    private const val MPS_TO_KMH = 3.6
    private const val MPS_TO_MPH = 2.23694

    fun formatSpeed(speedMps: Float, unit: DistanceUnit): String {
        val speed = if (unit == DistanceUnit.KILOMETERS) {
            speedMps * MPS_TO_KMH
        } else {
            speedMps * MPS_TO_MPH
        }
        return String.format(Locale.US, "%.1f", speed.coerceAtLeast(0.0))
    }

    fun formatSpeedWithUnit(speedMps: Float, unit: DistanceUnit): String {
        return "${formatSpeed(speedMps, unit)} ${unit.speedSymbol}"
    }

    fun formatDistance(meters: Double, unit: DistanceUnit): String {
        val distance = if (unit == DistanceUnit.KILOMETERS) {
            meters / METERS_PER_KM
        } else {
            meters / METERS_PER_MILE
        }
        return String.format(Locale.US, "%.2f", distance.coerceAtLeast(0.0))
    }

    fun formatDistanceWithUnit(meters: Double, unit: DistanceUnit): String {
        return "${formatDistance(meters, unit)} ${unit.unitSymbol}"
    }

    fun formatDuration(durationMillis: Long): String {
        val safeMillis = durationMillis.coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(safeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDurationShort(durationMillis: Long): String {
        val safeMillis = durationMillis.coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(safeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m ${seconds}s"
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long, format: TimeFormat): String {
        val pattern = if (format == TimeFormat.H24) "HH:mm:ss" else "h:mm:ss a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeShort(timestamp: Long, format: TimeFormat): String {
        val pattern = if (format == TimeFormat.H24) "HH:mm" else "h:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long, format: TimeFormat): String {
        val pattern = if (format == TimeFormat.H24) "MMM d, yyyy • HH:mm" else "MMM d, yyyy • h:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
