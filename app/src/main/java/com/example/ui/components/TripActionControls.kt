package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TripStatus
import com.example.ui.theme.ElegantErrorContainer
import com.example.ui.theme.ElegantOnErrorContainer
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantSurfaceVariant

@Composable
fun TripActionControls(
    tripStatus: TripStatus,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onStopTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStopConfirmationDialog by remember { mutableStateOf(false) }

    if (showStopConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmationDialog = false },
            title = {
                Text(
                    text = "Finish and Save Trip?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will conclude your active GPS tracking session, compute all final trip statistics, and save the trip and complete route to your local database."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmationDialog = false
                        onStopTrip()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantErrorContainer,
                        contentColor = ElegantOnErrorContainer
                    ),
                    modifier = Modifier.testTag("confirm_stop_trip_button")
                ) {
                    Text("Stop & Save Trip", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStopConfirmationDialog = false },
                    modifier = Modifier.testTag("cancel_stop_trip_button")
                ) {
                    Text("Continue Tracking")
                }
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (tripStatus) {
            TripStatus.NOT_STARTED, TripStatus.STOPPED -> {
                // Big Start Trip Button in Elegant Primary
                Button(
                    onClick = onStartTrip,
                    modifier = Modifier
                        .testTag("start_trip_button")
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Trip",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START TRIP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            TripStatus.MOVING, TripStatus.WAITING -> {
                // Pause and Stop Controls matching HTML spec
                Button(
                    onClick = onPauseTrip,
                    modifier = Modifier
                        .testTag("pause_trip_button")
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantSurfaceVariant,
                        contentColor = ElegantPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAUSE",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Button(
                    onClick = { showStopConfirmationDialog = true },
                    modifier = Modifier
                        .testTag("stop_trip_button")
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantErrorContainer,
                        contentColor = ElegantOnErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            TripStatus.PAUSED -> {
                // Resume and Stop Buttons
                Button(
                    onClick = onResumeTrip,
                    modifier = Modifier
                        .testTag("resume_trip_button")
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Button(
                    onClick = { showStopConfirmationDialog = true },
                    modifier = Modifier
                        .testTag("stop_trip_button")
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantErrorContainer,
                        contentColor = ElegantOnErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
