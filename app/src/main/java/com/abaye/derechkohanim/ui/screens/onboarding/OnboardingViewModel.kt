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

/**
 * The welcome flow, in the order the web client presents it: a clean welcome, a short
 * "how it works" tour, the data source and its limits, acceptance of the server-provided
 * terms, the stringency preference, and finally location permissions and pre-downloads.
 */
enum class OnboardingStep { Welcome, HowItWorks, DataSource, Terms, Stringency, Permissions }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val stringency: Stringency = Stringency.BASIC,
    val termsAccepted: Boolean = false,
    /** Version string of the terms document fetched from the server; recorded on accept. */
    val termsVersion: String? = null,
    val termsTitle: String = "",
    /** Markdown body of the terms, as delivered by the server. Empty until loaded or if absent. */
    val termsBody: String = "",
    val termsLoading: Boolean = true,
    val finished: Boolean = false,
) {
    val stepIndex: Int get() = OnboardingStep.entries.indexOf(step)
    val stepCount: Int get() = OnboardingStep.entries.size
    val isFirst: Boolean get() = stepIndex == 0
    val isLast: Boolean get() = stepIndex == OnboardingStep.entries.lastIndex
}

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
            val doc = (terms as? DkResult.Success)?.value
            _state.value = _state.value.copy(
                stringency = snapshot.stringency,
                termsVersion = doc?.version,
                termsTitle = doc?.title.orEmpty(),
                termsBody = doc?.body.orEmpty(),
                termsLoading = false,
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
            // Records the accepted terms version and marks onboarding complete: exactly the two
            // flags DkNavGraph/AppViewModel read to decide onboarding vs home on the next launch.
            current.termsVersion?.let { sessionStore.setAcceptedTermsVersion(it) }
            sessionStore.setOnboardingComplete(true)
            _state.value = current.copy(finished = true)
        }
    }
}
