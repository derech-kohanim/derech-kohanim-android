package com.abaye.derechkohanim.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import com.abaye.derechkohanim.data.local.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the bearer token and the interface language. Token refresh is not implemented yet -
 * a 401 simply surfaces and the session is cleared by the repository.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val snapshot = runBlocking { sessionStore.snapshot() }
        val request = chain.request().newBuilder()
            .header("Accept-Language", snapshot.language.tag)
            .apply { snapshot.accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        return chain.proceed(request)
    }
}
