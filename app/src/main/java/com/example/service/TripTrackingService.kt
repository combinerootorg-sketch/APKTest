package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.TripTimerApplication
import com.example.data.tracking.ActiveTripState
import com.example.domain.model.TripStatus
import com.example.utils.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TripTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "trip_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? TripTimerApplication
        val engine = app?.trackingEngine

        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                observeTripState()
                serviceScope.launch {
                    engine?.startTrip()
                }
            }
            ACTION_PAUSE -> {
                serviceScope.launch {
                    engine?.pauseTrip()
                }
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    engine?.resumeTrip()
                }
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    engine?.stopTrip()
                    stopForegroundAndSelf()
                }
            }
            else -> {
                // If restarted by system
                startForegroundWithNotification()
                observeTripState()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val initialNotification = buildNotification(
            ActiveTripState(tripStatus = TripStatus.MOVING, tripNumber = 1)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    private fun observeTripState() {
        stateObserverJob?.cancel()
        val app = application as? TripTimerApplication ?: return
        val engine = app.trackingEngine

        stateObserverJob = serviceScope.launch {
            engine.tripState.collectLatest { state ->
                if (state.tripStatus == TripStatus.STOPPED) {
                    stopForegroundAndSelf()
                } else if (state.isTracking) {
                    val notification = buildNotification(state)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun buildNotification(state: ActiveTripState): Notification {
        val app = application as? TripTimerApplication
        val settings = app?.currentSettingsSnapshot

        val distanceUnit = settings?.distanceUnit ?: com.example.domain.model.DistanceUnit.KILOMETERS
        val distStr = Formatters.formatDistanceWithUnit(state.totalDistanceMeters, distanceUnit)
        val speedStr = Formatters.formatSpeedWithUnit(state.currentSpeedMps, distanceUnit)
        val durationStr = Formatters.formatDuration(state.totalDurationMillis)

        val statusText = when (state.tripStatus) {
            TripStatus.MOVING -> "Moving • $speedStr"
            TripStatus.WAITING -> "Waiting • Idle"
            TripStatus.PAUSED -> "Paused"
            TripStatus.STOPPED -> "Trip Finished"
            TripStatus.NOT_STARTED -> "Ready"
        }

        val contentText = "$distStr | Time: $durationStr"

        // Main content click opens MainActivity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Trip #${state.tripNumber} — $statusText")
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(state.tripStatus != TripStatus.PAUSED)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Action Buttons: Pause/Resume, Stop
        if (state.tripStatus == TripStatus.MOVING || state.tripStatus == TripStatus.WAITING) {
            val pauseIntent = Intent(this, TripTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
        } else if (state.tripStatus == TripStatus.PAUSED) {
            val resumeIntent = Intent(this, TripTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            )
        }

        // Stop Action
        val stopIntent = Intent(this, TripTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopPendingIntent
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.trip_tracking_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.trip_tracking_notification_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        runCatching {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TripTimer:TrackingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // Safe 12 hour max timeout
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        }
    }

    private fun stopForegroundAndSelf() {
        stateObserverJob?.cancel()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stateObserverJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
