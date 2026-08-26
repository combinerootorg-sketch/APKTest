package com.example.ui.screens.summary

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.InteractiveRouteMap
import com.example.ui.components.StatCard
import com.example.ui.components.TripCostCard
import com.example.ui.screens.details.TripDetailsViewModel
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber
import com.example.utils.Formatters

@Composable
fun TripSummaryScreen(
    tripId: Long,
    onDone: () -> Unit,
    onViewFullDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailsViewModel = viewModel(
        factory = TripDetailsViewModel.provideFactory(tripId, LocalContext.current)
    )
) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .testTag("trip_summary_screen")
            .fillMaxSize()
    ) { innerPadding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading summary...")
            }
        } else {
            val t = trip!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Success Icon & Trip Completed Banner
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(StatusMovingGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = StatusMovingGreen,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Text(
                    text = "Trip Completed!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Trip #${t.tripNumber} • ${Formatters.formatDate(t.startTime)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Hero Distance & Duration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL DISTANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDistanceWithUnit(t.totalDistanceMeters, settings.distanceUnit),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("TOTAL TIME", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = Formatters.formatDuration(t.totalDurationMillis),
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MOVING", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = Formatters.formatDuration(t.movingDurationMillis),
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusMovingGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("WAITING", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = Formatters.formatDuration(t.waitingDurationMillis),
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWaitingAmber
                                )
                            }
                        }
                    }
                }

                // Speed Statistics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Average Speed",
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

                // Final Trip Cost & Fuel Calculation
                TripCostCard(
                    distanceMeters = t.totalDistanceMeters,
                    fuelPricePerLiter = settings.fuelPricePerLiter,
                    fuelEconomyKmPerLiter = settings.fuelEconomyKmPerLiter,
                    currencySymbol = settings.fuelCurrencySymbol,
                    distanceUnit = settings.distanceUnit,
                    title = "Trip Fuel Cost",
                    modifier = Modifier.fillMaxWidth()
                )

                // Mini Map Route Preview
                if (points.isNotEmpty()) {
                    InteractiveRouteMap(
                        points = points,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.shareTripJson() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .testTag("summary_done_button")
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
