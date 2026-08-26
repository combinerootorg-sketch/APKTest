package com.example.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimeFormat
import com.example.ui.components.TripCostCard
import com.example.ui.theme.ElegantAccentGold
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.provideFactory(LocalContext.current))
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .testTag("settings_screen")
            .fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Column {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize tracking thresholds, units and display",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tracking Engine Configurations
            SettingsSectionHeader(title = "Tracking & Detection")

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Movement Speed Threshold Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Movement Speed Threshold", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f km/h", settings.movementThresholdKmh),
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                        }
                        Text(
                            text = "Speed required to transition trip from Waiting to Moving",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.movementThresholdKmh,
                            onValueChange = { viewModel.updateMovementThreshold(it) },
                            valueRange = 1f..10f,
                            steps = 17,
                            modifier = Modifier.testTag("movement_threshold_slider")
                        )
                    }

                    // Idle Detection Delay Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Idle Detection Delay", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Text(
                                text = "${settings.idleDetectionDelaySeconds}s",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                        }
                        Text(
                            text = "Grace duration vehicle stays under threshold before marking Waiting",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.idleDetectionDelaySeconds.toFloat(),
                            onValueChange = { viewModel.updateIdleDetectionDelay(it.toInt()) },
                            valueRange = 5f..60f,
                            steps = 10,
                            modifier = Modifier.testTag("idle_delay_slider")
                        )
                    }

                    // GPS Interval Chips
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPS Sampling Interval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5).forEach { interval ->
                                FilterChip(
                                    selected = settings.gpsUpdateIntervalSeconds == interval,
                                    onClick = { viewModel.updateGpsInterval(interval) },
                                    label = { Text("${interval}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Units & Display
            SettingsSectionHeader(title = "Units & Display")

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Distance Units
                    Column {
                        Text("Distance & Speed Units", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.distanceUnit == DistanceUnit.KILOMETERS,
                                onClick = { viewModel.updateDistanceUnit(DistanceUnit.KILOMETERS) },
                                label = { Text("Kilometers (km, km/h)") }
                            )
                            FilterChip(
                                selected = settings.distanceUnit == DistanceUnit.MILES,
                                onClick = { viewModel.updateDistanceUnit(DistanceUnit.MILES) },
                                label = { Text("Miles (mi, mph)") }
                            )
                        }
                    }

                    // Time Format
                    Column {
                        Text("Clock Time Format", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.timeFormat == TimeFormat.H24,
                                onClick = { viewModel.updateTimeFormat(TimeFormat.H24) },
                                label = { Text("24-Hour (14:30)") }
                            )
                            FilterChip(
                                selected = settings.timeFormat == TimeFormat.H12,
                                onClick = { viewModel.updateTimeFormat(TimeFormat.H12) },
                                label = { Text("12-Hour (2:30 PM)") }
                            )
                        }
                    }

                    // Theme
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Application Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) },
                                label = { Text("System") }
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) },
                                label = { Text("Light") }
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.updateThemeMode(ThemeMode.DARK) },
                                label = { Text("Dark") }
                            )
                        }
                    }
                }
            }

            // Trip Cost Calculator Section
            SettingsSectionHeader(title = "Trip Cost Calculator")

            TripCostSettingsCard(
                settings = settings,
                onSaveSettings = { price, economy, symbol ->
                    viewModel.updateCostCalculatorSettings(price, economy, symbol)
                },
                onClearSettings = {
                    viewModel.updateCostCalculatorSettings(0.0, 0.0, "Rs.")
                }
            )

            // Screen & Power
            SettingsSectionHeader(title = "Screen & Feedback")

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Default.Power,
                        title = "Keep Screen Awake",
                        subtitle = "Prevents device display from sleeping while a trip is active",
                        checked = settings.keepScreenAwake,
                        onCheckedChange = { viewModel.updateKeepScreenAwake(it) }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Vibration Feedback",
                        subtitle = "Vibrates briefly when moving, waiting or pausing states change",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                    )

                    SettingsSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Sound Chimes",
                        subtitle = "Plays audio beep when trip state transitions",
                        checked = settings.soundEnabled,
                        onCheckedChange = { viewModel.updateSoundEnabled(it) }
                    )
                }
            }

            // Google Drive Backup & Restore Section
            SettingsSectionHeader(title = "Backup & Restore")

            val driveConnection by viewModel.driveConnection.collectAsStateWithLifecycle()
            val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
            val isCheckingCloud by viewModel.isCheckingCloud.collectAsStateWithLifecycle()
            val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
            val cloudBackupInfo by viewModel.cloudBackupInfo.collectAsStateWithLifecycle()
            val successMessage by viewModel.backupSuccessMessage.collectAsStateWithLifecycle()
            val errorMessage by viewModel.backupErrorMessage.collectAsStateWithLifecycle()
            val showRestoreDialog by viewModel.showRestoreConfirmDialog.collectAsStateWithLifecycle()
            val showDisconnectDialog by viewModel.showDisconnectDialog.collectAsStateWithLifecycle()
            val showDeleteCloudDialog by viewModel.showDeleteCloudDialog.collectAsStateWithLifecycle()

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                viewModel.handleGoogleSignInResult(result.resultCode, result.data)
            }

            BackupRestoreCard(
                connectionState = driveConnection,
                isBackingUp = isBackingUp,
                isCheckingCloud = isCheckingCloud,
                isRestoring = isRestoring,
                cloudBackupInfo = cloudBackupInfo,
                successMessage = successMessage,
                errorMessage = errorMessage,
                showRestoreConfirmDialog = showRestoreDialog,
                showDisconnectDialog = showDisconnectDialog,
                showDeleteCloudDialog = showDeleteCloudDialog,
                onConnectGoogleDrive = {
                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                },
                onOpenDisconnectDialog = { viewModel.openDisconnectDialog() },
                onDismissDisconnectDialog = { viewModel.dismissDisconnectDialog() },
                onConfirmDisconnect = { viewModel.confirmDisconnectGoogleDrive() },
                onOpenDeleteCloudDialog = { viewModel.openDeleteCloudDialog() },
                onDismissDeleteCloudDialog = { viewModel.dismissDeleteCloudDialog() },
                onConfirmDeleteCloud = { viewModel.confirmDeleteCloudBackup() },
                onBackupNow = { viewModel.backupNow() },
                onPrepareRestore = { viewModel.prepareRestoreFromDrive() },
                onDismissRestoreDialog = { viewModel.dismissRestoreDialog() },
                onConfirmRestore = { viewModel.confirmRestore() },
                onToggleAutoBackup = { viewModel.toggleAutoBackup(it) },
                onSelectBackupFrequency = { viewModel.updateBackupFrequency(it) },
                onDismissMessage = { viewModel.clearMessages() }
            )

            // Data & Export
            SettingsSectionHeader(title = "Data & Privacy")

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.exportAllCsv() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.exportAllJson() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "100% Offline & Private",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All GPS tracks and metrics are stored strictly on your local device with zero cloud tracking or telemetry.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // About Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Trip Timer v1.0.0",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Native Android GPS Vehicle Tracking Engine",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TripCostSettingsCard(
    settings: AppSettings,
    onSaveSettings: (price: Double, economy: Double, symbol: String) -> Unit,
    onClearSettings: () -> Unit
) {
    var priceInput by remember(settings.fuelPricePerLiter) {
        mutableStateOf(if (settings.fuelPricePerLiter > 0.0) String.format(Locale.US, "%.2f", settings.fuelPricePerLiter) else "")
    }
    var economyInput by remember(settings.fuelEconomyKmPerLiter) {
        mutableStateOf(if (settings.fuelEconomyKmPerLiter > 0.0) String.format(Locale.US, "%.1f", settings.fuelEconomyKmPerLiter) else "")
    }
    var selectedSymbol by remember(settings.fuelCurrencySymbol) {
        mutableStateOf(settings.fuelCurrencySymbol.ifBlank { "Rs." })
    }
    var hasAttemptedSave by remember { mutableStateOf(false) }

    val parsedPrice = priceInput.toDoubleOrNull()
    val parsedEconomy = economyInput.toDoubleOrNull()

    val isPriceValid = parsedPrice != null && parsedPrice > 0.0
    val isEconomyValid = parsedEconomy != null && parsedEconomy > 0.0
    val isFormValid = isPriceValid && isEconomyValid

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trip_cost_settings_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElegantAccentGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = ElegantAccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Fuel Expenditure Estimation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Calculates fuel cost in real-time as trip distance increases",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Currency Symbol Chooser
            Column {
                Text(
                    text = "Currency Symbol",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Rs.", "$", "€", "£", "₹").forEach { sym ->
                        FilterChip(
                            selected = selectedSymbol == sym,
                            onClick = {
                                selectedSymbol = sym
                                if (isFormValid && parsedPrice != null && parsedEconomy != null) {
                                    onSaveSettings(parsedPrice, parsedEconomy, sym)
                                }
                            },
                            label = { Text(sym) },
                            modifier = Modifier.testTag("currency_chip_$sym")
                        )
                    }
                }
            }

            // Fuel Price Input
            Column {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { input ->
                        priceInput = input
                        val p = input.toDoubleOrNull()
                        val e = economyInput.toDoubleOrNull()
                        if (p != null && p > 0.0 && e != null && e > 0.0) {
                            onSaveSettings(p, e, selectedSymbol)
                        }
                    },
                    label = { Text("Fuel Price per litre") },
                    placeholder = { Text("e.g. 310.00") },
                    leadingIcon = {
                        Text(
                            text = selectedSymbol,
                            fontWeight = FontWeight.Bold,
                            color = ElegantAccentGold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        if (priceInput.isNotEmpty()) {
                            IconButton(onClick = {
                                priceInput = ""
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = (hasAttemptedSave || priceInput.isNotEmpty()) && !isPriceValid,
                    supportingText = {
                        if (priceInput.isNotEmpty() && !isPriceValid) {
                            Text(
                                text = "Fuel Price must be greater than zero.",
                                color = StatusErrorRed,
                                fontSize = 11.sp
                            )
                        } else {
                            Text("Current market price per 1 litre of fuel", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_price_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Fuel Economy Input
            Column {
                OutlinedTextField(
                    value = economyInput,
                    onValueChange = { input ->
                        economyInput = input
                        val p = priceInput.toDoubleOrNull()
                        val e = input.toDoubleOrNull()
                        if (p != null && p > 0.0 && e != null && e > 0.0) {
                            onSaveSettings(p, e, selectedSymbol)
                        }
                    },
                    label = { Text("Fuel Economy (km/L)") },
                    placeholder = { Text("e.g. 15.0") },
                    trailingIcon = {
                        Text(
                            text = "km/L",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = (hasAttemptedSave || economyInput.isNotEmpty()) && !isEconomyValid,
                    supportingText = {
                        if (economyInput.isNotEmpty() && !isEconomyValid) {
                            Text(
                                text = "Fuel Economy must be greater than zero.",
                                color = StatusErrorRed,
                                fontSize = 11.sp
                            )
                        } else {
                            Text("Vehicle fuel efficiency in kilometres per litre", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_economy_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Actions & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.isCostCalculatorConfigured) {
                    FilledTonalButton(
                        onClick = {
                            priceInput = ""
                            economyInput = ""
                            onClearSettings()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_fuel_settings_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset / Disable", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        hasAttemptedSave = true
                        if (isFormValid && parsedPrice != null && parsedEconomy != null) {
                            onSaveSettings(parsedPrice, parsedEconomy, selectedSymbol)
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_fuel_settings_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Live Preview Card
            Text(
                text = "PREVIEW (25.5 km TRIP)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TripCostCard(
                distanceMeters = 25500.0,
                fuelPricePerLiter = if (isPriceValid && parsedPrice != null) parsedPrice else settings.fuelPricePerLiter,
                fuelEconomyKmPerLiter = if (isEconomyValid && parsedEconomy != null) parsedEconomy else settings.fuelEconomyKmPerLiter,
                currencySymbol = selectedSymbol,
                distanceUnit = settings.distanceUnit,
                title = "Preview Calculation",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
