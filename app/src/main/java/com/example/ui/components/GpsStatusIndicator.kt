package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GpsStatus
import com.example.ui.theme.ElegantError
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantWaitingAmber

private data class GpsVisualState(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

@Composable
fun GpsStatusIndicator(
    gpsStatus: GpsStatus,
    modifier: Modifier = Modifier
) {
    val visualState = when (gpsStatus) {
        GpsStatus.AVAILABLE -> GpsVisualState(Icons.Default.GpsFixed, "GPS Active", ElegantMovingGreen)
        GpsStatus.WEAK -> GpsVisualState(Icons.Default.GpsNotFixed, "GPS Weak", ElegantWaitingAmber)
        GpsStatus.UNAVAILABLE -> GpsVisualState(Icons.Default.GpsOff, "GPS Unavailable", ElegantError)
        GpsStatus.PERMISSION_REQUIRED -> GpsVisualState(Icons.Default.Lock, "Permission Needed", ElegantError)
    }

    Row(
        modifier = modifier
            .testTag("gps_status_indicator")
            .clip(RoundedCornerShape(12.dp))
            .background(visualState.color.copy(alpha = 0.12f))
            .border(1.dp, visualState.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = visualState.icon,
            contentDescription = visualState.label,
            tint = visualState.color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = visualState.label,
            color = visualState.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
