package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BackupFrequency
import com.example.domain.model.DriveBackupInfo
import com.example.domain.model.DriveConnectionState
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreCard(
    connectionState: DriveConnectionState,
    isBackingUp: Boolean,
    isCheckingCloud: Boolean,
    isRestoring: Boolean,
    cloudBackupInfo: DriveBackupInfo?,
    successMessage: String?,
    errorMessage: String?,
    showRestoreConfirmDialog: Boolean,
    showDisconnectDialog: Boolean,
    showDeleteCloudDialog: Boolean,
    onConnectGoogleDrive: () -> Unit,
    onOpenDisconnectDialog: () -> Unit,
    onDismissDisconnectDialog: () -> Unit,
    onConfirmDisconnect: () -> Unit,
    onOpenDeleteCloudDialog: () -> Unit,
    onDismissDeleteCloudDialog: () -> Unit,
    onConfirmDeleteCloud: () -> Unit,
    onBackupNow: () -> Unit,
    onPrepareRestore: () -> Unit,
    onDismissRestoreDialog: () -> Unit,
    onConfirmRestore: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onSelectBackupFrequency: (BackupFrequency) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("backup_restore_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Connection Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (connectionState.isConnected) PrimaryCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (connectionState.isConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (connectionState.isConnected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Drive Cloud Backup",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (connectionState.isConnected) "Connected & Authorized" else "Offline (Not Connected)",
                            fontSize = 12.sp,
                            color = if (connectionState.isConnected) StatusMovingGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Small Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (connectionState.isConnected) StatusMovingGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (connectionState.isConnected) "Connected" else "Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connectionState.isConnected) StatusMovingGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Message Banners
            AnimatedVisibility(visible = successMessage != null, enter = fadeIn(), exit = fadeOut()) {
                if (successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusMovingGreen.copy(alpha = 0.12f))
                            .clickable { onDismissMessage() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusMovingGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = successMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusErrorRed.copy(alpha = 0.12f))
                            .clickable { onDismissMessage() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (!connectionState.isConnected) {
                // Not Connected State: Description & Connect Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sign in with your Google Account to authorize Trip Timer to safely store backups on your Google Drive.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onConnectGoogleDrive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_google_drive_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Google Drive", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Connected State: Account Card, Backup Stats, Action Buttons & Auto-Backup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Account info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = connectionState.accountDisplayName.ifBlank { "Google Account" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = connectionState.accountEmail,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenDisconnectDialog,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("disconnect_drive_button")
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 11.sp)
                        }
                    }

                    // Metadata info (Last Backup / Last Restore)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Cloud Backup:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatTimestamp(connectionState.lastBackupTimestamp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (connectionState.lastBackupTimestamp > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (connectionState.lastBackupTimestamp > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Backed Up Data:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${connectionState.lastBackupTripCount} trips • ${connectionState.lastBackupRouteCount} GPS points",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryCyan
                                    )
                                }
                            }

                            if (connectionState.lastRestoreTimestamp > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Last Restored:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatTimestamp(connectionState.lastRestoreTimestamp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons (Backup Now & Restore)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onBackupNow,
                            enabled = !isBackingUp && !isRestoring && !isCheckingCloud,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backing up...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledTonalButton(
                            onClick = onPrepareRestore,
                            enabled = !isBackingUp && !isRestoring && !isCheckingCloud,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_drive_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCheckingCloud || isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRestoring) "Restoring..." else "Checking...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Delete Cloud Backup Button
                    TextButton(
                        onClick = onOpenDeleteCloudDialog,
                        enabled = !isBackingUp && !isRestoring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_cloud_backup_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Backup from Google Drive", color = StatusErrorRed, fontSize = 12.sp)
                    }
                }

                // Automatic Backup Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Automatic Backup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Automatically sync trips in the background", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = connectionState.autoBackupEnabled,
                            onCheckedChange = onToggleAutoBackup,
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }

                    AnimatedVisibility(visible = connectionState.autoBackupEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Backup Schedule Frequency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BackupFrequency.entries.forEach { freq ->
                                    FilterChip(
                                        selected = connectionState.backupFrequency == freq,
                                        onClick = { onSelectBackupFrequency(freq) },
                                        label = {
                                            Text(
                                                text = when (freq) {
                                                    BackupFrequency.AFTER_COMPLETED_TRIP -> "After Trip"
                                                    BackupFrequency.DAILY -> "Daily"
                                                    BackupFrequency.WEEKLY -> "Weekly"
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = onDismissDisconnectDialog,
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null, tint = StatusWaitingAmber) },
            title = { Text("Disconnect Google Drive?") },
            text = {
                Text(
                    "Disconnecting will stop automatic synchronization. Your local trips and any existing cloud backup in Google Drive will remain safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusWaitingAmber)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDisconnectDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Cloud Backup Confirmation Dialog
    if (showDeleteCloudDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteCloudDialog,
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusErrorRed) },
            title = { Text("Delete Cloud Backup?") },
            text = {
                Text(
                    "This will permanently delete 'TripTimer_Backup.json' from your Google Drive. Your local trip data on this phone will NOT be affected."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteCloud,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed)
                ) {
                    Text("Delete Cloud File")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteCloudDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && cloudBackupInfo != null) {
        RestoreConfirmationDialog(
            info = cloudBackupInfo,
            onDismiss = onDismissRestoreDialog,
            onConfirm = onConfirmRestore
        )
    }
}

@Composable
private fun RestoreConfirmationDialog(
    info: DriveBackupInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusWaitingAmber) },
        title = { Text("Restore Trips from Google Drive?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "A valid cloud backup was found on your Google Drive. Restoring will replace the current local database with this backup.",
                    fontSize = 13.sp
                )

                // Backup Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Backup Date:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimestamp(info.createdAt.takeIf { it > 0 } ?: info.modifiedTime), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Trips:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${info.tripCount} trips", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GPS Route Points:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${info.routeCount} points", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GPS Diagnostics:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${info.gpsDiagnosticCount} events", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusWaitingAmber.copy(alpha = 0.15f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusWaitingAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Safety snapshot is created automatically before restoring so no data can be lost.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                modifier = Modifier.testTag("confirm_restore_button")
            ) {
                Text("Confirm Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Never"
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
