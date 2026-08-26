package com.example.data.repository

import com.example.data.database.dao.TripDao
import com.example.data.database.entity.GpsDiagnosticEntity
import com.example.data.database.entity.TripEntity
import com.example.data.database.entity.TripPointEntity
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.domain.model.TripStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getTripById(tripId: Long): Trip? {
        return tripDao.getTripById(tripId)?.toDomain()
    }

    fun observeTripById(tripId: Long): Flow<Trip?> {
        return tripDao.observeTripById(tripId).map { it?.toDomain() }
    }

    suspend fun getNextTripNumber(): Int {
        val maxNumber = tripDao.getMaxTripNumber() ?: 0
        return maxNumber + 1
    }

    suspend fun saveCompletedTrip(
        trip: Trip,
        points: List<TripPoint>,
        gpsDiagnostics: List<GpsDiagnosticEvent> = emptyList()
    ): Long {
        val tripEntity = TripEntity.fromDomain(trip)
        val pointEntities = points.map { TripPointEntity.fromDomain(it) }
        val diagEntities = gpsDiagnostics.map { GpsDiagnosticEntity.fromDomain(it) }
        tripDao.saveCompletedTrip(tripEntity, pointEntities, diagEntities)
        return tripEntity.id
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(TripEntity.fromDomain(trip))
    }

    suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteGpsDiagnosticsForTrip(tripId)
        tripDao.deletePointsForTrip(tripId)
        tripDao.deleteTrip(tripId)
    }

    suspend fun getAllTripsList(): List<Trip> {
        return tripDao.getAllTripsList().map { it.toDomain() }
    }

    suspend fun getAllTripPointsList(): List<TripPoint> {
        return tripDao.getAllTripPointsList().map { it.toDomain() }
    }

    suspend fun getAllGpsDiagnosticsList(): List<GpsDiagnosticEvent> {
        return tripDao.getAllGpsDiagnosticsList().map { it.toDomain() }
    }

    suspend fun replaceAllData(
        trips: List<Trip>,
        points: List<TripPoint>,
        gpsDiagnostics: List<GpsDiagnosticEvent>
    ) {
        val tripEntities = trips.map { TripEntity.fromDomain(it) }
        val pointEntities = points.map { TripPointEntity.fromDomain(it) }
        val diagEntities = gpsDiagnostics.map { GpsDiagnosticEntity.fromDomain(it) }
        tripDao.replaceAllData(tripEntities, pointEntities, diagEntities)
    }

    suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }

    fun getPointsForTrip(tripId: Long): Flow<List<TripPoint>> {
        return tripDao.getPointsForTrip(tripId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getPointsForTripList(tripId: Long): List<TripPoint> {
        return tripDao.getPointsForTripList(tripId).map { it.toDomain() }
    }

    fun getGpsDiagnosticsForTrip(tripId: Long): Flow<List<GpsDiagnosticEvent>> {
        return tripDao.getGpsDiagnosticsForTrip(tripId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getGpsDiagnosticsForTripList(tripId: Long): List<GpsDiagnosticEvent> {
        return tripDao.getGpsDiagnosticsForTripList(tripId).map { it.toDomain() }
    }

    fun getTripStatistics(): Flow<TripStatistics> {
        return allTrips.map { trips ->
            val completedTrips = trips.filter { it.isCompleted }
            if (completedTrips.isEmpty()) {
                TripStatistics()
            } else {
                val totalTrips = completedTrips.size
                val totalDistance = completedTrips.sumOf { it.totalDistanceMeters }
                val totalMovingMillis = completedTrips.sumOf { it.movingDurationMillis }
                val totalWaitingMillis = completedTrips.sumOf { it.waitingDurationMillis }
                val totalDurationMillis = completedTrips.sumOf { it.totalDurationMillis }
                val avgDurationMillis = totalDurationMillis / totalTrips
                val avgDistance = totalDistance / totalTrips
                val longest = completedTrips.maxOfOrNull { it.totalDistanceMeters } ?: 0.0
                val shortest = completedTrips.minOfOrNull { it.totalDistanceMeters } ?: 0.0
                val highestSpeed = completedTrips.maxOfOrNull { it.maxSpeedMps } ?: 0f
                val totalDrivingHours = totalMovingMillis.toDouble() / (1000.0 * 60.0 * 60.0)

                TripStatistics(
                    totalTrips = totalTrips,
                    totalDistanceMeters = totalDistance,
                    totalMovingDurationMillis = totalMovingMillis,
                    totalWaitingDurationMillis = totalWaitingMillis,
                    totalDurationMillis = totalDurationMillis,
                    averageDurationMillis = avgDurationMillis,
                    averageDistanceMeters = avgDistance,
                    longestTripDistanceMeters = longest,
                    shortestTripDistanceMeters = shortest,
                    highestRecordedSpeedMps = highestSpeed,
                    totalDrivingHours = totalDrivingHours
                )
            }
        }
    }
}
