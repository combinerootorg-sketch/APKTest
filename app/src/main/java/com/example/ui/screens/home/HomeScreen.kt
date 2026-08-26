package com.example.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.GpsStatus
import com.example.domain.model.TripStatus
import com.example.ui.components.PermissionExplanationDialog
import com.example.ui.components.PowerSavingTripView
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TripActionControls
import com.example.ui.components.TripCostCard
import com.example.ui.components.TripStatusBadge
import com.example.ui.components.TripTimerDisplay
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantWaitingAmber
import com.example.utils.Formatters

@Composable
fun HomeScreen(
    onNavigateToSummary: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    // Screen Keep-Awake Window Handling
    val activity = context as? Activity
    DisposableEffect(settings.keepScreenAwake, tripState.isTracking, tripState.tripStatus) {
        val shouldKeepAwake = settings.keepScreenAwake &&
                tripState.isTracking &&
                tripState.tripStatus != TripStatus.PAUSED &&
                tripState.tripStatus != TripStatus.STOPPED

        if (shouldKeepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle completed trip navigation
    LaunchedEffect(Unit) {
        viewModel.navigateToSummary.collect { completedTrip ->
            onNavigateToSummary(completedTrip.id)
        }
    }

    // Permission Check & Request Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.updateGpsStatus(GpsStatus.AVAILABLE)
            viewModel.startTrip()
        } else {
            viewModel.updateGpsStatus(GpsStatus.PERMISSION_REQUIRED)
            isPermanentlyDenied = true
        }
    }

    fun checkAndStartTrip() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            viewModel.startTrip()
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        PermissionExplanationDialog(
            onGrantPermissions = {
                showPermissionDialog = false
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            onDismiss = { showPermissionDialog = false },
            isPermanentlyDenied = isPermanentlyDenied,
            onOpenAppSettings = {
                showPermissionDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    // If power saving is enabled and trip is active, render OLED Power Saving View
    if (settings.powerSavingDimScreen && tripState.isTracking) {
        PowerSavingTripView(
            state = tripState,
            distanceUnit = settings.distanceUnit,
            keepAwake = settings.keepScreenAwake,
            onPauseTrip = { viewModel.pauseTrip() },
            onResumeTrip = { viewModel.resumeTrip() },
            onStopTrip = { viewModel.stopTrip() },
            onExitPowerSaving = { viewModel.setPowerSaving(false) }
        )
    } else {
        Scaffold(
            modifier = modifier
                .testTag("home_screen")
                .fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Elegant Top Status Bar (GPS + Screen Awake + Trip Info)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GPS Status with dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (tripState.gpsStatus == GpsStatus.AVAILABLE)
                                    ElegantMovingGreen.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tripState.gpsStatus == GpsStatus.AVAILABLE) ElegantMovingGreen else ElegantWaitingAmber
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tripState.gpsStatus == GpsStatus.AVAILABLE) "GPS: ACTIVE" else "GPS: ${tripState.gpsStatus.name}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (tripState.gpsStatus == GpsStatus.AVAILABLE) ElegantMovingGreen else ElegantWaitingAmber
                        )
                    }

                    // Screen Awake & Dim Mode Quick Pills
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.setKeepScreenAwake(!settings.keepScreenAwake) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AWAKE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (settings.keepScreenAwake) ElegantPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (settings.keepScreenAwake) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = ElegantWaitingAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Text(
                            text = if (tripState.isTracking) "TRIP #${tripState.tripNumber}" else "OFFLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Speedometer Gauge (Hero Speed Readout)
                SpeedometerGauge(
                    currentSpeedMps = tripState.currentSpeedMps,
                    maxSpeedMps = tripState.maxSpeedMps,
                    averageSpeedMps = tripState.averageSpeedMps,
                    unit = settings.distanceUnit
                )

                // Status Indicator Pill (Moving, Waiting, Paused)
                TripStatusBadge(
                    status = tripState.tripStatus,
                    gpsStatus = tripState.gpsStatus
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Statistics 2x2 Grid (Distance, Total Time, Moving Time, Waiting Time)
                TripTimerDisplay(
                    totalDurationMillis = tripState.totalDurationMillis,
                    movingDurationMillis = tripState.movingDurationMillis,
                    waitingDurationMillis = tripState.waitingDurationMillis,
                    totalDistanceMeters = tripState.totalDistanceMeters,
                    distanceUnit = settings.distanceUnit
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Trip Cost Calculator Card (Independent presentación layer)
                TripCostCard(
                    distanceMeters = tripState.totalDistanceMeters,
                    fuelPricePerLiter = settings.fuelPricePerLiter,
                    fuelEconomyKmPerLiter = settings.fuelEconomyKmPerLiter,
                    currencySymbol = settings.fuelCurrencySymbol,
                    distanceUnit = settings.distanceUnit,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Controls (PAUSE in dark grey / STOP in soft coral)
                TripActionControls(
                    tripStatus = tripState.tripStatus,
                    onStartTrip = { checkAndStartTrip() },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onStopTrip = { viewModel.stopTrip() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
