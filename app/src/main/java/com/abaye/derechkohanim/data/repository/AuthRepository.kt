package com.abaye.derechkohanim.data.repository

import kotlinx.coroutines.flow.Flow
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.map
import com.abaye.derechkohanim.data.local.SessionSnapshot
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.AcceptTermsRequest
import com.abaye.derechkohanim.data.model.AuthResponse
import com.abaye.derechkohanim.data.model.CreateDeletionRequestRequest
import com.abaye.derechkohanim.data.model.GoogleAuthRequest
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.LoginRequest
import com.abaye.derechkohanim.data.model.LogoutRequest
import com.abaye.derechkohanim.data.model.RegisterRequest
import com.abaye.derechkohanim.data.model.TermsDocument
import com.abaye.derechkohanim.data.model.User
import com.abaye.derechkohanim.data.model.UserPreferences
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import javax.inject.Inject

interface AuthRepository {
    val session: Flow<SessionSnapshot>
    suspend fun login(email: String, password: String): DkResult<User>
    suspend fun register(
        email: String,
        password: String,
        displayName: String?,
        acceptedTermsVersion: String,
    ): DkResult<User>
    suspend fun loginWithGoogle(idToken: String, acceptedTermsVersion: String?): DkResult<User>
    suspend fun terms(language: Language): DkResult<TermsDocument>
    suspend fun acceptTerms(version: String): DkResult<User>
    /** The signed-in account. Fails for a guest, which callers treat as "no account". */
    suspend fun currentUser(): DkResult<User>
    /**
     * Asks for the account and its data to be deleted. Works for a guest too, since someone who
     * signed out still has an account to remove. Records a request; the server deletes nothing
     * until an operator has verified the requester.
     */
    suspend fun requestAccountDeletion(email: String, reason: String?): DkResult<Unit>
    /** Persists preferences to the server so settings follow the account across devices. */
    suspend fun updatePreferences(preferences: UserPreferences): DkResult<User>
    suspend fun signOut()
}

class RemoteAuthRepository @Inject constructor(
    private val api: DerechKohanimApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override val session: Flow<SessionSnapshot> = sessionStore.snapshots

    override suspend fun login(email: String, password: String): DkResult<User> =
        apiCall { api.login(LoginRequest(email, password)) }.persist()

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?,
        acceptedTermsVersion: String,
    ): DkResult<User> = apiCall {
        api.register(RegisterRequest(email, password, displayName, acceptedTermsVersion))
    }.persist()

    override suspend fun loginWithGoogle(
        idToken: String,
        acceptedTermsVersion: String?,
    ): DkResult<User> =
        apiCall { api.loginWithGoogle(GoogleAuthRequest(idToken, acceptedTermsVersion)) }.persist()

    override suspend fun terms(language: Language): DkResult<TermsDocument> =
        apiCall { api.terms(language.tag) }

    override suspend fun acceptTerms(version: String): DkResult<User> = apiCall {
        api.acceptTerms(AcceptTermsRequest(version))
    }.also {
        if (it is DkResult.Success) sessionStore.setAcceptedTermsVersion(version)
    }

    override suspend fun currentUser(): DkResult<User> = apiCall { api.currentUser() }

    override suspend fun requestAccountDeletion(email: String, reason: String?): DkResult<Unit> =
        apiCall {
            api.requestAccountDeletion(
                CreateDeletionRequestRequest(email = email, reason = reason),
            )
        }.map { }

    override suspend fun updatePreferences(preferences: UserPreferences): DkResult<User> =
        apiCall { api.updatePreferences(preferences) }

    override suspend fun signOut() {
        // Best-effort server logout, then always clear the local session.
        val refreshToken = sessionStore.snapshot().refreshToken
        if (refreshToken != null) apiCall { api.logout(LogoutRequest(refreshToken)) }
        sessionStore.clearSession()
    }

    private suspend fun DkResult<AuthResponse>.persist(): DkResult<User> {
        if (this is DkResult.Success) {
            sessionStore.setTokens(value.tokens.accessToken, value.tokens.refreshToken)
            sessionStore.setLanguage(value.user.preferences.language)
            sessionStore.setStringency(value.user.preferences.stringency)
            sessionStore.setTheme(value.user.preferences.theme)
            sessionStore.setTrafficAware(value.user.preferences.trafficAware)
            sessionStore.setVoiceEnabled(value.user.preferences.voiceEnabled)
            value.user.acceptedTermsVersion?.let { sessionStore.setAcceptedTermsVersion(it) }
        }
        return map { it.user }
    }
}
