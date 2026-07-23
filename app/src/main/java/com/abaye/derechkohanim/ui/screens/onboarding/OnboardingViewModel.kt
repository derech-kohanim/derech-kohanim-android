package com.abaye.derechkohanim.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.repository.AuthRepository
import javax.inject.Inject

enum class OnboardingStep { Welcome, Stringency, Terms, Permissions }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val stringency: Stringency = Stringency.BASIC,
    val termsAccepted: Boolean = false,
    val termsVersion: String? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val terms = authRepository.terms(snapshot.language)
            _state.value = _state.value.copy(
                stringency = snapshot.stringency,
                termsVersion = (terms as? DkResult.Success)?.value?.version,
            )
        }
    }

    fun onStringencySelected(stringency: Stringency) {
        _state.value = _state.value.copy(stringency = stringency)
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _state.value = _state.value.copy(termsAccepted = accepted)
    }

    fun next() {
        val current = _state.value
        val steps = OnboardingStep.entries
        val index = steps.indexOf(current.step)
        if (index < steps.lastIndex) {
            _state.value = current.copy(step = steps[index + 1])
        } else {
            finish()
        }
    }

    fun back() {
        val current = _state.value
        val steps = OnboardingStep.entries
        val index = steps.indexOf(current.step)
        if (index > 0) _state.value = current.copy(step = steps[index - 1])
    }

    private fun finish() {
        val current = _state.value
        viewModelScope.launch {
            sessionStore.setStringency(current.stringency)
            current.termsVersion?.let { sessionStore.setAcceptedTermsVersion(it) }
            sessionStore.setOnboardingComplete(true)
            _state.value = current.copy(finished = true)
        }
    }
}
