package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryCyan

@Composable
fun ScreenAwakeToggle(
    keepAwake: Boolean,
    onToggle: (Boolean) -> Unit,
    powerSavingActive: Boolean,
    onTogglePowerSaving: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Screen Awake Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggle(!keepAwake) }
        ) {
            Icon(
                imageVector = if (keepAwake) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Screen Awake",
                tint = if (keepAwake) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (keepAwake) "Screen Awake: ON" else "Screen Awake: OFF",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (keepAwake) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Power Saving / Dim screen quick button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onTogglePowerSaving(!powerSavingActive) }
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = "Power Saving Mode",
                tint = if (powerSavingActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (powerSavingActive) "Dim Mode: ON" else "Dim Mode: OFF",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (powerSavingActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
