package com.mapzone.mapzonealertview.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val REQUIRED = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun Context.hasLocationPermission(): Boolean = REQUIRED.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

class LocationPermissionState(
    val granted: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberLocationPermissionState(autoRequest: Boolean = true): LocationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (autoRequest && !granted) launcher.launch(REQUIRED)
    }
    return LocationPermissionState(
        granted = granted,
        request = { launcher.launch(REQUIRED) },
    )
}
