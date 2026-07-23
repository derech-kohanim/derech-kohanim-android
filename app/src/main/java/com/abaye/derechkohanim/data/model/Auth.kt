package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors derech-kohanim-shared/src/types/auth.ts. */

@Serializable
enum class UserTier {
    @SerialName("guest") GUEST,
    @SerialName("registered") REGISTERED,
    @SerialName("verified") VERIFIED,
}

@Serializable
enum class AuthProvider {
    @SerialName("password") PASSWORD,
    @SerialName("google") GOOGLE,
    @SerialName("phone") PHONE,
}

@Serializable
data class UserPreferences(
    val language: Language,
    val theme: ThemePreference,
    val stringency: Stringency,
    val trafficAware: Boolean,
    val voiceEnabled: Boolean,
    /** Voice pack identifier, e.g. "he-default". */
    val voiceId: String,
    val units: String = "metric",
)

@Serializable
data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val tier: UserTier,
    val providers: List<AuthProvider> = emptyList(),
    val preferences: UserPreferences,
    /** Version of the terms the user accepted, null until accepted. */
    val acceptedTermsVersion: String? = null,
    val createdAt: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
    val acceptedTermsVersion: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** Google Identity Services credential, exchanged server side. */
@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val acceptedTermsVersion: String? = null,
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Seconds until the access token expires. */
    val expiresIn: Long,
)

@Serializable
data class AuthResponse(
    val user: User,
    val tokens: AuthTokens,
)

@Serializable
data class TermsDocument(
    val version: String,
    val language: Language,
    val title: String,
    /** Markdown. */
    val body: String,
    val updatedAt: String,
)
