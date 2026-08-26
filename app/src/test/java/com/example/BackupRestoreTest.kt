package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupRepository
import com.example.data.backup.GoogleDriveAuthManager
import com.example.data.backup.GoogleDriveDataSource
import com.example.data.backup.LocalBackupDataSource
import com.example.data.database.AppDatabase
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupFrequency
import com.example.domain.model.DistanceUnit
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.domain.model.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var tripRepository: TripRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var localBackupDataSource: LocalBackupDataSource
    private lateinit var googleDriveDataSource: GoogleDriveDataSource
    private lateinit var googleDriveAuthManager: GoogleDriveAuthManager
    private lateinit var backupRepository: BackupRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tripRepository = TripRepository(database.tripDao())
        settingsRepository = SettingsRepository(context)
        localBackupDataSource = LocalBackupDataSource(context)
        googleDriveDataSource = GoogleDriveDataSource()
        googleDriveAuthManager = GoogleDriveAuthManager(context)
        backupRepository = BackupRepository(
            context = context,
            tripRepository = tripRepository,
            settingsRepository = settingsRepository,
            localBackupDataSource = localBackupDataSource,
            googleDriveDataSource = googleDriveDataSource,
            googleDriveAuthManager = googleDriveAuthManager
        )
    }

    @After
    fun tearDown() {
        database.close()
        localBackupDataSource.deleteSafetyBackupSnapshot()
    }

    @Test
    fun `test LocalBackupDataSource serialization and parsing integrity`() {
        val testTrips = listOf(
            Trip(
                id = 1L,
                tripNumber = 1,
                title = "Downtown Commute",
                startTime = 1000000L,
                endTime = 1001800L,
                totalDurationMillis = 1800000L,
                movingDurationMillis = 1200000L,
                waitingDurationMillis = 600000L,
                totalDistanceMeters = 15250.0,
                averageSpeedMps = 12.7f,
                maxSpeedMps = 22.5f,
                startLatitude = 37.7749,
                startLongitude = -122.4194,
                endLatitude = 37.7833,
                endLongitude = -122.4167,
                isCompleted = true,
                notes = "Morning route test"
            )
        )

        val testPoints = listOf(
            TripPoint(
                id = 10L,
                tripId = 1L,
                sequenceNumber = 0,
                timestamp = 1000000L,
                latitude = 37.7749,
                longitude = -122.4194,
                speedMps = 10.0f,
                accuracyMeters = 4.5f,
                altitudeMeters = 15.0,
                bearingDegrees = 90.0f,
                status = TripStatus.MOVING
            )
        )

        val testDiagnostics = listOf(
            GpsDiagnosticEvent(
                id = 100L,
                tripId = 1L,
                gpsLostTime = 1000500L,
                gpsRecoveredTime = 1000530L,
                durationMillis = 30000L
            )
        )

        val testSettings = AppSettings(
            movementThresholdKmh = 3.5f,
            distanceUnit = DistanceUnit.MILES,
            fuelPricePerLiter = 3.85,
            fuelEconomyKmPerLiter = 14.2,
            fuelCurrencySymbol = "$"
        )

        val json = localBackupDataSource.createBackupJson(testTrips, testPoints, testDiagnostics, testSettings)
        assertTrue(json.contains("Trip Timer"))
        assertTrue(json.contains("Downtown Commute"))

        val parsed = localBackupDataSource.parseBackupJson(json)
        assertEquals(1, parsed.backupVersion)
        assertEquals("Trip Timer", parsed.application)
        assertEquals(1, parsed.trips.size)
        assertEquals("Downtown Commute", parsed.trips.first().title)
        assertEquals(15250.0, parsed.trips.first().totalDistanceMeters, 0.01)

        assertEquals(1, parsed.routes.size)
        assertEquals(37.7749, parsed.routes.first().latitude, 0.0001)

        assertEquals(1, parsed.gpsDiagnostics.size)
        assertEquals(30000L, parsed.gpsDiagnostics.first().durationMillis)

        assertEquals(3.5f, parsed.settings.movementThresholdKmh)
        assertEquals(DistanceUnit.MILES, parsed.settings.distanceUnit)
        assertEquals(3.85, parsed.settings.fuelPricePerLiter, 0.01)
    }

    @Test
    fun `test safety snapshot creation and recovery`() {
        val testTrips = listOf(
            Trip(id = 1L, tripNumber = 1, title = "Safety Trip", totalDistanceMeters = 5000.0, isCompleted = true)
        )
        val testPoints = listOf(
            TripPoint(id = 1L, tripId = 1L, sequenceNumber = 0, timestamp = 1000L, latitude = 12.0, longitude = 77.0)
        )

        localBackupDataSource.createSafetyBackupSnapshot(testTrips, testPoints, emptyList(), AppSettings())
        val snapshot = localBackupDataSource.loadSafetyBackupSnapshot()
        assertNotNull(snapshot)
        assertEquals(1, snapshot!!.trips.size)
        assertEquals("Safety Trip", snapshot.trips.first().title)

        localBackupDataSource.deleteSafetyBackupSnapshot()
        val afterDelete = localBackupDataSource.loadSafetyBackupSnapshot()
        assertEquals(null, afterDelete)
    }

    @Test
    fun `test disconnect Google Drive preserves local data`() = runTest {
        backupRepository.connectGoogleDrive("test@drive.com", "Test", "sample_token")
        val initialTrip = Trip(id = 2L, tripNumber = 2, title = "Local Trip")
        tripRepository.saveCompletedTrip(initialTrip, emptyList())

        backupRepository.disconnectGoogleDrive()
        val conn = backupRepository.driveConnectionState.first()
        assertEquals(false, conn.isConnected)

        val localTrips = tripRepository.getAllTripsList()
        assertEquals(1, localTrips.size)
        assertEquals("Local Trip", localTrips.first().title)
    }
}
