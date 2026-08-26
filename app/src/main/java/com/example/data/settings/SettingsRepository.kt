package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.BackupFrequency
import com.example.domain.model.DistanceUnit
import com.example.domain.model.DriveConnectionState
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trip_timer_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val MOVEMENT_THRESHOLD_KMH = floatPreferencesKey("movement_threshold_kmh")
        val IDLE_DETECTION_DELAY_SEC = intPreferencesKey("idle_detection_delay_sec")
        val GPS_UPDATE_INTERVAL_SEC = intPreferencesKey("gps_update_interval_sec")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val POWER_SAVING_DIM_SCREEN = booleanPreferencesKey("power_saving_dim_screen")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val FUEL_PRICE_PER_LITER = doublePreferencesKey("fuel_price_per_liter")
        val FUEL_ECONOMY_KM_PER_LITER = doublePreferencesKey("fuel_economy_km_per_liter")
        val FUEL_CURRENCY_SYMBOL = stringPreferencesKey("fuel_currency_symbol")

        // Google Drive Backup & Restore Keys
        val GOOGLE_DRIVE_CONNECTED = booleanPreferencesKey("google_drive_connected")
        val GOOGLE_DRIVE_ACCOUNT_EMAIL = stringPreferencesKey("google_drive_account_email")
        val GOOGLE_DRIVE_ACCOUNT_NAME = stringPreferencesKey("google_drive_account_name")
        val GOOGLE_DRIVE_ACCESS_TOKEN = stringPreferencesKey("google_drive_access_token")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_RESTORE_TIMESTAMP = longPreferencesKey("last_restore_timestamp")
        val LAST_BACKUP_TRIP_COUNT = intPreferencesKey("last_backup_trip_count")
        val LAST_BACKUP_ROUTE_COUNT = intPreferencesKey("last_backup_route_count")
        val LAST_BACKUP_GPS_DIAGNOSTIC_COUNT = intPreferencesKey("last_backup_gps_diagnostic_count")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
    }

    val driveConnectionFlow: Flow<DriveConnectionState> = context.dataStore.data.map { preferences ->
        DriveConnectionState(
            isConnected = preferences[PreferencesKeys.GOOGLE_DRIVE_CONNECTED] ?: false,
            accountEmail = preferences[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_EMAIL] ?: "",
            accountDisplayName = preferences[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_NAME] ?: "",
            lastBackupTimestamp = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: 0L,
            lastRestoreTimestamp = preferences[PreferencesKeys.LAST_RESTORE_TIMESTAMP] ?: 0L,
            lastBackupTripCount = preferences[PreferencesKeys.LAST_BACKUP_TRIP_COUNT] ?: 0,
            lastBackupRouteCount = preferences[PreferencesKeys.LAST_BACKUP_ROUTE_COUNT] ?: 0,
            lastBackupGpsDiagnosticCount = preferences[PreferencesKeys.LAST_BACKUP_GPS_DIAGNOSTIC_COUNT] ?: 0,
            autoBackupEnabled = preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false,
            backupFrequency = preferences[PreferencesKeys.BACKUP_FREQUENCY]?.let {
                runCatching { BackupFrequency.valueOf(it) }.getOrNull()
            } ?: BackupFrequency.AFTER_COMPLETED_TRIP
        )
    }

    val googleDriveAccessTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_DRIVE_ACCESS_TOKEN] ?: ""
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            movementThresholdKmh = preferences[PreferencesKeys.MOVEMENT_THRESHOLD_KMH] ?: 2.0f,
            idleDetectionDelaySeconds = preferences[PreferencesKeys.IDLE_DETECTION_DELAY_SEC] ?: 10,
            gpsUpdateIntervalSeconds = preferences[PreferencesKeys.GPS_UPDATE_INTERVAL_SEC] ?: 2,
            distanceUnit = preferences[PreferencesKeys.DISTANCE_UNIT]?.let {
                runCatching { DistanceUnit.valueOf(it) }.getOrNull()
            } ?: DistanceUnit.KILOMETERS,
            timeFormat = preferences[PreferencesKeys.TIME_FORMAT]?.let {
                runCatching { TimeFormat.valueOf(it) }.getOrNull()
            } ?: TimeFormat.H24,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: false,
            powerSavingDimScreen = preferences[PreferencesKeys.POWER_SAVING_DIM_SCREEN] ?: false,
            vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: false,
            fuelPricePerLiter = preferences[PreferencesKeys.FUEL_PRICE_PER_LITER] ?: 0.0,
            fuelEconomyKmPerLiter = preferences[PreferencesKeys.FUEL_ECONOMY_KM_PER_LITER] ?: 0.0,
            fuelCurrencySymbol = preferences[PreferencesKeys.FUEL_CURRENCY_SYMBOL] ?: "Rs."
        )
    }

    suspend fun updateMovementThreshold(thresholdKmh: Float) {
        context.dataStore.edit { it[PreferencesKeys.MOVEMENT_THRESHOLD_KMH] = thresholdKmh }
    }

    suspend fun updateIdleDetectionDelay(delaySeconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.IDLE_DETECTION_DELAY_SEC] = delaySeconds }
    }

    suspend fun updateGpsInterval(intervalSeconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.GPS_UPDATE_INTERVAL_SEC] = intervalSeconds }
    }

    suspend fun updateDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { it[PreferencesKeys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun updateTimeFormat(format: TimeFormat) {
        context.dataStore.edit { it[PreferencesKeys.TIME_FORMAT] = format.name }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updateKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun updatePowerSavingDimScreen(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.POWER_SAVING_DIM_SCREEN] = enabled }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateFuelPricePerLiter(price: Double) {
        context.dataStore.edit { it[PreferencesKeys.FUEL_PRICE_PER_LITER] = price }
    }

    suspend fun updateFuelEconomyKmPerLiter(economy: Double) {
        context.dataStore.edit { it[PreferencesKeys.FUEL_ECONOMY_KM_PER_LITER] = economy }
    }

    suspend fun updateFuelCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[PreferencesKeys.FUEL_CURRENCY_SYMBOL] = symbol }
    }

    suspend fun updateCostCalculatorSettings(price: Double, economy: Double, symbol: String) {
        context.dataStore.edit {
            it[PreferencesKeys.FUEL_PRICE_PER_LITER] = price
            it[PreferencesKeys.FUEL_ECONOMY_KM_PER_LITER] = economy
            it[PreferencesKeys.FUEL_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun connectGoogleDrive(email: String, displayName: String, token: String = "") {
        context.dataStore.edit {
            it[PreferencesKeys.GOOGLE_DRIVE_CONNECTED] = true
            it[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_EMAIL] = email
            it[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_NAME] = displayName
            if (token.isNotEmpty()) {
                it[PreferencesKeys.GOOGLE_DRIVE_ACCESS_TOKEN] = token
            }
        }
    }

    suspend fun disconnectGoogleDrive() {
        context.dataStore.edit {
            it[PreferencesKeys.GOOGLE_DRIVE_CONNECTED] = false
            it[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_EMAIL] = ""
            it[PreferencesKeys.GOOGLE_DRIVE_ACCOUNT_NAME] = ""
            it[PreferencesKeys.GOOGLE_DRIVE_ACCESS_TOKEN] = ""
            it[PreferencesKeys.AUTO_BACKUP_ENABLED] = false
        }
    }

    suspend fun updateGoogleDriveToken(token: String) {
        context.dataStore.edit {
            it[PreferencesKeys.GOOGLE_DRIVE_ACCESS_TOKEN] = token
        }
    }

    suspend fun recordSuccessfulBackup(tripCount: Int, routeCount: Int, diagCount: Int) {
        val now = System.currentTimeMillis()
        context.dataStore.edit {
            it[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = now
            it[PreferencesKeys.LAST_BACKUP_TRIP_COUNT] = tripCount
            it[PreferencesKeys.LAST_BACKUP_ROUTE_COUNT] = routeCount
            it[PreferencesKeys.LAST_BACKUP_GPS_DIAGNOSTIC_COUNT] = diagCount
        }
    }

    suspend fun recordSuccessfulRestore() {
        val now = System.currentTimeMillis()
        context.dataStore.edit {
            it[PreferencesKeys.LAST_RESTORE_TIMESTAMP] = now
        }
    }

    suspend fun updateAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun updateBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit {
            it[PreferencesKeys.BACKUP_FREQUENCY] = frequency.name
        }
    }
}
