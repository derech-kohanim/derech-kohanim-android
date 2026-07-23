package com.abaye.derechkohanim.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.AppLocale
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.repository.AuthRepository
import javax.inject.Inject

data class SettingsState(
    val language: Language = Language.HE,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val stringency: Stringency = Stringency.BASIC,
    val trafficAware: Boolean = true,
    val voiceEnabled: Boolean = true,
    val signedIn: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = sessionStore.snapshots
        .map { snapshot ->
            SettingsState(
                language = snapshot.language,
                theme = snapshot.theme,
                stringency = snapshot.stringency,
                trafficAware = snapshot.trafficAware,
                voiceEnabled = snapshot.voiceEnabled,
                signedIn = snapshot.isSignedIn,
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

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
