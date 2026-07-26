package com.abaye.derechkohanim.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.RefreshRequest
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Refreshes an expired access token on a 401 and retries the request once. The stored access
 * token is short-lived (server `accessTokenTtlSeconds`), while the refresh token lives for days.
 * Without this a persisted session keeps sending an expired bearer, so every authed call fails
 * with 401 even though the user is "still signed in". On a spent/invalid refresh token the local
 * session is cleared so the interface can prompt a fresh sign-in.
 *
 * [api] is injected as a Provider to break the DI cycle (api -> retrofit -> okhttp -> this).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: Provider<DerechKohanimApi>,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never try to refresh the refresh call itself, and give up after one retry.
        if (response.request.url.encodedPath.endsWith("auth/refresh")) return null
        if (responseCount(response) >= 2) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(lock) {
            val current = runBlocking { sessionStore.snapshot() }
            val currentToken = current.accessToken
            // Another request already refreshed while this one was in flight: just retry with the
            // newer token instead of refreshing again (and rotating the refresh token twice).
            if (currentToken != null && currentToken != failedToken) {
                return response.request.retryWith(currentToken)
            }

            val refreshToken = current.refreshToken ?: return null
            val tokens = runCatching {
                runBlocking { api.get().refresh(RefreshRequest(refreshToken)) }
            }.getOrNull() ?: run {
                // Refresh token is spent or invalid: drop the dead session.
                runBlocking { sessionStore.clearSession() }
                return null
            }

            runBlocking { sessionStore.setTokens(tokens.accessToken, tokens.refreshToken) }
            return response.request.retryWith(tokens.accessToken)
        }
    }

    private fun Request.retryWith(accessToken: String): Request =
        newBuilder().header("Authorization", "Bearer $accessToken").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
