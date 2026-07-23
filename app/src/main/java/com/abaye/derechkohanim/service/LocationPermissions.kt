package com.abaye.derechkohanim.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

object LocationPermissions {

    val foreground = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    const val BACKGROUND = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun hasForeground(context: Context): Boolean = foreground.any { granted(context, it) }

    fun hasBackground(context: Context): Boolean = granted(context, BACKGROUND)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Two separate requests, in order, each preceded by its own explanation in the interface.
 * Android will not grant background location in the same prompt as foreground location, and
 * asking for it cold is the fastest way to be permanently denied.
 */
class LocationPermissionState internal constructor(
    private val foregroundGranted: MutableState<Boolean>,
    private val backgroundGranted: MutableState<Boolean>,
    private val requestForeground: () -> Unit,
    private val requestBackground: () -> Unit,
) {
    val hasForeground: Boolean get() = foregroundGranted.value
    val hasBackground: Boolean get() = backgroundGranted.value

    fun askForeground() = requestForeground()
    fun askBackground() = requestBackground()
}

@Composable
fun rememberLocationPermissionState(context: Context): LocationPermissionState {
    val foregroundGranted = remember { mutableStateOf(LocationPermissions.hasForeground(context)) }
    val backgroundGranted = remember { mutableStateOf(LocationPermissions.hasBackground(context)) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        foregroundGranted.value = result.values.any { it }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundGranted.value = granted
    }

    return remember {
        LocationPermissionState(
            foregroundGranted = foregroundGranted,
            backgroundGranted = backgroundGranted,
            requestForeground = { foregroundLauncher.launch(LocationPermissions.foreground) },
            requestBackground = { backgroundLauncher.launch(LocationPermissions.BACKGROUND) },
        )
    }
}
