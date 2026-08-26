package com.example

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.feedback.FeedbackManager
import com.example.data.location.LocationClient
import com.example.data.location.LocationResultWrapper
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.data.tracking.ActiveTripState
import com.example.data.tracking.TripTrackingEngine
import com.example.domain.model.GpsStatus
import com.example.domain.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeLocationClient : LocationClient {
    val locationFlow = MutableSharedFlow<LocationResultWrapper>(replay = 1, extraBufferCapacity = 64)

    override fun getLocationUpdates(intervalSeconds: Int): Flow<LocationResultWrapper> {
        return locationFlow
    }

    fun emitLocation(
        lat: Double,
        lon: Double,
        speedMps: Float = 0f,
        accuracyMeters: Float = 5f,
        timeMillis: Long = System.currentTimeMillis()
    ) {
        val loc = Location("fused").apply {
            latitude = lat
            longitude = lon
            speed = speedMps
            accuracy = accuracyMeters
            time = timeMillis
        }
        locationFlow.tryEmit(LocationResultWrapper(location = loc, isLocationAvailable = true))
    }

    fun emitAvailability(isAvailable: Boolean) {
        locationFlow.tryEmit(LocationResultWrapper(location = null, isLocationAvailable = isAvailable))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripTrackingEngineTest {

    private lateinit var context: Context
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var tripRepository: TripRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var engine: TripTrackingEngine
    private var baseTime: Long = 1_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val db = AppDatabase.getInstance(context)
        tripRepository = TripRepository(db.tripDao())
        settingsRepository = SettingsRepository(context)
        feedbackManager = FeedbackManager(context)
        fakeLocationClient = FakeLocationClient()
        baseTime = 1_000_000L

        engine = TripTrackingEngine(
            context = context,
            tripRepository = tripRepository,
            settingsRepository = settingsRepository,
            locationClient = fakeLocationClient,
            feedbackManager = feedbackManager
        )
    }

    private suspend fun awaitCondition(
        timeoutMs: Long = 5000L,
        predicate: (ActiveTripState) -> Boolean
    ): ActiveTripState = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val current = engine.tripState.value
            if (predicate(current)) {
                return@withContext current
            }
            delay(20)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms. Current state: ${engine.tripState.value}")
    }

    @Test
    fun `stationary trip enters waiting state and maintains active GPS`() = runTest {
        engine.startTrip()
        val initialState = awaitCondition { it.isTracking }
        assertEquals(TripStatus.WAITING, initialState.tripStatus)
        assertEquals(GpsStatus.AVAILABLE, initialState.gpsStatus)

        // Stationary location update (speed 0 m/s)
        baseTime += 1000L
        fakeLocationClient.emitLocation(51.5000, -0.1200, speedMps = 0f, accuracyMeters = 8f, timeMillis = baseTime)
        val stationaryState = awaitCondition { it.lastLocation != null }
        assertEquals(TripStatus.WAITING, stationaryState.tripStatus)
        assertEquals(GpsStatus.AVAILABLE, stationaryState.gpsStatus)

        // Temporary location availability false callback should NOT force GPS UNAVAILABLE
        fakeLocationClient.emitAvailability(false)
        delay(150)
        assertEquals(GpsStatus.AVAILABLE, engine.tripState.value.gpsStatus)

        // Start moving (speed 5 m/s = 18 km/h >= 2 km/h)
        baseTime += 2000L
        fakeLocationClient.emitLocation(51.5001, -0.1200, speedMps = 5f, accuracyMeters = 8f, timeMillis = baseTime)
        val movingState = awaitCondition { it.tripStatus == TripStatus.MOVING }
        assertEquals(TripStatus.MOVING, movingState.tripStatus)
        assertEquals(GpsStatus.AVAILABLE, movingState.gpsStatus)
        assertTrue(movingState.totalDistanceMeters > 0.0)

        // Stop moving (speed 0 m/s)
        baseTime += 2000L
        fakeLocationClient.emitLocation(51.5001, -0.1200, speedMps = 0f, accuracyMeters = 8f, timeMillis = baseTime)
        delay(150)
        // GPS remains Available
        assertEquals(GpsStatus.AVAILABLE, engine.tripState.value.gpsStatus)
    }
}
