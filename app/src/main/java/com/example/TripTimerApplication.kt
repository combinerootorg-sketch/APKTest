package com.example

import android.app.Application
import android.content.Context
import com.example.data.backup.BackupRepository
import com.example.data.backup.BackupScheduler
import com.example.data.backup.GoogleDriveAuthManager
import com.example.data.backup.GoogleDriveDataSource
import com.example.data.backup.LocalBackupDataSource
import com.example.data.database.AppDatabase
import com.example.data.export.TripExportManager
import com.example.data.feedback.FeedbackManager
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.data.tracking.TripTrackingEngine
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class TripTimerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set
    lateinit var tripRepository: TripRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var exportManager: TripExportManager
        private set
    lateinit var feedbackManager: FeedbackManager
        private set
    lateinit var trackingEngine: TripTrackingEngine
        private set
    lateinit var localBackupDataSource: LocalBackupDataSource
        private set
    lateinit var googleDriveAuthManager: GoogleDriveAuthManager
        private set
    lateinit var googleDriveDataSource: GoogleDriveDataSource
        private set
    lateinit var backupRepository: BackupRepository
        private set

    var currentSettingsSnapshot: AppSettings = AppSettings()
        private set

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid for modern tile caching and memory management
        runCatching {
            val sharedPrefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            val config = Configuration.getInstance()
            config.load(this, sharedPrefs)
            config.userAgentValue = packageName
            val basePath = File(cacheDir, "osmdroid")
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = File(basePath, "tiles")
            config.isMapViewHardwareAccelerated = true
            config.cacheMapTileCount = 12
        }

        database = AppDatabase.getInstance(this)
        tripRepository = TripRepository(database.tripDao())
        settingsRepository = SettingsRepository(this)
        exportManager = TripExportManager(this)
        feedbackManager = FeedbackManager(this)
        localBackupDataSource = LocalBackupDataSource(this)
        googleDriveAuthManager = GoogleDriveAuthManager(this)
        googleDriveDataSource = GoogleDriveDataSource()
        backupRepository = BackupRepository(
            context = this,
            tripRepository = tripRepository,
            settingsRepository = settingsRepository,
            localBackupDataSource = localBackupDataSource,
            googleDriveDataSource = googleDriveDataSource,
            googleDriveAuthManager = googleDriveAuthManager
        )

        trackingEngine = TripTrackingEngine(
            context = this,
            tripRepository = tripRepository,
            settingsRepository = settingsRepository,
            feedbackManager = feedbackManager
        )

        applicationScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettingsSnapshot = settings
            }
        }

        // Listen for auto-backup settings and schedule accordingly
        applicationScope.launch {
            settingsRepository.driveConnectionFlow.collectLatest { conn ->
                BackupScheduler.scheduleBackup(
                    context = this@TripTimerApplication,
                    enabled = conn.isConnected && conn.autoBackupEnabled,
                    frequency = conn.backupFrequency
                )
            }
        }

        // Trigger after-trip auto-backup asynchronously when a trip finishes
        applicationScope.launch {
            trackingEngine.tripCompletedEvent.collectLatest { _ ->
                val conn = settingsRepository.driveConnectionFlow.first()
                if (conn.isConnected && conn.autoBackupEnabled && conn.backupFrequency == BackupFrequency.AFTER_COMPLETED_TRIP) {
                    BackupScheduler.triggerOneTimeBackup(this@TripTimerApplication)
                }
            }
        }
    }
}
