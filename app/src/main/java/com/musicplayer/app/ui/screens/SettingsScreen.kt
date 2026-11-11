package com.musicplayer.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.app.ui.theme.AccentPalettes
import com.musicplayer.app.util.FileUtil
import com.musicplayer.app.util.TimeFormatter
import com.musicplayer.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val themeMode by settingsViewModel.themeMode.collectAsState()
    val accentColorIndex by settingsViewModel.accentColorIndex.collectAsState()
    val resumePlayback by settingsViewModel.resumePlayback.collectAsState()
    val gaplessPlayback by settingsViewModel.gaplessPlayback.collectAsState()
    val pauseOnDisconnect by settingsViewModel.pauseOnDisconnect.collectAsState()
    val rememberPosition by settingsViewModel.rememberPosition.collectAsState()
    val hideShortAudio by settingsViewModel.hideShortAudio.collectAsState()
    val notificationArtwork by settingsViewModel.notificationArtwork.collectAsState()
    val libraryStats by settingsViewModel.libraryStats.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.messageEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. APPEARANCE
            SettingsSectionHeader("Appearance")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "DARK" to "Dark", "LIGHT" to "Light").forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { settingsViewModel.setThemeMode(mode) },
                                label = { Text(label) },
                                leadingIcon = if (themeMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AccentPalettes.forEachIndexed { index, palette ->
                            val isSelected = accentColorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary)
                                    .clickable { settingsViewModel.setAccentColorIndex(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. PLAYBACK
            SettingsSectionHeader("Playback")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Resume Playback",
                        subtitle = "Remember last played track and restore position on startup",
                        checked = resumePlayback,
                        onCheckedChange = { settingsViewModel.setResumePlayback(it) }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Remember Playback Position",
                        subtitle = "Save exact playback position for paused songs",
                        checked = rememberPosition,
                        onCheckedChange = { settingsViewModel.setRememberPosition(it) }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Pause on Headphone Disconnect",
                        subtitle = "Automatically pause playback when headphones are disconnected",
                        checked = pauseOnDisconnect,
                        onCheckedChange = { settingsViewModel.setPauseOnDisconnect(it) }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Gapless Playback",
                        subtitle = "Eliminate silent gaps between consecutive audio tracks",
                        checked = gaplessPlayback,
                        onCheckedChange = { settingsViewModel.setGaplessPlayback(it) }
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Equalizer & Sound Effects",
                        subtitle = "Tune frequency bands, presets, Bass Boost, and 3D Virtualizer",
                        icon = Icons.Default.GraphicEq,
                        onClick = onOpenEqualizer
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Sleep Timer",
                        subtitle = "Automatically stop audio playback after a set duration",
                        icon = Icons.Default.Timer,
                        onClick = onOpenSleepTimer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. BACKGROUND PLAYBACK
            SettingsSectionHeader("Background Playback")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Service Status: Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "AndroidX Media3 ExoPlayer running via MediaSessionService",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Battery Usage Guidance",
                        subtitle = "Set battery usage to 'Unrestricted' in system settings to prevent OS background stops",
                        icon = Icons.Default.BatteryAlert,
                        onClick = { settingsViewModel.openBatteryOptimization(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. MUSIC LIBRARY
            SettingsSectionHeader("Music Library")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsClickableRow(
                        title = "Scan & Refresh Library",
                        subtitle = "Discover newly added audio files across device storage",
                        icon = Icons.Default.Refresh,
                        onClick = { settingsViewModel.refreshLibrary() }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Hide Short Audio Files",
                        subtitle = "Filter out voice notes and ringtones shorter than 30 seconds",
                        checked = hideShortAudio,
                        onCheckedChange = { settingsViewModel.setHideShortAudio(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. NOTIFICATIONS
            SettingsSectionHeader("Notifications")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        title = "Show Album Artwork in Notification",
                        subtitle = "Display high-resolution artwork on notification and lock screen",
                        checked = notificationArtwork,
                        onCheckedChange = { settingsViewModel.setNotificationArtwork(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. STORAGE & BACKUP
            SettingsSectionHeader("Storage & Backup")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Library Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${libraryStats.totalSongs} songs • ${FileUtil.formatFileSize(libraryStats.totalSizeBytes)} • ${TimeFormatter.formatDuration(libraryStats.totalDurationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Export Playlists",
                        subtitle = "Save playlists as JSON backup to app external files folder",
                        icon = Icons.Default.Download,
                        onClick = { settingsViewModel.exportPlaylists(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. PRIVACY
            SettingsSectionHeader("Privacy")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "100% Offline & Local Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "All playlists, favorites, and settings remain solely on your device. Zero internet connection used.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Clear Listening History",
                        subtitle = "Remove all tracks from the Recently Played history list",
                        icon = Icons.Default.Delete,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. ABOUT
            SettingsSectionHeader("About")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Music Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0 (Release)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Native Android offline audio player developed with Kotlin, Jetpack Compose, AndroidX Media3 ExoPlayer, and Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Open Source Licenses",
                        subtitle = "View software licenses and libraries",
                        icon = Icons.Default.Info,
                        onClick = { showLicensesDialog = true }
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        title = "Privacy Policy",
                        subtitle = "Zero data collection, zero network tracking",
                        icon = Icons.Default.Security,
                        onClick = { showPrivacyDialog = true }
                    )

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Developer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Md. Sobuj Ali (github.com/sobuj-here)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogs
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Listening History") },
                text = { Text("Are you sure you want to clear your recently played history? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            settingsViewModel.clearListeningHistory()
                            showClearHistoryDialog = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showLicensesDialog) {
            AlertDialog(
                onDismissRequest = { showLicensesDialog = false },
                title = { Text("Open Source Licenses") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Music Player is released under the MIT License.\n\n" +
                                    "Third-Party Components:\n" +
                                    "• AndroidX Media3 (Apache 2.0)\n" +
                                    "• Jetpack Compose (Apache 2.0)\n" +
                                    "• Room SQLite (Apache 2.0)\n" +
                                    "• DataStore (Apache 2.0)\n" +
                                    "• Coil Image Loader (Apache 2.0)\n" +
                                    "• Kotlin Standard Library (Apache 2.0)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLicensesDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy") },
                text = {
                    Text(
                        text = "Music Player is an offline-first application designed for personal use. " +
                                "It requires zero internet permissions. None of your music files, " +
                                "listening habits, playlists, or device information are ever collected, " +
                                "tracked, or transmitted outside your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
