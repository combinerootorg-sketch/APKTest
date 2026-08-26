package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryCyan

@Composable
fun PermissionExplanationDialog(
    onGrantPermissions: () -> Unit,
    onDismiss: () -> Unit,
    isPermanentlyDenied: Boolean = false,
    onOpenAppSettings: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPermanentlyDenied) "Permission Required" else "Location & Notifications",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Trip Timer tracks real-time vehicle movement, speed, distance, and routes with complete local privacy on your device.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                PermissionRowItem(
                    icon = Icons.Default.GpsFixed,
                    title = "Precise Location",
                    desc = "Calculates exact vehicle speed, travelled distance, and generates the route polyline."
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRowItem(
                    icon = Icons.Default.Notifications,
                    title = "Foreground Service & Notification",
                    desc = "Guarantees reliable GPS tracking when your screen is locked or the app is minimized."
                )

                if (isPermanentlyDenied) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Location permission has been permanently denied. Please enable it in Android App Settings to start tracking trips.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) onOpenAppSettings() else onGrantPermissions()
                },
                modifier = Modifier.testTag("grant_permissions_button")
            ) {
                Text(if (isPermanentlyDenied) "Open App Settings" else "Allow Permissions")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_permissions_button")
            ) {
                Text("Not Now")
            }
        }
    )
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
