package com.example.domain.model

data class TripPoint(
    val id: Long = 0,
    val tripId: Long = 0,
    val sequenceNumber: Int = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float = 0f,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val status: TripStatus = TripStatus.MOVING
)

data class GpsDiagnosticEvent(
    val id: Long = 0,
    val tripId: Long = 0,
    val gpsLostTime: Long,
    val gpsRecoveredTime: Long? = null,
    val durationMillis: Long? = null
)

data class Trip(
    val id: Long = 0,
    val tripNumber: Int = 1,
    val title: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val totalDurationMillis: Long = 0L,
    val movingDurationMillis: Long = 0L,
    val waitingDurationMillis: Long = 0L,
    val totalDistanceMeters: Double = 0.0,
    val averageSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val isCompleted: Boolean = true,
    val notes: String = ""
)

data class AppSettings(
    val movementThresholdKmh: Float = 2.0f,
    val idleDetectionDelaySeconds: Int = 10,
    val gpsUpdateIntervalSeconds: Int = 2,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val timeFormat: TimeFormat = TimeFormat.H24,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenAwake: Boolean = false,
    val powerSavingDimScreen: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val fuelPricePerLiter: Double = 0.0,
    val fuelEconomyKmPerLiter: Double = 0.0,
    val fuelCurrencySymbol: String = "Rs."
) {
    val isCostCalculatorConfigured: Boolean
        get() = fuelPricePerLiter > 0.0 && fuelEconomyKmPerLiter > 0.0
}

data class TripStatistics(
    val totalTrips: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalMovingDurationMillis: Long = 0L,
    val totalWaitingDurationMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val averageDurationMillis: Long = 0L,
    val averageDistanceMeters: Double = 0.0,
    val longestTripDistanceMeters: Double = 0.0,
    val shortestTripDistanceMeters: Double = 0.0,
    val highestRecordedSpeedMps: Float = 0f,
    val totalDrivingHours: Double = 0.0
)

enum class BackupFrequency(val label: String) {
    AFTER_COMPLETED_TRIP("After every completed trip"),
    DAILY("Daily"),
    WEEKLY("Weekly")
}

data class FullTripBackup(
    val backupVersion: Int = 1,
    val application: String = "Trip Timer",
    val createdAt: Long = System.currentTimeMillis(),
    val trips: List<Trip> = emptyList(),
    val routes: List<TripPoint> = emptyList(),
    val gpsDiagnostics: List<GpsDiagnosticEvent> = emptyList(),
    val settings: AppSettings = AppSettings()
)

data class DriveBackupInfo(
    val fileId: String,
    val name: String,
    val modifiedTime: Long,
    val sizeBytes: Long,
    val backupVersion: Int = 1,
    val tripCount: Int = 0,
    val routeCount: Int = 0,
    val gpsDiagnosticCount: Int = 0,
    val createdAt: Long = 0L
)

data class DriveConnectionState(
    val isConnected: Boolean = false,
    val accountEmail: String = "",
    val accountDisplayName: String = "",
    val lastBackupTimestamp: Long = 0L,
    val lastRestoreTimestamp: Long = 0L,
    val lastBackupTripCount: Int = 0,
    val lastBackupRouteCount: Int = 0,
    val lastBackupGpsDiagnosticCount: Int = 0,
    val autoBackupEnabled: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.AFTER_COMPLETED_TRIP
)
