package com.abaye.derechkohanim.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "derech_kohanim")

/**
 * Everything the app needs before the first network call: who is signed in, and the
 * preferences that decide how a route is computed and how the interface reads.
 */
data class SessionSnapshot(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val language: Language = Language.HE,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val stringency: Stringency = Stringency.BASIC,
    val trafficAware: Boolean = true,
    val avoidTolls: Boolean = false,
    val voiceEnabled: Boolean = true,
    val voiceId: String = "he-default",
    val acceptedTermsVersion: String? = null,
    val onboardingComplete: Boolean = false,
) {
    val isSignedIn: Boolean get() = accessToken != null
}

@Singleton
class SessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val stringency = stringPreferencesKey("stringency")
        val trafficAware = booleanPreferencesKey("traffic_aware")
        val avoidTolls = booleanPreferencesKey("avoid_tolls")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val voiceId = stringPreferencesKey("voice_id")
        val acceptedTermsVersion = stringPreferencesKey("accepted_terms_version")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    }

    val snapshots: Flow<SessionSnapshot> = context.dataStore.data.map { prefs ->
        SessionSnapshot(
            accessToken = prefs[Keys.accessToken],
            refreshToken = prefs[Keys.refreshToken],
            language = prefs[Keys.language]?.let { enumOrNull<Language>(it) } ?: Language.HE,
            theme = prefs[Keys.theme]?.let { enumOrNull<ThemePreference>(it) } ?: ThemePreference.SYSTEM,
            stringency = prefs[Keys.stringency]?.let { enumOrNull<Stringency>(it) } ?: Stringency.BASIC,
            trafficAware = prefs[Keys.trafficAware] ?: true,
            avoidTolls = prefs[Keys.avoidTolls] ?: false,
            voiceEnabled = prefs[Keys.voiceEnabled] ?: true,
            voiceId = prefs[Keys.voiceId] ?: "he-default",
            acceptedTermsVersion = prefs[Keys.acceptedTermsVersion],
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
        )
    }

    suspend fun snapshot(): SessionSnapshot = snapshots.first()

    suspend fun setTokens(access: String?, refresh: String?) = context.dataStore.edit { prefs ->
        if (access == null) prefs.remove(Keys.accessToken) else prefs[Keys.accessToken] = access
        if (refresh == null) prefs.remove(Keys.refreshToken) else prefs[Keys.refreshToken] = refresh
    }

    suspend fun setLanguage(language: Language) = context.dataStore.edit {
        it[Keys.language] = language.name
    }

    suspend fun setTheme(theme: ThemePreference) = context.dataStore.edit {
        it[Keys.theme] = theme.name
    }

    suspend fun setStringency(stringency: Stringency) = context.dataStore.edit {
        it[Keys.stringency] = stringency.name
    }

    suspend fun setTrafficAware(enabled: Boolean) = context.dataStore.edit {
        it[Keys.trafficAware] = enabled
    }

    suspend fun setAvoidTolls(enabled: Boolean) = context.dataStore.edit {
        it[Keys.avoidTolls] = enabled
    }

    suspend fun setVoiceEnabled(enabled: Boolean) = context.dataStore.edit {
        it[Keys.voiceEnabled] = enabled
    }

    suspend fun setAcceptedTermsVersion(version: String) = context.dataStore.edit {
        it[Keys.acceptedTermsVersion] = version
    }

    suspend fun setOnboardingComplete(complete: Boolean) = context.dataStore.edit {
        it[Keys.onboardingComplete] = complete
    }

    suspend fun clearSession() = context.dataStore.edit {
        it.remove(Keys.accessToken)
        it.remove(Keys.refreshToken)
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
