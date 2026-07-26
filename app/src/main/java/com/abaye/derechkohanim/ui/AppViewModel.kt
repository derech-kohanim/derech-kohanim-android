package com.abaye.derechkohanim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.data.local.NavStateStore
import com.abaye.derechkohanim.data.local.PersistedNavState
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.repository.ZoneLayerStore
import com.abaye.derechkohanim.nav.NavigationEngine
import com.abaye.derechkohanim.nav.Routes
import com.abaye.derechkohanim.voice.VoicePackManager
import javax.inject.Inject

data class AppUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val startDestination: String = Routes.HOME,
    /**
     * A drive that was still running when the app was last killed. When set, the app offers to
     * resume it on launch; null means there is nothing to resume (or a drive is already running
     * in this process, in which case we go straight to the navigation screen).
     */
    val pendingResume: PersistedNavState? = null,
    val ready: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val navStateStore: NavStateStore,
    private val engine: NavigationEngine,
    voicePackManager: VoicePackManager,
    zoneLayerStore: ZoneLayerStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        // Loads the cached voice pack into the player and refreshes it in the background. The
        // endpoint has no auth, so this runs at app start whether signed in or a guest. Never
        // blocks the UI; a failure quietly leaves the previous cache (or TTS) in place.
        voicePackManager.start()

        // Same shape for the map layers: load the cached zone/danger polygons into their flows and
        // delta-sync them in the background, so Home and Navigation draw them at once and offline.
        zoneLayerStore.start()

        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            // A drive already running in this process (e.g. returning via the notification) skips
            // the resume dialog and lands straight on the navigation screen.
            val running = engine.isActive
            val persisted = if (running) null else navStateStore.load()
            _state.value = AppUiState(
                theme = snapshot.theme,
                startDestination = when {
                    running -> Routes.NAVIGATION
                    snapshot.onboardingComplete -> Routes.HOME
                    else -> Routes.ONBOARDING
                },
                pendingResume = persisted,
                ready = true,
            )
        }

        // Keep theme reactive to settings changes without disturbing the one-shot resume decision.
        viewModelScope.launch {
            sessionStore.snapshots.collect { snapshot ->
                _state.value = _state.value.copy(theme = snapshot.theme)
            }
        }
    }

    /** User chose to continue the saved drive: hand it to the engine and dismiss the dialog. */
    fun confirmResume() {
        val persisted = _state.value.pendingResume ?: return
        engine.resume(persisted)
        _state.value = _state.value.copy(pendingResume = null)
    }

    /** User declined: discard the saved drive and stay home. */
    fun discardResume() {
        _state.value = _state.value.copy(pendingResume = null)
        viewModelScope.launch { navStateStore.clear() }
    }
}
