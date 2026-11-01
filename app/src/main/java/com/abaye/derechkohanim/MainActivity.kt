package com.abaye.derechkohanim

import android.content.Intent
import com.abaye.derechkohanim.nav.NavigationEngine
import com.abaye.derechkohanim.data.model.NavigationStatus
import kotlinx.coroutines.flow.MutableStateFlow
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.abaye.derechkohanim.analytics.Analytics
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.nav.DkNavGraph
import com.abaye.derechkohanim.service.MapProviderState
import com.abaye.derechkohanim.ui.AppViewModel
import com.abaye.derechkohanim.ui.components.LocalMapProviderState
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import javax.inject.Inject

/**
 * AppCompatActivity, not ComponentActivity, so per-app language switching works below
 * Android 13 (spec 14.3). The manifest supportsRtl flag handles direction for Hebrew.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    // Resolves the effective map provider (Google vs OpenFreeMap) for the whole app; exposed to
    // DkMap through a CompositionLocal.
    @Inject
    lateinit var mapProviderState: MapProviderState

    // GA4 (Measurement Protocol) client, used here to record screen_view for the main screens as the
    // nav back stack changes. A complete no-op unless the GA ids are configured at build time.
    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var navigationEngine: NavigationEngine

    /** Bumped each time the navigation notification is tapped while a drive is live. */
    private val openNavigationRequests = MutableStateFlow(0)

    // POST_NOTIFICATIONS is declared but must be requested at runtime from Android 13+. The
    // ongoing navigation notification depends on it, so we ask once at app start; a denial only
    // means the drive runs without its notification, never that navigation fails.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleOpenNavigation(intent)
        setContent {
            val openNavigation by openNavigationRequests.collectAsStateWithLifecycle()
            val state by appViewModel.state.collectAsStateWithLifecycle()
            val dark = when (state.theme) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            DerechKohanimTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalMapProviderState provides mapProviderState) {
                    if (state.ready) {
                        DkNavGraph(
                            startDestination = state.startDestination,
                            pendingResume = state.pendingResume != null,
                            onConfirmResume = appViewModel::confirmResume,
                            onDiscardResume = appViewModel::discardResume,
                            onScreen = analytics::screen,
                            openNavigationRequest = openNavigation,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenNavigation(intent)
    }

    /** The navigation notification asks for the live drive: open it only while one is running. */
    private fun handleOpenNavigation(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NAVIGATION, false) != true) return
        intent.removeExtra(EXTRA_OPEN_NAVIGATION)
        val status = navigationEngine.state.value.status
        val live = status == NavigationStatus.NAVIGATING || status == NavigationStatus.REROUTING
        if (live) openNavigationRequests.value += 1
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

    companion object {
        /** Set on the navigation notification's tap intent. */
        const val EXTRA_OPEN_NAVIGATION = "open_navigation"
    }
}
