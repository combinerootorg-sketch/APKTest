package com.example.data.tracking

import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.example.data.feedback.FeedbackManager
import com.example.data.location.FusedLocationClientImpl
import com.example.data.location.LocationClient
import com.example.data.location.LocationResultWrapper
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.GpsStatus
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.domain.model.TripStatus
import com.example.utils.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActiveTripState(
    val tripStatus: TripStatus = TripStatus.NOT_STARTED,
    val gpsStatus: GpsStatus = GpsStatus.AVAILABLE,
    val tripNumber: Int = 1,
    val startTimeMillis: Long = 0L,
    val currentSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val averageSpeedMps: Float = 0f,
    val totalDistanceMeters: Double = 0.0,
    val movingDurationMillis: Long = 0L,
    val waitingDurationMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val lastLocation: Location? = null,
    val routePoints: List<TripPoint> = emptyList(),
    val isTracking: Boolean = false
)

class TripTrackingEngine(
    private val context: Context,
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository,
    private val locationClient: LocationClient = FusedLocationClientImpl(context),
    private val feedbackManager: FeedbackManager = FeedbackManager(context)
) {

    companion object {
        private const val TAG = "TripTimerGPS"

        // Robust GPS Status thresholds based on time elapsed since the last valid location update
        // 0–8s: GPS Active (or Weak if accuracy > 25m)
        const val GPS_ACTIVE_THRESHOLD_MS = 8_000L
        // 8–20s (up to 25s): GPS Weak / Searching grace period
        const val GPS_WEAK_THRESHOLD_MS = 25_000L
        // > 25s without valid location: GPS Unavailable

        const val ACCURACY_WEAK_THRESHOLD_METERS = 25.0f
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()

    private val _tripState = MutableStateFlow(ActiveTripState())
    val tripState: StateFlow<ActiveTripState> = _tripState.asStateFlow()

    private val _tripCompletedEvent = MutableSharedFlow<Trip>(extraBufferCapacity = 1)
    val tripCompletedEvent: SharedFlow<Trip> = _tripCompletedEvent.asSharedFlow()

    private var locationJob: Job? = null
    private var timerJob: Job? = null

    // Timing tracking with monotonic time to prevent clock drift/skips
    private var lastTickElapsedRealtime: Long = 0L
    private var belowThresholdSinceElapsedRealtime: Long? = null
    private var lastRecordedPointLocation: Location? = null
    private var currentSettings: AppSettings = AppSettings()

    // GPS Health tracking state
    private var trackingStartTimeElapsedRealtime: Long = 0L
    private var lastValidLocationElapsedRealtime: Long = 0L
    private var lastValidLocationWallClock: Long = 0L
    private var lastValidLocationAccuracy: Float = 0f
    private var lastKnownLocationAvailability: Boolean = true
    private var isPermissionDenied: Boolean = false

    // GPS Diagnostics Outage tracking
    private val activeGpsDiagnosticEvents = mutableListOf<GpsDiagnosticEvent>()
    private var currentGpsLossStartTimeWallClock: Long? = null

    init {
        engineScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
            }
        }
    }

    private fun handleGpsStatusLossCheck(gpsStatus: GpsStatus, nowWallClock: Long, isTracking: Boolean, tripStatus: TripStatus) {
        if (!isTracking || tripStatus == TripStatus.STOPPED || tripStatus == TripStatus.NOT_STARTED) {
            return
        }
        if (gpsStatus == GpsStatus.UNAVAILABLE || gpsStatus == GpsStatus.PERMISSION_REQUIRED) {
            if (currentGpsLossStartTimeWallClock == null) {
                currentGpsLossStartTimeWallClock = nowWallClock
                Log.d(TAG, "GPS Diagnostic: Signal loss detected at timestamp=$nowWallClock")
            }
        }
    }

    private fun handleGpsRecoveryCheck(nowWallClock: Long) {
        val lossStart = currentGpsLossStartTimeWallClock
        if (lossStart != null) {
            val duration = (nowWallClock - lossStart).coerceAtLeast(0L)
            activeGpsDiagnosticEvents.add(
                GpsDiagnosticEvent(
                    tripId = 0,
                    gpsLostTime = lossStart,
                    gpsRecoveredTime = nowWallClock,
                    durationMillis = duration
                )
            )
            currentGpsLossStartTimeWallClock = null
            Log.d(TAG, "GPS Diagnostic: Signal recovered at timestamp=$nowWallClock, duration=${duration}ms")
        }
    }

    suspend fun startTrip() = stateMutex.withLock {
        if (_tripState.value.tripStatus != TripStatus.NOT_STARTED &&
            _tripState.value.tripStatus != TripStatus.STOPPED
        ) {
            return@withLock // Already active or paused
        }

        val nextNumber = tripRepository.getNextTripNumber()
        val nowWallClock = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        lastTickElapsedRealtime = nowElapsed
        belowThresholdSinceElapsedRealtime = null
        lastRecordedPointLocation = null
        trackingStartTimeElapsedRealtime = nowElapsed
        lastValidLocationElapsedRealtime = 0L
        lastValidLocationWallClock = 0L
        lastValidLocationAccuracy = 0f
        lastKnownLocationAvailability = true
        isPermissionDenied = false
        activeGpsDiagnosticEvents.clear()
        currentGpsLossStartTimeWallClock = null

        val previousStatus = _tripState.value.tripStatus
        val initialStatus = TripStatus.WAITING // Initially waiting until speed >= movement threshold

        _tripState.value = ActiveTripState(
            tripStatus = initialStatus,
            gpsStatus = GpsStatus.AVAILABLE,
            tripNumber = nextNumber,
            startTimeMillis = nowWallClock,
            currentSpeedMps = 0f,
            maxSpeedMps = 0f,
            averageSpeedMps = 0f,
            totalDistanceMeters = 0.0,
            movingDurationMillis = 0L,
            waitingDurationMillis = 0L,
            totalDurationMillis = 0L,
            routePoints = emptyList(),
            isTracking = true
        )

        feedbackManager.onTripStatusChanged(
            oldStatus = previousStatus,
            newStatus = initialStatus,
            vibrationEnabled = currentSettings.vibrationEnabled,
            soundEnabled = currentSettings.soundEnabled
        )

        startTimerTicker()
        startLocationUpdates()
    }

    suspend fun pauseTrip() = stateMutex.withLock {
        val currentStatus = _tripState.value.tripStatus
        if (currentStatus != TripStatus.MOVING && currentStatus != TripStatus.WAITING) {
            return@withLock
        }

        val oldStatus = _tripState.value.tripStatus
        _tripState.update { it.copy(tripStatus = TripStatus.PAUSED, currentSpeedMps = 0f) }

        feedbackManager.onTripStatusChanged(
            oldStatus = oldStatus,
            newStatus = TripStatus.PAUSED,
            vibrationEnabled = currentSettings.vibrationEnabled,
            soundEnabled = currentSettings.soundEnabled
        )
    }

    suspend fun resumeTrip() = stateMutex.withLock {
        if (_tripState.value.tripStatus != TripStatus.PAUSED) {
            return@withLock
        }

        lastTickElapsedRealtime = SystemClock.elapsedRealtime()
        belowThresholdSinceElapsedRealtime = null

        val oldStatus = _tripState.value.tripStatus
        val newStatus = TripStatus.WAITING

        _tripState.update { it.copy(tripStatus = newStatus) }

        feedbackManager.onTripStatusChanged(
            oldStatus = oldStatus,
            newStatus = newStatus,
            vibrationEnabled = currentSettings.vibrationEnabled,
            soundEnabled = currentSettings.soundEnabled
        )
    }

    suspend fun stopTrip(): Trip? = stateMutex.withLock {
        val state = _tripState.value
        if (state.tripStatus == TripStatus.NOT_STARTED || state.tripStatus == TripStatus.STOPPED) {
            return@withLock null
        }

        stopLocationUpdates()
        stopTimerTicker()

        val oldStatus = state.tripStatus
        val endWallClock = System.currentTimeMillis()
        val points = state.routePoints
        val startPoint = points.firstOrNull()
        val endPoint = points.lastOrNull()

        val completedTrip = Trip(
            tripNumber = state.tripNumber,
            title = "Trip #${state.tripNumber}",
            startTime = state.startTimeMillis,
            endTime = endWallClock,
            totalDurationMillis = state.totalDurationMillis,
            movingDurationMillis = state.movingDurationMillis,
            waitingDurationMillis = state.waitingDurationMillis,
            totalDistanceMeters = state.totalDistanceMeters,
            averageSpeedMps = state.averageSpeedMps,
            maxSpeedMps = state.maxSpeedMps,
            startLatitude = startPoint?.latitude ?: state.lastLocation?.latitude,
            startLongitude = startPoint?.longitude ?: state.lastLocation?.longitude,
            endLatitude = endPoint?.latitude ?: state.lastLocation?.latitude,
            endLongitude = endPoint?.longitude ?: state.lastLocation?.longitude,
            isCompleted = true
        )

        // Finalize any open GPS outage at trip conclusion
        handleGpsRecoveryCheck(endWallClock)

        // Save to Room database with recorded GPS diagnostic events
        tripRepository.saveCompletedTrip(completedTrip, points, activeGpsDiagnosticEvents.toList())

        _tripState.value = state.copy(
            tripStatus = TripStatus.STOPPED,
            isTracking = false,
            currentSpeedMps = 0f
        )

        feedbackManager.onTripStatusChanged(
            oldStatus = oldStatus,
            newStatus = TripStatus.STOPPED,
            vibrationEnabled = currentSettings.vibrationEnabled,
            soundEnabled = currentSettings.soundEnabled
        )

        _tripCompletedEvent.tryEmit(completedTrip)
        return@withLock completedTrip
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (isActive) {
                delay(1000L)
                tickTimer()
            }
        }
    }

    private fun stopTimerTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun tickTimer() = stateMutex.withLock {
        val state = _tripState.value
        if (!state.isTracking || state.tripStatus == TripStatus.PAUSED ||
            state.tripStatus == TripStatus.STOPPED || state.tripStatus == TripStatus.NOT_STARTED
        ) {
            return@withLock
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val elapsedDelta = (nowElapsed - lastTickElapsedRealtime).coerceAtLeast(0L)
        lastTickElapsedRealtime = nowElapsed

        val newMoving = if (state.tripStatus == TripStatus.MOVING) {
            state.movingDurationMillis + elapsedDelta
        } else {
            state.movingDurationMillis
        }

        val newWaiting = if (state.tripStatus == TripStatus.WAITING) {
            state.waitingDurationMillis + elapsedDelta
        } else {
            state.waitingDurationMillis
        }

        val newTotal = newMoving + newWaiting

        // Average speed based on moving duration (or total duration if moving is 0)
        val avgSpeed = if (newMoving > 0) {
            (state.totalDistanceMeters / (newMoving / 1000.0)).toFloat()
        } else if (newTotal > 0) {
            (state.totalDistanceMeters / (newTotal / 1000.0)).toFloat()
        } else {
            0f
        }

        // Re-evaluate GPS health periodically so signal loss transitions through WEAK -> UNAVAILABLE
        val currentGpsStatus = computeGpsStatus(nowElapsed)
        handleGpsStatusLossCheck(currentGpsStatus, System.currentTimeMillis(), state.isTracking, state.tripStatus)

        _tripState.update {
            it.copy(
                movingDurationMillis = newMoving,
                waitingDurationMillis = newWaiting,
                totalDurationMillis = newTotal,
                averageSpeedMps = avgSpeed,
                gpsStatus = currentGpsStatus
            )
        }
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = engineScope.launch {
            val interval = currentSettings.gpsUpdateIntervalSeconds
            locationClient.getLocationUpdates(interval).collect { wrapper ->
                processLocationUpdate(wrapper)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    private fun computeGpsStatus(nowElapsed: Long): GpsStatus {
        if (isPermissionDenied) {
            return GpsStatus.PERMISSION_REQUIRED
        }

        // If no valid location update received yet
        if (lastValidLocationElapsedRealtime == 0L) {
            val timeSinceStart = if (trackingStartTimeElapsedRealtime > 0L) {
                nowElapsed - trackingStartTimeElapsedRealtime
            } else {
                0L
            }
            return if (timeSinceStart <= GPS_WEAK_THRESHOLD_MS) {
                GpsStatus.AVAILABLE // Initial startup grace period
            } else {
                if (!lastKnownLocationAvailability) GpsStatus.UNAVAILABLE else GpsStatus.WEAK
            }
        }

        val timeSinceLastValid = nowElapsed - lastValidLocationElapsedRealtime

        return when {
            timeSinceLastValid <= GPS_ACTIVE_THRESHOLD_MS -> {
                if (lastValidLocationAccuracy > ACCURACY_WEAK_THRESHOLD_METERS) {
                    GpsStatus.WEAK
                } else {
                    GpsStatus.AVAILABLE
                }
            }
            timeSinceLastValid <= GPS_WEAK_THRESHOLD_MS -> {
                // Grace period: GPS Weak / Searching rather than immediate Unavailable
                GpsStatus.WEAK
            }
            else -> {
                // Sustained period (> 25s) without a valid location update
                GpsStatus.UNAVAILABLE
            }
        }
    }

    private suspend fun processLocationUpdate(wrapper: LocationResultWrapper) = stateMutex.withLock {
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWallClock = System.currentTimeMillis()
        val state = _tripState.value

        if (wrapper.isPermissionDenied) {
            isPermissionDenied = true
            handleGpsStatusLossCheck(GpsStatus.PERMISSION_REQUIRED, nowWallClock, state.isTracking, state.tripStatus)
            _tripState.update { it.copy(gpsStatus = GpsStatus.PERMISSION_REQUIRED) }
            return@withLock
        }

        if (wrapper.isLocationAvailable != null) {
            lastKnownLocationAvailability = wrapper.isLocationAvailable
            val timeSinceLastValid = if (lastValidLocationElapsedRealtime > 0L) {
                nowElapsed - lastValidLocationElapsedRealtime
            } else {
                -1L
            }
            Log.d(
                TAG,
                "LocationAvailability callback: isLocationAvailable=${wrapper.isLocationAvailable}, " +
                        "timeSinceLastValidLocation=${timeSinceLastValid}ms, " +
                        "tripStatus=${state.tripStatus}, " +
                        "currentGpsStatus=${state.gpsStatus}"
            )
        }

        val location = wrapper.location
        if (location == null) {
            val evaluatedGpsStatus = computeGpsStatus(nowElapsed)
            handleGpsStatusLossCheck(evaluatedGpsStatus, nowWallClock, state.isTracking, state.tripStatus)
            _tripState.update { it.copy(gpsStatus = evaluatedGpsStatus) }
            return@withLock
        }

        isPermissionDenied = false
        val accuracy = if (location.hasAccuracy()) location.accuracy else 15f

        val prevLocation = lastRecordedPointLocation
        val isValid = GeoUtils.isValidGpsPoint(
            accuracyMeters = accuracy,
            previousLat = prevLocation?.latitude,
            previousLon = prevLocation?.longitude,
            previousTimeMillis = prevLocation?.time,
            currentLat = location.latitude,
            currentLon = location.longitude,
            currentTimeMillis = location.time
        )

        if (!isValid) {
            val evaluatedGpsStatus = computeGpsStatus(nowElapsed)
            handleGpsStatusLossCheck(evaluatedGpsStatus, nowWallClock, state.isTracking, state.tripStatus)
            _tripState.update { it.copy(gpsStatus = evaluatedGpsStatus) }
            return@withLock
        }

        // Valid location received - check if recovering from GPS loss
        handleGpsRecoveryCheck(nowWallClock)

        // Calculate time since previous valid location
        val timeSincePrevValid = if (lastValidLocationElapsedRealtime > 0L) {
            nowElapsed - lastValidLocationElapsedRealtime
        } else {
            0L
        }

        lastValidLocationElapsedRealtime = nowElapsed
        lastValidLocationWallClock = nowWallClock
        lastValidLocationAccuracy = accuracy

        // Calculate speed in m/s
        val currentSpeedMps = if (location.hasSpeed() && location.speed >= 0f) {
            location.speed
        } else if (prevLocation != null) {
            val dist = GeoUtils.calculateDistanceMeters(
                prevLocation.latitude,
                prevLocation.longitude,
                location.latitude,
                location.longitude
            )
            val timeSec = ((location.time - prevLocation.time).coerceAtLeast(1000L)) / 1000.0
            (dist / timeSec).toFloat()
        } else {
            0f
        }

        // Added distance delta
        var addedDistance = 0.0
        if (prevLocation != null) {
            val d = GeoUtils.calculateDistanceMeters(
                prevLocation.latitude,
                prevLocation.longitude,
                location.latitude,
                location.longitude
            )
            // Filter stationary GPS drift jitter (< 1.5 meters without velocity)
            if (d >= 1.5 || currentSpeedMps > 0.5f) {
                addedDistance = d
            }
        }

        val newTotalDistance = state.totalDistanceMeters + addedDistance
        val newMaxSpeed = maxOf(state.maxSpeedMps, currentSpeedMps)

        // Threshold check (convert km/h threshold from settings to m/s: 1 km/h = 1/3.6 m/s)
        val thresholdMps = currentSettings.movementThresholdKmh / 3.6f
        val idleDelayMillis = currentSettings.idleDetectionDelaySeconds * 1000L

        var nextTripStatus = state.tripStatus

        if (state.isTracking && state.tripStatus != TripStatus.PAUSED && state.tripStatus != TripStatus.STOPPED) {
            if (currentSpeedMps >= thresholdMps) {
                // Speed meets or exceeds threshold -> Moving immediately
                belowThresholdSinceElapsedRealtime = null
                nextTripStatus = TripStatus.MOVING
            } else {
                // Speed is below threshold
                if (belowThresholdSinceElapsedRealtime == null) {
                    belowThresholdSinceElapsedRealtime = nowElapsed
                }
                val idleDuration = nowElapsed - (belowThresholdSinceElapsedRealtime ?: nowElapsed)
                if (idleDuration >= idleDelayMillis) {
                    nextTripStatus = TripStatus.WAITING
                }
            }
        }

        if (nextTripStatus != state.tripStatus && state.tripStatus != TripStatus.PAUSED && state.tripStatus != TripStatus.STOPPED) {
            feedbackManager.onTripStatusChanged(
                oldStatus = state.tripStatus,
                newStatus = nextTripStatus,
                vibrationEnabled = currentSettings.vibrationEnabled,
                soundEnabled = currentSettings.soundEnabled
            )
        }

        val newGpsStatus = computeGpsStatus(nowElapsed)

        // Development diagnostic logging without GPS coordinates
        Log.d(
            TAG,
            "Valid location received: timestamp=$nowWallClock, " +
                    "accuracy=${accuracy}m, " +
                    "speed=${currentSpeedMps}m/s, " +
                    "timeSincePrevValid=${timeSincePrevValid}ms, " +
                    "locationAvailability=$lastKnownLocationAvailability, " +
                    "tripStatus=$nextTripStatus, " +
                    "gpsStatus=$newGpsStatus"
        )

        // Add route point if location moved noticeably or every few points
        val shouldAddPoint = prevLocation == null || addedDistance >= 3.0 ||
                (nowWallClock - (state.routePoints.lastOrNull()?.timestamp ?: 0L) >= 4000L)

        val updatedPoints = if (shouldAddPoint && state.isTracking && state.tripStatus != TripStatus.PAUSED) {
            val newPoint = TripPoint(
                sequenceNumber = state.routePoints.size + 1,
                timestamp = nowWallClock,
                latitude = location.latitude,
                longitude = location.longitude,
                speedMps = currentSpeedMps,
                accuracyMeters = accuracy,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                bearingDegrees = if (location.hasBearing()) location.bearing else null,
                status = nextTripStatus
            )
            state.routePoints + newPoint
        } else {
            state.routePoints
        }

        lastRecordedPointLocation = location

        _tripState.update {
            it.copy(
                tripStatus = nextTripStatus,
                gpsStatus = newGpsStatus,
                currentSpeedMps = currentSpeedMps,
                maxSpeedMps = newMaxSpeed,
                totalDistanceMeters = newTotalDistance,
                lastLocation = location,
                routePoints = updatedPoints
            )
        }
    }

    fun updateGpsStatus(status: GpsStatus) {
        _tripState.update { it.copy(gpsStatus = status) }
    }

    fun reset() {
        stopLocationUpdates()
        stopTimerTicker()
        activeGpsDiagnosticEvents.clear()
        currentGpsLossStartTimeWallClock = null
        _tripState.value = ActiveTripState()
    }
}
