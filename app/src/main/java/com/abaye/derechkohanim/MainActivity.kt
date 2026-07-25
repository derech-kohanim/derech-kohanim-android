package com.abaye.derechkohanim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.nav.DkNavGraph
import com.abaye.derechkohanim.ui.AppViewModel
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme

/**
 * AppCompatActivity, not ComponentActivity, so per-app language switching works below
 * Android 13 (spec 14.3). The manifest supportsRtl flag handles direction for Hebrew.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    // POST_NOTIFICATIONS is declared but must be requested at runtime from Android 13+. The
    // ongoing navigation notification depends on it, so we ask once at app start; a denial only
    // means the drive runs without its notification, never that navigation fails.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            val dark = when (state.theme) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            DerechKohanimTheme(darkTheme = dark) {
                if (state.ready) {
                    DkNavGraph(
                        startDestination = state.startDestination,
                        pendingResume = state.pendingResume != null,
                        onConfirmResume = appViewModel::confirmResume,
                        onDiscardResume = appViewModel::discardResume,
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Keep the screen awake while the app is in the foreground - essential for navigation.
     * FLAG_KEEP_SCREEN_ON only holds the screen on while this window is visible; adding it on
     * resume and clearing it on pause guarantees it never keeps the display awake off-app.
     */
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
