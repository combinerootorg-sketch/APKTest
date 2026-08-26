package com.example.ui.screens.details

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.GpsDiagnosticEvent
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.components.StatCard
import com.example.ui.components.TripCostCard
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber
import com.example.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailsViewModel = viewModel(
        factory = TripDetailsViewModel.provideFactory(tripId, LocalContext.current)
    )
) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val gpsDiagnostics by viewModel.gpsDiagnostics.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete This Trip?") },
            text = { Text("This will permanently erase all route points and statistics for Trip #${trip?.tripNumber}.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(onDeleted = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .testTag("trip_details_screen")
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (trip != null) "Trip #${trip?.tripNumber}" else "Trip Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareTripJson() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Trip"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Trip",
                            tint = StatusErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading trip details...")
            }
        } else {
            val t = trip!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Route Map
                InteractiveRouteMap(
                    points = points,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STARTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatTime(t.startTime, settings.timeFormat),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatDate(t.startTime),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ENDED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatTime(t.endTime, settings.timeFormat),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatDate(t.endTime),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grid of key statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Distance",
                        value = Formatters.formatDistanceWithUnit(t.totalDistanceMeters, settings.distanceUnit),
                        icon = Icons.Default.Navigation,
                        iconColor = PrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Time",
                        value = Formatters.formatDuration(t.totalDurationMillis),
                        icon = Icons.Default.Timer,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Moving Time",
                        value = Formatters.formatDuration(t.movingDurationMillis),
                        icon = Icons.Default.Navigation,
                        iconColor = StatusMovingGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Waiting Time",
                        value = Formatters.formatDuration(t.waitingDurationMillis),
                        icon = Icons.Default.HourglassEmpty,
                        iconColor = StatusWaitingAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Avg Speed",
                        value = Formatters.formatSpeedWithUnit(t.averageSpeedMps, settings.distanceUnit),
                        icon = Icons.Default.Speed,
                        iconColor = PrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Max Speed",
                        value = Formatters.formatSpeedWithUnit(t.maxSpeedMps, settings.distanceUnit),
                        icon = Icons.Default.TrendingUp,
                        iconColor = StatusWaitingAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Trip Cost & Fuel Calculation Card
                TripCostCard(
                    distanceMeters = t.totalDistanceMeters,
                    fuelPricePerLiter = settings.fuelPricePerLiter,
                    fuelEconomyKmPerLiter = settings.fuelEconomyKmPerLiter,
                    currencySymbol = settings.fuelCurrencySymbol,
                    distanceUnit = settings.distanceUnit,
                    title = "Trip Fuel Cost",
                    modifier = Modifier.fillMaxWidth()
                )

                // GPS Location Coordinates Card
                if (t.startLatitude != null && t.startLongitude != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "GPS COORDINATES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Start Point", fontSize = 12.sp, color = StatusMovingGreen, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${String.format("%.5f", t.startLatitude)}, ${String.format("%.5f", t.startLongitude)}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (t.endLatitude != null && t.endLongitude != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("End Point", fontSize = 12.sp, color = StatusErrorRed, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${String.format("%.5f", t.endLatitude)}, ${String.format("%.5f", t.endLongitude)}",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // GPS Diagnostics Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "GPS Diagnostics",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GPS DIAGNOSTICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (gpsDiagnostics.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(StatusMovingGreen, CircleShape)
                                )
                                Text(
                                    text = "No GPS signal loss recorded during this trip.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                gpsDiagnostics.forEachIndexed { index, event ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (gpsDiagnostics.size > 1) {
                                            Text(
                                                text = "Outage #${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusWaitingAmber
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "GPS Lost:",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = Formatters.formatTime(event.gpsLostTime, settings.timeFormat),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = StatusErrorRed
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "GPS Recovered:",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (event.gpsRecoveredTime != null) {
                                                    Formatters.formatTime(event.gpsRecoveredTime, settings.timeFormat)
                                                } else {
                                                    "Not recovered"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = StatusMovingGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        val durationMs = event.durationMillis
                                            ?: if (event.gpsRecoveredTime != null) (event.gpsRecoveredTime - event.gpsLostTime).coerceAtLeast(0L) else null

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "GPS Loss Duration:",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (durationMs != null) {
                                                    Formatters.formatDurationShort(durationMs)
                                                } else {
                                                    "—"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = StatusWaitingAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Export & Share Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.shareTripCsv() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Export CSV")
                    }

                    Button(
                        onClick = { viewModel.shareTripJson() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Share Route JSON")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
