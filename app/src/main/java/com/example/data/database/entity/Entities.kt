package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.domain.model.TripStatus

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["tripNumber"])
    ]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripNumber: Int,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val totalDurationMillis: Long,
    val movingDurationMillis: Long,
    val waitingDurationMillis: Long,
    val totalDistanceMeters: Double,
    val averageSpeedMps: Float,
    val maxSpeedMps: Float,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val isCompleted: Boolean,
    val notes: String = ""
) {
    fun toDomain(): Trip = Trip(
        id = id,
        tripNumber = tripNumber,
        title = title,
        startTime = startTime,
        endTime = endTime,
        totalDurationMillis = totalDurationMillis,
        movingDurationMillis = movingDurationMillis,
        waitingDurationMillis = waitingDurationMillis,
        totalDistanceMeters = totalDistanceMeters,
        averageSpeedMps = averageSpeedMps,
        maxSpeedMps = maxSpeedMps,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        isCompleted = isCompleted,
        notes = notes
    )

    companion object {
        fun fromDomain(trip: Trip): TripEntity = TripEntity(
            id = trip.id,
            tripNumber = trip.tripNumber,
            title = trip.title,
            startTime = trip.startTime,
            endTime = trip.endTime,
            totalDurationMillis = trip.totalDurationMillis,
            movingDurationMillis = trip.movingDurationMillis,
            waitingDurationMillis = trip.waitingDurationMillis,
            totalDistanceMeters = trip.totalDistanceMeters,
            averageSpeedMps = trip.averageSpeedMps,
            maxSpeedMps = trip.maxSpeedMps,
            startLatitude = trip.startLatitude,
            startLongitude = trip.startLongitude,
            endLatitude = trip.endLatitude,
            endLongitude = trip.endLongitude,
            isCompleted = trip.isCompleted,
            notes = trip.notes
        )
    }
}

@Entity(
    tableName = "trip_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["timestamp"])
    ]
)
data class TripPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val sequenceNumber: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val accuracyMeters: Float,
    val altitudeMeters: Double?,
    val bearingDegrees: Float?,
    val status: String
) {
    fun toDomain(): TripPoint = TripPoint(
        id = id,
        tripId = tripId,
        sequenceNumber = sequenceNumber,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        speedMps = speedMps,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        bearingDegrees = bearingDegrees,
        status = runCatching { TripStatus.valueOf(status) }.getOrDefault(TripStatus.MOVING)
    )

    companion object {
        fun fromDomain(point: TripPoint): TripPointEntity = TripPointEntity(
            id = point.id,
            tripId = point.tripId,
            sequenceNumber = point.sequenceNumber,
            timestamp = point.timestamp,
            latitude = point.latitude,
            longitude = point.longitude,
            speedMps = point.speedMps,
            accuracyMeters = point.accuracyMeters,
            altitudeMeters = point.altitudeMeters,
            bearingDegrees = point.bearingDegrees,
            status = point.status.name
        )
    }
}

@Entity(
    tableName = "gps_diagnostic_events",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["gpsLostTime"])
    ]
)
data class GpsDiagnosticEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val gpsLostTime: Long,
    val gpsRecoveredTime: Long?,
    val durationMillis: Long?
) {
    fun toDomain(): GpsDiagnosticEvent = GpsDiagnosticEvent(
        id = id,
        tripId = tripId,
        gpsLostTime = gpsLostTime,
        gpsRecoveredTime = gpsRecoveredTime,
        durationMillis = durationMillis ?: if (gpsRecoveredTime != null) (gpsRecoveredTime - gpsLostTime).coerceAtLeast(0L) else null
    )

    companion object {
        fun fromDomain(event: GpsDiagnosticEvent): GpsDiagnosticEntity = GpsDiagnosticEntity(
            id = event.id,
            tripId = event.tripId,
            gpsLostTime = event.gpsLostTime,
            gpsRecoveredTime = event.gpsRecoveredTime,
            durationMillis = event.durationMillis ?: if (event.gpsRecoveredTime != null) (event.gpsRecoveredTime - event.gpsLostTime).coerceAtLeast(0L) else null
        )
    }
}

