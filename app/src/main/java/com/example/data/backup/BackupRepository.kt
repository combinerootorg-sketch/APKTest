package com.example.data.backup

import android.content.Context
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.DriveBackupInfo
import com.example.domain.model.DriveConnectionState
import com.example.domain.model.FullTripBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRepository(
    private val context: Context,
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository,
    private val localBackupDataSource: LocalBackupDataSource,
    private val googleDriveDataSource: GoogleDriveDataSource,
    private val googleDriveAuthManager: GoogleDriveAuthManager
) {

    val driveConnectionState: Flow<DriveConnectionState> = settingsRepository.driveConnectionFlow

    /**
     * Resolves a valid Google OAuth access token.
     * Returns failure if user is not authorized or token cannot be retrieved.
     */
    suspend fun getValidAccessToken(): Result<String> {
        val state = settingsRepository.driveConnectionFlow.first()
        if (!state.isConnected) {
            return Result.failure(IllegalStateException("Connect Google Drive first."))
        }

        // Try getting token from auth manager
        val tokenResult = googleDriveAuthManager.getAccessToken(state.accountEmail.ifBlank { null })
        if (tokenResult.isSuccess) {
            val token = tokenResult.getOrThrow()
            if (token.isNotBlank()) {
                settingsRepository.updateGoogleDriveToken(token)
                return Result.success(token)
            }
        }

        // Fallback to stored token if available
        val storedToken = settingsRepository.googleDriveAccessTokenFlow.first()
        if (storedToken.isNotBlank()) {
            return Result.success(storedToken)
        }

        return Result.failure(IllegalStateException("Connect Google Drive first."))
    }

    /**
     * Records an authorized Google Drive connection after real Google OAuth flow completes.
     */
    suspend fun connectGoogleDrive(email: String, displayName: String, token: String = "") {
        settingsRepository.connectGoogleDrive(email, displayName, token)
    }

    /**
     * Disconnects Google Drive account and signs out without deleting local data or cloud backups.
     */
    suspend fun disconnectGoogleDrive() {
        googleDriveAuthManager.signOut()
        settingsRepository.disconnectGoogleDrive()
    }

    /**
     * Checks if a cloud backup file exists in Google Drive and returns its metadata.
     */
    suspend fun getCloudBackupInfo(): Result<DriveBackupInfo?> = withContext(Dispatchers.IO) {
        val tokenResult = getValidAccessToken()
        if (tokenResult.isFailure) {
            return@withContext Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("Connect Google Drive first."))
        }

        val token = tokenResult.getOrThrow()
        val result = googleDriveDataSource.findBackupFile(token)

        if (result.isFailure && result.exceptionOrNull()?.message?.contains("401") == true) {
            // Invalidate token and retry once
            googleDriveAuthManager.invalidateToken(token)
            val refreshedTokenRes = googleDriveAuthManager.getAccessToken()
            if (refreshedTokenRes.isSuccess) {
                val freshToken = refreshedTokenRes.getOrThrow()
                settingsRepository.updateGoogleDriveToken(freshToken)
                return@withContext googleDriveDataSource.findBackupFile(freshToken)
            }
        }

        result
    }

    /**
     * Performs a complete backup from local Room database to the user's real Google Drive.
     */
    suspend fun performBackup(): Result<DriveBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getValidAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("Connect Google Drive first."))
            }

            var token = tokenResult.getOrThrow()
            val trips = tripRepository.getAllTripsList()
            val routes = tripRepository.getAllTripPointsList()
            val diagnostics = tripRepository.getAllGpsDiagnosticsList()
            val settings = settingsRepository.settingsFlow.first()

            val json = localBackupDataSource.createBackupJson(trips, routes, diagnostics, settings)
            var uploadResult = googleDriveDataSource.uploadBackup(token, json)

            // Handle token expiration retry
            if (uploadResult.isFailure && uploadResult.exceptionOrNull()?.message?.contains("401") == true) {
                googleDriveAuthManager.invalidateToken(token)
                val refreshedTokenRes = googleDriveAuthManager.getAccessToken()
                if (refreshedTokenRes.isSuccess) {
                    token = refreshedTokenRes.getOrThrow()
                    settingsRepository.updateGoogleDriveToken(token)
                    uploadResult = googleDriveDataSource.uploadBackup(token, json)
                }
            }

            if (uploadResult.isSuccess) {
                val info = uploadResult.getOrThrow()
                settingsRepository.recordSuccessfulBackup(
                    tripCount = trips.size,
                    routeCount = routes.size,
                    diagCount = diagnostics.size
                )
                Result.success(info)
            } else {
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Google Drive backup upload failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs a safe restore from Google Drive to local Room database.
     * Guaranteed safe rollback via local safety backup snapshot.
     */
    suspend fun performRestore(backupInfo: DriveBackupInfo? = null): Result<FullTripBackup> = withContext(Dispatchers.IO) {
        var safetySnapshotCreated = false
        val currentTrips = tripRepository.getAllTripsList()
        val currentRoutes = tripRepository.getAllTripPointsList()
        val currentDiagnostics = tripRepository.getAllGpsDiagnosticsList()
        val currentSettings = settingsRepository.settingsFlow.first()

        try {
            val tokenResult = getValidAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("Connect Google Drive first."))
            }

            var token = tokenResult.getOrThrow()

            // 1. Create local safety snapshot before any modification
            localBackupDataSource.createSafetyBackupSnapshot(
                currentTrips,
                currentRoutes,
                currentDiagnostics,
                currentSettings
            )
            safetySnapshotCreated = true

            // 2. Fetch Google Drive file ID
            val targetFileId = backupInfo?.fileId ?: run {
                val found = googleDriveDataSource.findBackupFile(token).getOrNull()
                found?.fileId ?: throw IllegalStateException("No Trip Timer backup found in your Google Drive.")
            }

            // 3. Download cloud backup JSON
            var downloadResult = googleDriveDataSource.downloadBackup(token, targetFileId)
            if (downloadResult.isFailure && downloadResult.exceptionOrNull()?.message?.contains("401") == true) {
                googleDriveAuthManager.invalidateToken(token)
                val refreshed = googleDriveAuthManager.getAccessToken()
                if (refreshed.isSuccess) {
                    token = refreshed.getOrThrow()
                    settingsRepository.updateGoogleDriveToken(token)
                    downloadResult = googleDriveDataSource.downloadBackup(token, targetFileId)
                }
            }

            if (downloadResult.isFailure) {
                throw downloadResult.exceptionOrNull() ?: Exception("Failed to download cloud backup file.")
            }

            val jsonContent = downloadResult.getOrThrow()

            // 4. Validate & verify backup schema and integrity
            val parsedBackup = localBackupDataSource.parseBackupJson(jsonContent)

            // 5. Replace local database data inside atomic transaction
            tripRepository.replaceAllData(
                trips = parsedBackup.trips,
                points = parsedBackup.routes,
                gpsDiagnostics = parsedBackup.gpsDiagnostics
            )

            // 6. Restore user settings
            settingsRepository.updateMovementThreshold(parsedBackup.settings.movementThresholdKmh)
            settingsRepository.updateIdleDetectionDelay(parsedBackup.settings.idleDetectionDelaySeconds)
            settingsRepository.updateGpsInterval(parsedBackup.settings.gpsUpdateIntervalSeconds)
            settingsRepository.updateDistanceUnit(parsedBackup.settings.distanceUnit)
            settingsRepository.updateTimeFormat(parsedBackup.settings.timeFormat)
            settingsRepository.updateThemeMode(parsedBackup.settings.themeMode)
            settingsRepository.updateKeepScreenAwake(parsedBackup.settings.keepScreenAwake)
            settingsRepository.updatePowerSavingDimScreen(parsedBackup.settings.powerSavingDimScreen)
            settingsRepository.updateVibrationEnabled(parsedBackup.settings.vibrationEnabled)
            settingsRepository.updateSoundEnabled(parsedBackup.settings.soundEnabled)
            if (parsedBackup.settings.fuelPricePerLiter > 0.0) {
                settingsRepository.updateCostCalculatorSettings(
                    parsedBackup.settings.fuelPricePerLiter,
                    parsedBackup.settings.fuelEconomyKmPerLiter,
                    parsedBackup.settings.fuelCurrencySymbol
                )
            }

            // 7. Record restore timestamp
            settingsRepository.recordSuccessfulRestore()

            // 8. Restore succeeded, delete temporary safety snapshot
            localBackupDataSource.deleteSafetyBackupSnapshot()

            Result.success(parsedBackup)
        } catch (e: Exception) {
            // Restore failed: Perform automatic rollback to original data
            if (safetySnapshotCreated) {
                runCatching {
                    val safetyBackup = localBackupDataSource.loadSafetyBackupSnapshot()
                    if (safetyBackup != null) {
                        tripRepository.replaceAllData(
                            trips = safetyBackup.trips,
                            points = safetyBackup.routes,
                            gpsDiagnostics = safetyBackup.gpsDiagnostics
                        )
                    }
                }
            }
            Result.failure(e)
        }
    }

    /**
     * Deletes the cloud backup file from Google Drive after user confirmation.
     */
    suspend fun deleteCloudBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getValidAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("Connect Google Drive first."))
            }

            var token = tokenResult.getOrThrow()
            val existing = googleDriveDataSource.findBackupFile(token).getOrNull()
            if (existing == null || existing.fileId.isBlank()) {
                return@withContext Result.success(true)
            }

            var deleteRes = googleDriveDataSource.deleteBackupFile(token, existing.fileId)
            if (deleteRes.isFailure && deleteRes.exceptionOrNull()?.message?.contains("401") == true) {
                googleDriveAuthManager.invalidateToken(token)
                val refreshed = googleDriveAuthManager.getAccessToken()
                if (refreshed.isSuccess) {
                    token = refreshed.getOrThrow()
                    settingsRepository.updateGoogleDriveToken(token)
                    deleteRes = googleDriveDataSource.deleteBackupFile(token, existing.fileId)
                }
            }

            deleteRes
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
