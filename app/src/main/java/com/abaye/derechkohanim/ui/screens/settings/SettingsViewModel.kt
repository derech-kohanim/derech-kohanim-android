package com.abaye.derechkohanim.ui.screens.settings

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.repository.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.AppLocale
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.MapProvider
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.model.VoiceIds
import com.abaye.derechkohanim.data.repository.AuthRepository
import com.abaye.derechkohanim.data.repository.ZoneLayerStatus
import com.abaye.derechkohanim.data.repository.ZoneLayerStore
import com.abaye.derechkohanim.voice.VoicePackManager
import com.abaye.derechkohanim.voice.VoicePackStatus
import javax.inject.Inject

data class SettingsState(
    val language: Language = Language.HE,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val stringency: Stringency = Stringency.BASIC,
    val trafficAware: Boolean = true,
    val voiceEnabled: Boolean = true,
    /** True when the feminine Hebrew voice is chosen. */
    val voiceFemale: Boolean = false,
    val avoidDangerZones: Boolean = true,
    val avoidTolls: Boolean = false,
    val mapProvider: MapProvider = MapProvider.AUTOMATIC,
    val signedIn: Boolean = false,
    val voicePack: VoicePackStatus = VoicePackStatus(),
    val zoneLayer: ZoneLayerStatus = ZoneLayerStatus(),
    /** How many places the account has saved; null for a guest or before it loads. */
    val savedPlaceCount: Int? = null,
    val profileName: String? = null,
    val profileEmail: String? = null,
    val profileAvatarUrl: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val voicePackManager: VoicePackManager,
    private val zoneLayerStore: ZoneLayerStore,
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    /** Loaded whenever the session becomes signed in; cleared on sign-out. */
    private val savedPlaceCount = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            sessionStore.snapshots.map { it.isSignedIn }.distinctUntilChanged().collect { signedIn ->
                // Refresh the stored profile (a newer Google photo, a renamed account).
                if (signedIn) {
                    (authRepository.currentUser() as? DkResult.Success)?.value?.let { user ->
                        sessionStore.setProfile(user.displayName, user.email, user.avatarUrl)
                    }
                }
                savedPlaceCount.value = if (signedIn) {
                    (placesRepository.savedPlaces() as? DkResult.Success)?.value?.size
                } else {
                    null
                }
            }
        }
    }

    val state: StateFlow<SettingsState> =
        combine(
            sessionStore.snapshots,
            voicePackManager.status,
            zoneLayerStore.status,
            savedPlaceCount,
        ) { snapshot, voicePack, zoneLayer, savedCount ->
            SettingsState(
                language = snapshot.language,
                theme = snapshot.theme,
                stringency = snapshot.stringency,
                trafficAware = snapshot.trafficAware,
                voiceEnabled = snapshot.voiceEnabled,
                voiceFemale = VoiceIds.isFemale(snapshot.voiceId),
                avoidDangerZones = snapshot.avoidDangerZones,
                avoidTolls = snapshot.avoidTolls,
                mapProvider = snapshot.mapProvider,
                signedIn = snapshot.isSignedIn,
                voicePack = voicePack,
                zoneLayer = zoneLayer,
                savedPlaceCount = savedCount,
                profileName = snapshot.profileName,
                profileEmail = snapshot.profileEmail,
                profileAvatarUrl = snapshot.profileAvatarUrl,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setLanguage(language: Language) {
        viewModelScope.launch { sessionStore.setLanguage(language) }
        // Applied immediately so the layout direction flips without waiting for a restart.
        AppLocale.apply(language)
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { sessionStore.setTheme(theme) }
    }

    fun setStringency(stringency: Stringency) {
        viewModelScope.launch { sessionStore.setStringency(stringency) }
    }

    fun setTrafficAware(enabled: Boolean) {
        viewModelScope.launch { sessionStore.setTrafficAware(enabled) }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch { sessionStore.setVoiceEnabled(enabled) }
    }

    /**
     * Switches between the male and female Hebrew speaker. Each is its own recorded pack, so the
     * pack for the new voice is fetched right away rather than at the next periodic refresh.
     */
    fun setVoiceFemale(female: Boolean) {
        viewModelScope.launch {
            sessionStore.setVoiceId(if (female) VoiceIds.HE_FEMALE else VoiceIds.HE_MALE)
            voicePackManager.refreshNow()
        }
    }

    fun setAvoidDangerZones(enabled: Boolean) {
        viewModelScope.launch { sessionStore.setAvoidDangerZones(enabled) }
    }

    fun setAvoidTolls(enabled: Boolean) {
        viewModelScope.launch { sessionStore.setAvoidTolls(enabled) }
    }

    fun setMapProvider(provider: MapProvider) {
        viewModelScope.launch { sessionStore.setMapProvider(provider) }
    }

    fun downloadVoicePack() {
        voicePackManager.refreshNow()
    }

    fun downloadZoneLayers() {
        zoneLayerStore.refreshNow()
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
