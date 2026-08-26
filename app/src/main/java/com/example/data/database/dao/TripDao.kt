package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.GpsDiagnosticEntity
import com.example.data.database.entity.TripEntity
import com.example.data.database.entity.TripPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startTime ASC")
    suspend fun getAllTripsList(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTripById(tripId: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveOrInterruptedTrip(): TripEntity?

    @Query("SELECT MAX(tripNumber) FROM trips")
    suspend fun getMaxTripNumber(): Int?

    @Query("SELECT COUNT(*) FROM trips WHERE isCompleted = 1")
    fun getCompletedTripCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()

    // Points queries
    @Query("SELECT * FROM trip_points ORDER BY tripId ASC, sequenceNumber ASC")
    suspend fun getAllTripPointsList(): List<TripPointEntity>

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY sequenceNumber ASC")
    fun getPointsForTrip(tripId: Long): Flow<List<TripPointEntity>>

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY sequenceNumber ASC")
    suspend fun getPointsForTripList(tripId: Long): List<TripPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TripPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TripPointEntity): Long

    @Query("DELETE FROM trip_points WHERE tripId = :tripId")
    suspend fun deletePointsForTrip(tripId: Long)

    @Query("DELETE FROM trip_points")
    suspend fun deleteAllTripPoints()

    // GPS Diagnostics queries
    @Query("SELECT * FROM gps_diagnostic_events ORDER BY tripId ASC, gpsLostTime ASC")
    suspend fun getAllGpsDiagnosticsList(): List<GpsDiagnosticEntity>

    @Query("SELECT * FROM gps_diagnostic_events WHERE tripId = :tripId ORDER BY gpsLostTime ASC")
    fun getGpsDiagnosticsForTrip(tripId: Long): Flow<List<GpsDiagnosticEntity>>

    @Query("SELECT * FROM gps_diagnostic_events WHERE tripId = :tripId ORDER BY gpsLostTime ASC")
    suspend fun getGpsDiagnosticsForTripList(tripId: Long): List<GpsDiagnosticEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsDiagnostics(events: List<GpsDiagnosticEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsDiagnostic(event: GpsDiagnosticEntity): Long

    @Query("DELETE FROM gps_diagnostic_events WHERE tripId = :tripId")
    suspend fun deleteGpsDiagnosticsForTrip(tripId: Long)

    @Query("DELETE FROM gps_diagnostic_events")
    suspend fun deleteAllGpsDiagnostics()

    @Transaction
    suspend fun replaceAllData(
        trips: List<TripEntity>,
        points: List<TripPointEntity>,
        gpsEvents: List<GpsDiagnosticEntity>
    ) {
        deleteAllGpsDiagnostics()
        deleteAllTripPoints()
        deleteAllTrips()
        if (trips.isNotEmpty()) {
            insertTrips(trips)
        }
        if (points.isNotEmpty()) {
            insertPoints(points)
        }
        if (gpsEvents.isNotEmpty()) {
            insertGpsDiagnostics(gpsEvents)
        }
    }

    @Transaction
    suspend fun saveCompletedTrip(
        trip: TripEntity,
        points: List<TripPointEntity>,
        gpsEvents: List<GpsDiagnosticEntity> = emptyList()
    ) {
        val tripId = insertTrip(trip)
        val mappedPoints = points.map { it.copy(tripId = tripId) }
        if (mappedPoints.isNotEmpty()) {
            insertPoints(mappedPoints)
        }
        val mappedEvents = gpsEvents.map { it.copy(tripId = tripId) }
        if (mappedEvents.isNotEmpty()) {
            insertGpsDiagnostics(mappedEvents)
        }
    }
}
