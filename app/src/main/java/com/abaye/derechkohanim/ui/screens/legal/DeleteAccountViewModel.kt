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

data class DeleteAccountState(
    val email: String = "",
    val reason: String = "",
    /** True once the account's own address was loaded, which locks the field. */
    val emailFromAccount: Boolean = false,
    val signedIn: Boolean = false,
    val busy: Boolean = false,
    val sent: Boolean = false,
    /** Stable error code, or null. The screen owns the wording. */
    val errorCode: String? = null,
) {
    val canSubmit: Boolean get() = !busy && email.isNotBlank()
}

/**
 * Backs the deletion-request form. A signed-in user's own address is fetched and shown read-only,
 * because it is the address the server records for them regardless of what the field says.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val signedIn = sessionStore.snapshot().isSignedIn
            _state.value = _state.value.copy(signedIn = signedIn)
            if (!signedIn) return@launch

            // Best effort: a failed lookup just leaves the field empty and typeable.
            val email = (authRepository.currentUser() as? DkResult.Success)?.value?.email
            if (!email.isNullOrBlank()) {
                _state.value = _state.value.copy(email = email, emailFromAccount = true)
            }
        }
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value, errorCode = null)
    }

    fun onReasonChange(value: String) {
        _state.value = _state.value.copy(reason = value)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.value = current.copy(busy = true, errorCode = null)
        viewModelScope.launch {
            val result = authRepository.requestAccountDeletion(
                email = current.email.trim(),
                reason = current.reason.trim().ifBlank { null },
            )
            _state.value = when (result) {
                is DkResult.Success -> _state.value.copy(busy = false, sent = true)
                is DkResult.Failure -> _state.value.copy(busy = false, errorCode = result.code)
            }
        }
    }
}
