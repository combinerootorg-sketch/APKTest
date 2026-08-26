package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DistanceUnit
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantWaitingAmber
import com.example.utils.Formatters

@Composable
fun TripTimerDisplay(
    totalDurationMillis: Long,
    movingDurationMillis: Long,
    waitingDurationMillis: Long,
    totalDistanceMeters: Double,
    distanceUnit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    // 2x2 Grid Statistics matching Elegant Dark layout
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Distance & Total Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElegantStatTile(
                label = "DISTANCE",
                value = Formatters.formatDistanceWithUnit(totalDistanceMeters, distanceUnit),
                modifier = Modifier.weight(1f)
            )

            ElegantStatTile(
                label = "TOTAL TIME",
                value = Formatters.formatDuration(totalDurationMillis),
                isMonospace = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Moving Time & Waiting Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElegantStatTile(
                label = "MOVING TIME",
                value = Formatters.formatDuration(movingDurationMillis),
                accentColor = ElegantMovingGreen,
                isMonospace = true,
                modifier = Modifier.weight(1f)
            )

            ElegantStatTile(
                label = "WAITING TIME",
                value = Formatters.formatDuration(waitingDurationMillis),
                accentColor = ElegantWaitingAmber,
                isMonospace = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ElegantStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.onSurface,
    isMonospace: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = accentColor
            )
        }
    }
}
