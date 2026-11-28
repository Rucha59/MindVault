package com.mindvault.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

private var askedThisProcess = false

/**
 * Draws nothing. When Home opens, asks once per app launch for the Android 13+ notification permission if it is not
 * granted and a notification kind is on. Android itself stops showing the dialog after two refusals.
 * To remove: delete the single `NotificationPermissionPrompt()` call in HomeScreen (the Profile switches still ask).
 */
@Composable
fun NotificationPermissionPrompt(viewModel: NotificationPermissionViewModel = hiltViewModel()) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (askedThisProcess) return@LaunchedEffect
        askedThisProcess = true
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted && viewModel.anyNotificationEnabled()) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}