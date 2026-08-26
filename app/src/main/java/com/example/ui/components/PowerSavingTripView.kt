package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tracking.ActiveTripState
import com.example.domain.model.DistanceUnit
import com.example.domain.model.TripStatus
import com.example.ui.theme.OledAccent
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledTextDim
import com.example.ui.theme.OledTextPrimary
import com.example.utils.Formatters

@Composable
fun PowerSavingTripView(
    state: ActiveTripState,
    distanceUnit: DistanceUnit,
    keepAwake: Boolean,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onExitPowerSaving: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("power_saving_view")
            .fillMaxSize()
            .background(OledBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Trip Number, Status, GPS & Exit Power Saving
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRIP #${state.tripNumber}",
                        color = OledTextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.tripStatus.name,
                        color = when (state.tripStatus) {
                            TripStatus.MOVING -> Color(0xFF10B981)
                            TripStatus.WAITING -> Color(0xFFF59E0B)
                            TripStatus.PAUSED -> Color(0xFF94A3B8)
                            else -> OledTextDim
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GPS: ${state.gpsStatus.name}",
                        color = OledTextDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onExitPowerSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OledSurface,
                            contentColor = OledAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessLow,
                            contentDescription = "Exit Dim Mode",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit Dim", fontSize = 12.sp)
                    }
                }
            }

            // Center: Big OLED Speed & Unit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val speedValue = if (distanceUnit == DistanceUnit.KILOMETERS) state.currentSpeedMps * 3.6f else state.currentSpeedMps * 2.23694f
                Text(
                    text = String.format("%.0f", speedValue.coerceAtLeast(0f)),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = OledTextPrimary
                )
                Text(
                    text = distanceUnit.speedSymbol.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OledAccent
                )
            }

            // Middle: Distance & Timers Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OledSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("DISTANCE", fontSize = 11.sp, color = OledTextDim, fontWeight = FontWeight.Bold)
                        Text(
                            text = Formatters.formatDistanceWithUnit(state.totalDistanceMeters, distanceUnit),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = OledTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL TIME", fontSize = 11.sp, color = OledTextDim, fontWeight = FontWeight.Bold)
                        Text(
                            text = Formatters.formatDuration(state.totalDurationMillis),
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = OledTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MOVING", fontSize = 11.sp, color = OledTextDim)
                        Text(
                            text = Formatters.formatDuration(state.movingDurationMillis),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WAITING", fontSize = 11.sp, color = OledTextDim)
                        Text(
                            text = Formatters.formatDuration(state.waitingDurationMillis),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("SCREEN AWAKE", fontSize = 11.sp, color = OledTextDim)
                        Text(
                            text = if (keepAwake) "ON" else "OFF",
                            fontSize = 15.sp,
                            color = if (keepAwake) OledAccent else OledTextDim
                        )
                    }
                }
            }

            // Bottom Actions: Pause / Resume / Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.tripStatus == TripStatus.MOVING || state.tripStatus == TripStatus.WAITING) {
                    Button(
                        onClick = onPauseTrip,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OledSurface,
                            contentColor = Color(0xFFF59E0B)
                        )
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PAUSE", fontWeight = FontWeight.Bold)
                    }
                } else if (state.tripStatus == TripStatus.PAUSED) {
                    Button(
                        onClick = onResumeTrip,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OledSurface,
                            contentColor = Color(0xFF10B981)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onStopTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7F1D1D),
                        contentColor = Color(0xFFFCA5A5)
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STOP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
