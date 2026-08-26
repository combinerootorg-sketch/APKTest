package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GpsStatus
import com.example.domain.model.TripStatus
import com.example.ui.theme.ElegantError
import com.example.ui.theme.ElegantErrorBg
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantMovingGreenBg
import com.example.ui.theme.ElegantPausedGray
import com.example.ui.theme.ElegantPausedGrayBg
import com.example.ui.theme.ElegantWaitingAmber
import com.example.ui.theme.ElegantWaitingAmberBg

@Composable
fun TripStatusBadge(
    status: TripStatus,
    gpsStatus: GpsStatus,
    modifier: Modifier = Modifier
) {
    val isGpsError = gpsStatus == GpsStatus.UNAVAILABLE || gpsStatus == GpsStatus.PERMISSION_REQUIRED

    val (badgeBgColor, badgeTextColor, badgeBorderColor, badgeLabel, badgeIcon) = when {
        isGpsError -> Quintuple(
            ElegantErrorBg,
            ElegantError,
            ElegantError.copy(alpha = 0.3f),
            "GPS UNAVAILABLE",
            Icons.Default.LocationSearching
        )
        status == TripStatus.MOVING -> Quintuple(
            ElegantMovingGreenBg,
            ElegantMovingGreen,
            ElegantMovingGreen.copy(alpha = 0.25f),
            "MOVING",
            Icons.Default.Navigation
        )
        status == TripStatus.WAITING -> Quintuple(
            ElegantWaitingAmberBg,
            ElegantWaitingAmber,
            ElegantWaitingAmber.copy(alpha = 0.25f),
            "WAITING",
            Icons.Default.HourglassEmpty
        )
        status == TripStatus.PAUSED -> Quintuple(
            ElegantPausedGrayBg,
            ElegantPausedGray,
            ElegantPausedGray.copy(alpha = 0.2f),
            "PAUSED",
            Icons.Default.Pause
        )
        status == TripStatus.STOPPED -> Quintuple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            "COMPLETED",
            Icons.Default.Stop
        )
        else -> Quintuple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            "READY TO TRACK",
            Icons.Default.Navigation
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (status == TripStatus.MOVING || status == TripStatus.WAITING) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .testTag("trip_status_badge")
            .clip(RoundedCornerShape(32.dp))
            .background(badgeBgColor)
            .border(1.dp, badgeBorderColor, RoundedCornerShape(32.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(badgeTextColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = badgeIcon,
            contentDescription = null,
            tint = badgeTextColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = badgeLabel,
            color = badgeTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
