package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TripTimerApplication
import com.example.data.backup.BackupRepository
import com.example.data.backup.GoogleDriveAuthManager
import com.example.data.export.TripExportManager
import com.example.data.repository.TripRepository
import com.example.data.settings.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupFrequency
import com.example.domain.model.DistanceUnit
import com.example.domain.model.DriveBackupInfo
import com.example.domain.model.DriveConnectionState
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimeFormat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val tripRepository: TripRepository,
    private val exportManager: TripExportManager,
    private val backupRepository: BackupRepository,
    private val googleDriveAuthManager: GoogleDriveAuthManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val driveConnection: StateFlow<DriveConnectionState> = backupRepository.driveConnectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveConnectionState())

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isCheckingCloud = MutableStateFlow(false)
    val isCheckingCloud: StateFlow<Boolean> = _isCheckingCloud.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _cloudBackupInfo = MutableStateFlow<DriveBackupInfo?>(null)
    val cloudBackupInfo: StateFlow<DriveBackupInfo?> = _cloudBackupInfo.asStateFlow()

    private val _backupSuccessMessage = MutableStateFlow<String?>(null)
    val backupSuccessMessage: StateFlow<String?> = _backupSuccessMessage.asStateFlow()

    private val _backupErrorMessage = MutableStateFlow<String?>(null)
    val backupErrorMessage: StateFlow<String?> = _backupErrorMessage.asStateFlow()

    private val _showRestoreConfirmDialog = MutableStateFlow(false)
    val showRestoreConfirmDialog: StateFlow<Boolean> = _showRestoreConfirmDialog.asStateFlow()

    private val _showDisconnectDialog = MutableStateFlow(false)
    val showDisconnectDialog: StateFlow<Boolean> = _showDisconnectDialog.asStateFlow()

    private val _showDeleteCloudDialog = MutableStateFlow(false)
    val showDeleteCloudDialog: StateFlow<Boolean> = _showDeleteCloudDialog.asStateFlow()

    fun updateMovementThreshold(thresholdKmh: Float) {
        viewModelScope.launch {
            settingsRepository.updateMovementThreshold(thresholdKmh)
        }
    }

    fun updateIdleDetectionDelay(delaySec: Int) {
        viewModelScope.launch {
            settingsRepository.updateIdleDetectionDelay(delaySec)
        }
    }

    fun updateGpsInterval(intervalSec: Int) {
        viewModelScope.launch {
            settingsRepository.updateGpsInterval(intervalSec)
        }
    }

    fun updateDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            settingsRepository.updateDistanceUnit(unit)
        }
    }

    fun updateTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            settingsRepository.updateTimeFormat(format)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateKeepScreenAwake(enabled)
        }
    }

    fun updatePowerSavingDimScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePowerSavingDimScreen(enabled)
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVibrationEnabled(enabled)
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSoundEnabled(enabled)
        }
    }

    fun updateFuelPrice(price: Double) {
        viewModelScope.launch {
            settingsRepository.updateFuelPricePerLiter(price)
        }
    }

    fun updateFuelEconomy(economy: Double) {
        viewModelScope.launch {
            settingsRepository.updateFuelEconomyKmPerLiter(economy)
        }
    }

    fun updateFuelCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            settingsRepository.updateFuelCurrencySymbol(symbol)
        }
    }

    fun updateCostCalculatorSettings(price: Double, economy: Double, symbol: String) {
        viewModelScope.launch {
            settingsRepository.updateCostCalculatorSettings(price, economy, symbol)
        }
    }

    // Google Drive Backup & Restore Operations
    fun getGoogleSignInIntent(): Intent {
        return googleDriveAuthManager.getSignInIntent()
    }

    fun handleGoogleSignInResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            if (data == null) {
                _backupErrorMessage.value = "Google Sign-In cancelled."
                return@launch
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account == null) {
                    _backupErrorMessage.value = "Failed to obtain authorized Google account."
                    return@launch
                }
                val email = account.email ?: ""
                val displayName = account.displayName ?: ""

                if (email.isBlank()) {
                    _backupErrorMessage.value = "No email associated with authorized Google account."
                    return@launch
                }

                // Verify and fetch token
                val tokenRes = googleDriveAuthManager.getAccessToken(email)
                if (tokenRes.isFailure) {
                    val err = tokenRes.exceptionOrNull()
                    _backupErrorMessage.value = "Google Drive authorization failed: ${err?.localizedMessage ?: "Please grant Google Drive access permissions."}"
                    return@launch
                }

                val token = tokenRes.getOrThrow()
                backupRepository.connectGoogleDrive(email, displayName, token)
                _backupSuccessMessage.value = "Connected to Google Drive as $email"
                _backupErrorMessage.value = null
            } catch (e: ApiException) {
                val statusText = when (e.statusCode) {
                    12501 -> "Sign-in cancelled by user."
                    12500 -> "Sign-in failed. Please verify Google Play Services."
                    7 -> "Network error during Google Sign-In. Please check your connection."
                    else -> "Google Sign-In failed (Code ${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }
                _backupErrorMessage.value = statusText
            } catch (e: Exception) {
                _backupErrorMessage.value = "Google Drive authorization failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun openDisconnectDialog() {
        _showDisconnectDialog.value = true
    }

    fun dismissDisconnectDialog() {
        _showDisconnectDialog.value = false
    }

    fun confirmDisconnectGoogleDrive() {
        viewModelScope.launch {
            _showDisconnectDialog.value = false
            backupRepository.disconnectGoogleDrive()
            _backupSuccessMessage.value = "Google Drive disconnected. Local data and cloud files are safe."
            _backupErrorMessage.value = null
        }
    }

    fun openDeleteCloudDialog() {
        val conn = driveConnection.value
        if (!conn.isConnected) {
            _backupErrorMessage.value = "Connect Google Drive first."
            return
        }
        _showDeleteCloudDialog.value = true
    }

    fun dismissDeleteCloudDialog() {
        _showDeleteCloudDialog.value = false
    }

    fun confirmDeleteCloudBackup() {
        viewModelScope.launch {
            val conn = driveConnection.value
            if (!conn.isConnected) {
                _showDeleteCloudDialog.value = false
                _backupErrorMessage.value = "Connect Google Drive first."
                return@launch
            }

            _showDeleteCloudDialog.value = false
            _isBackingUp.value = true
            val result = backupRepository.deleteCloudBackup()
            _isBackingUp.value = false
            if (result.isSuccess) {
                _cloudBackupInfo.value = null
                _backupSuccessMessage.value = "Cloud backup deleted from Google Drive."
                _backupErrorMessage.value = null
            } else {
                _backupErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete cloud backup."
            }
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            val conn = driveConnection.value
            if (!conn.isConnected) {
                _backupErrorMessage.value = "Connect Google Drive first."
                return@launch
            }

            _isBackingUp.value = true
            _backupSuccessMessage.value = null
            _backupErrorMessage.value = null
            val result = backupRepository.performBackup()
            _isBackingUp.value = false
            if (result.isSuccess) {
                val info = result.getOrThrow()
                _cloudBackupInfo.value = info
                _backupSuccessMessage.value = "Backup successfully uploaded to Google Drive! (${info.tripCount} trips, ${info.routeCount} GPS points)"
            } else {
                _backupErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Backup failed. Please check network connection."
            }
        }
    }

    fun prepareRestoreFromDrive() {
        viewModelScope.launch {
            val conn = driveConnection.value
            if (!conn.isConnected) {
                _backupErrorMessage.value = "Connect Google Drive first."
                return@launch
            }

            _isCheckingCloud.value = true
            _backupSuccessMessage.value = null
            _backupErrorMessage.value = null
            val checkResult = backupRepository.getCloudBackupInfo()
            _isCheckingCloud.value = false

            if (checkResult.isSuccess) {
                val info = checkResult.getOrThrow()
                if (info != null) {
                    _cloudBackupInfo.value = info
                    _showRestoreConfirmDialog.value = true
                } else {
                    _backupErrorMessage.value = "No Trip Timer backup found in your Google Drive."
                }
            } else {
                _backupErrorMessage.value = checkResult.exceptionOrNull()?.localizedMessage ?: "Unable to access Google Drive."
            }
        }
    }

    fun dismissRestoreDialog() {
        _showRestoreConfirmDialog.value = false
    }

    fun confirmRestore() {
        viewModelScope.launch {
            val conn = driveConnection.value
            if (!conn.isConnected) {
                _showRestoreConfirmDialog.value = false
                _backupErrorMessage.value = "Connect Google Drive first."
                return@launch
            }

            _showRestoreConfirmDialog.value = false
            _isRestoring.value = true
            _backupSuccessMessage.value = null
            _backupErrorMessage.value = null

            val targetInfo = _cloudBackupInfo.value
            val result = backupRepository.performRestore(targetInfo)
            _isRestoring.value = false

            if (result.isSuccess) {
                val backup = result.getOrThrow()
                _backupSuccessMessage.value = "Restore completed successfully! Restored ${backup.trips.size} trips and ${backup.routes.size} GPS points."
            } else {
                _backupErrorMessage.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}. Original local data was preserved."
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackupEnabled(enabled)
        }
    }

    fun updateBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            settingsRepository.updateBackupFrequency(frequency)
        }
    }

    fun clearMessages() {
        _backupSuccessMessage.value = null
        _backupErrorMessage.value = null
    }

    fun exportAllCsv() {
        viewModelScope.launch {
            val all = tripRepository.allTrips.stateIn(viewModelScope).value
            if (all.isNotEmpty()) {
                val csvFile = exportManager.exportTripsToCsv(all)
                val shareIntent = exportManager.createShareIntent(csvFile, "text/csv").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export All Trips (CSV)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    fun exportAllJson() {
        viewModelScope.launch {
            val all = tripRepository.allTrips.stateIn(viewModelScope).value
            if (all.isNotEmpty()) {
                val tripsWithPoints = all.map { trip ->
                    trip to tripRepository.getPointsForTripList(trip.id)
                }
                val jsonFile = exportManager.exportTripsToJson(tripsWithPoints)
                val shareIntent = exportManager.createShareIntent(jsonFile, "application/json").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export All Trips & GPS Points (JSON)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as TripTimerApplication
                return SettingsViewModel(
                    context = app,
                    settingsRepository = app.settingsRepository,
                    tripRepository = app.tripRepository,
                    exportManager = app.exportManager,
                    backupRepository = app.backupRepository,
                    googleDriveAuthManager = app.googleDriveAuthManager
                ) as T
            }
        }
    }
}
