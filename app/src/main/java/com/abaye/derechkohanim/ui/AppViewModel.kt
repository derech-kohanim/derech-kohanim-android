package com.abaye.derechkohanim.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.nav.Routes
import javax.inject.Inject

data class AppUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val startDestination: String = Routes.HOME,
    val ready: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    val state: StateFlow<AppUiState> = sessionStore.snapshots
        .map { snapshot ->
            AppUiState(
                theme = snapshot.theme,
                startDestination = if (snapshot.onboardingComplete) Routes.HOME else Routes.ONBOARDING,
                ready = true,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())
}
