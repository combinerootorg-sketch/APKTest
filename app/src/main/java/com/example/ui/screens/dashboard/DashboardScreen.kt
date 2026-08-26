package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber
import com.example.utils.Formatters
import java.util.Locale

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.provideFactory(LocalContext.current))
) {
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val chartData by viewModel.recentTripsChartData.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .testTag("dashboard_screen")
            .fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Header Title
            Column {
                Text(
                    text = "Dashboard & Analytics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "All-time local tracking metrics",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Big Summary Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL DISTANCE LOGGED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatDistanceWithUnit(stats.totalDistanceMeters, settings.distanceUnit),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL TRIPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${stats.totalTrips}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DRIVING HOURS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%.1f hrs", stats.totalDrivingHours),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusMovingGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOP SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatSpeedWithUnit(stats.highestRecordedSpeedMps, settings.distanceUnit),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusWaitingAmber
                            )
                        }
                    }
                }
            }

            // Recent Trips Distance Chart
            SimpleBarChart(
                title = "Recent Trips Distance",
                data = chartData,
                unitLabel = "Distance (${settings.distanceUnit.unitSymbol})",
                barColor = PrimaryCyan
            )

            // Grid of Detailed Statistics
            Text(
                text = "Performance Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Avg Distance",
                    value = Formatters.formatDistanceWithUnit(stats.averageDistanceMeters, settings.distanceUnit),
                    icon = Icons.Default.Straighten,
                    iconColor = PrimaryCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg Duration",
                    value = Formatters.formatDuration(stats.averageDurationMillis),
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
                    title = "Longest Trip",
                    value = Formatters.formatDistanceWithUnit(stats.longestTripDistanceMeters, settings.distanceUnit),
                    icon = Icons.Default.TrendingUp,
                    iconColor = StatusMovingGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Shortest Trip",
                    value = Formatters.formatDistanceWithUnit(stats.shortestTripDistanceMeters, settings.distanceUnit),
                    icon = Icons.Default.Route,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Moving Time",
                    value = Formatters.formatDuration(stats.totalMovingDurationMillis),
                    icon = Icons.Default.Navigation,
                    iconColor = StatusMovingGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Idle/Waiting",
                    value = Formatters.formatDuration(stats.totalWaitingDurationMillis),
                    icon = Icons.Default.HourglassEmpty,
                    iconColor = StatusWaitingAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
