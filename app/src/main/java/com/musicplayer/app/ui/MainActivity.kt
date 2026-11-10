package com.musicplayer.app.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.musicplayer.app.ui.screens.MainScreen
import com.musicplayer.app.ui.theme.MusicPlayerTheme
import com.musicplayer.app.util.PermissionUtil
import com.musicplayer.app.viewmodel.EqualizerViewModel
import com.musicplayer.app.viewmodel.MainViewModel
import com.musicplayer.app.viewmodel.PlaylistViewModel
import com.musicplayer.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val equalizerViewModel: EqualizerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[PermissionUtil.audioPermission] == true
        if (audioGranted || PermissionUtil.hasAudioPermission(this)) {
            mainViewModel.onPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prompt for audio access & notifications if not already granted
        checkAndRequestPermissions()

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val accentColorIndex by settingsViewModel.accentColorIndex.collectAsState()

            MusicPlayerTheme(
                themeMode = themeMode,
                accentColorIndex = accentColorIndex
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        mainViewModel = mainViewModel,
                        playlistViewModel = playlistViewModel,
                        equalizerViewModel = equalizerViewModel,
                        settingsViewModel = settingsViewModel,
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!PermissionUtil.hasAudioPermission(this)) {
            permissionsToRequest.add(PermissionUtil.audioPermission)
        }

        val notificationPerm = PermissionUtil.notificationPermission
        if (notificationPerm != null && !PermissionUtil.hasNotificationPermission(this)) {
            permissionsToRequest.add(notificationPerm)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            mainViewModel.onPermissionGranted()
        }
    }
}
