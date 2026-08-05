package com.abaye.derechkohanim.ui.screens.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.repository.AuthRepository
import javax.inject.Inject

data class TermsState(
    val title: String = "",
    /** Markdown, as delivered by the server. Empty falls back to the bundled copy. */
    val body: String = "",
    val loading: Boolean = true,
    /** True once the fetch failed, so the screen can say the shipped copy is what is shown. */
    val usingBundledCopy: Boolean = false,
)

/**
 * Loads the current terms document for the reading screen. Same source as the onboarding step,
 * so what a user reads here is the document their acceptance is recorded against.
 */
@HiltViewModel
class TermsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TermsState())
    val state: StateFlow<TermsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val language = sessionStore.snapshot().language
            when (val result = authRepository.terms(language)) {
                is DkResult.Success -> _state.value = TermsState(
                    title = result.value.title,
                    body = result.value.body,
                    loading = false,
                    // A server document with an empty body is as much a fallback as a failure.
                    usingBundledCopy = result.value.body.isBlank(),
                )
                is DkResult.Failure -> _state.value = TermsState(
                    loading = false,
                    usingBundledCopy = true,
                )
            }
        }
    }
}
