package com.abaye.derechkohanim.ui.screens.auth

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

data class AuthState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val termsAccepted: Boolean = false,
    val busy: Boolean = false,
    /** True from the moment the Google button is tapped until the flow resolves. Drives the
     *  button spinner across the few seconds it takes Play Services to show its sheet. */
    val googleBusy: Boolean = false,
    val errorCode: String? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var termsVersion: String = ""

    init {
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val terms = authRepository.terms(snapshot.language)
            termsVersion = (terms as? DkResult.Success)?.value?.version.orEmpty()
            _state.value = _state.value.copy(termsAccepted = snapshot.acceptedTermsVersion != null)
        }
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, errorCode = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, errorCode = null)
    }

    fun onDisplayNameChange(value: String) {
        _state.value = _state.value.copy(displayName = value)
    }

    fun onTermsAcceptedChange(value: Boolean) {
        _state.value = _state.value.copy(termsAccepted = value)
    }

    fun login() {
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(busy = true, errorCode = null)
            apply(authRepository.login(current.email.trim(), current.password))
        }
    }

    fun register() {
        val current = _state.value
        if (!current.termsAccepted) {
            _state.value = current.copy(errorCode = "terms_required")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(busy = true, errorCode = null)
            apply(
                authRepository.register(
                    email = current.email.trim(),
                    password = current.password,
                    displayName = current.displayName.trim().ifBlank { null },
                    acceptedTermsVersion = termsVersion,
                )
            )
        }
    }

    /** Called the instant the Google button is tapped, before Play Services is invoked, so the
     *  button shows a spinner during the noticeable delay until its sheet appears. */
    fun onGoogleSignInStarted() {
        _state.value = _state.value.copy(googleBusy = true, errorCode = null)
    }

    fun loginWithGoogle(idToken: String?) {
        if (idToken == null) {
            // Cancelled or unavailable: stop the spinner. Only surface an error when Google
            // actually failed, not when the user simply dismissed the sheet.
            _state.value = _state.value.copy(googleBusy = false, errorCode = "google_unavailable")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, errorCode = null)
            apply(authRepository.loginWithGoogle(idToken, termsVersion))
        }
    }

    private fun apply(result: DkResult<*>) {
        _state.value = when (result) {
            is DkResult.Success -> _state.value.copy(busy = false, googleBusy = false, signedIn = true)
            is DkResult.Failure -> _state.value.copy(busy = false, googleBusy = false, errorCode = result.code)
        }
    }
}
