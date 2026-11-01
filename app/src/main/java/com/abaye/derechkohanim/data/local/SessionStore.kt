package com.abaye.derechkohanim.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.MapProvider
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.model.VoiceIds
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
    /** Avoid danger zones (Area A). On by default. */
    val avoidDangerZones: Boolean = true,
    val voiceEnabled: Boolean = true,
    /** Which voice speaks the guidance; see [com.abaye.derechkohanim.data.model.VoiceIds]. */
    val voiceId: String = VoiceIds.HE_MALE,
    /** Which map renderer to use. AUTOMATIC falls back to OpenFreeMap on NetFree. */
    val mapProvider: MapProvider = MapProvider.AUTOMATIC,
    val acceptedTermsVersion: String? = null,
    val onboardingComplete: Boolean = false,
    /** The voice-pack version already downloaded and cached, per [voicePackLanguage]. */
    val voicePackVersion: Int = 0,
    /** The language tag the cached voice pack was downloaded for. */
    val voicePackLanguage: String? = null,
    /** The voice id the cached voice pack was downloaded for. */
    val voicePackVoiceId: String? = null,
    /** The zone-layer version already downloaded and cached, for [zoneLayerStringency]. */
    val zoneLayerVersion: Int = 0,
    /** The stringency the cached zone layer was downloaded for (wire form). */
    val zoneLayerStringency: String? = null,
    /** The danger-zone (Area A) layer version already downloaded and cached. */
    val dangerLayerVersion: Int = 0,
    /** The signed-in account's display name, email and picture URL, for the settings screens. */
    val profileName: String? = null,
    val profileEmail: String? = null,
    val profileAvatarUrl: String? = null,
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
        val profileName = stringPreferencesKey("profile_name")
        val profileEmail = stringPreferencesKey("profile_email")
        val profileAvatarUrl = stringPreferencesKey("profile_avatar_url")
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val stringency = stringPreferencesKey("stringency")
        val trafficAware = booleanPreferencesKey("traffic_aware")
        val avoidTolls = booleanPreferencesKey("avoid_tolls")
        val avoidDangerZones = booleanPreferencesKey("avoid_danger_zones")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val voiceId = stringPreferencesKey("voice_id")
        val mapProvider = stringPreferencesKey("map_provider")
        val acceptedTermsVersion = stringPreferencesKey("accepted_terms_version")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val voicePackVersion = intPreferencesKey("voice_pack_version")
        val voicePackLanguage = stringPreferencesKey("voice_pack_language")
        val voicePackVoiceId = stringPreferencesKey("voice_pack_voice_id")
        val zoneLayerVersion = intPreferencesKey("zone_layer_version")
        val zoneLayerStringency = stringPreferencesKey("zone_layer_stringency")
        val dangerLayerVersion = intPreferencesKey("danger_layer_version")
        val analyticsClientId = stringPreferencesKey("analytics_client_id")
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
            avoidDangerZones = prefs[Keys.avoidDangerZones] ?: true,
            voiceEnabled = prefs[Keys.voiceEnabled] ?: true,
            // A legacy "he-default" (saved before the voice choice existed) reads as he-male.
            voiceId = prefs[Keys.voiceId]
                ?.takeIf { it != VoiceIds.HE_DEFAULT_LEGACY }
                ?: VoiceIds.HE_MALE,
            mapProvider = prefs[Keys.mapProvider]?.let { enumOrNull<MapProvider>(it) } ?: MapProvider.AUTOMATIC,
            acceptedTermsVersion = prefs[Keys.acceptedTermsVersion],
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
            voicePackVersion = prefs[Keys.voicePackVersion] ?: 0,
            voicePackLanguage = prefs[Keys.voicePackLanguage],
            voicePackVoiceId = prefs[Keys.voicePackVoiceId],
            zoneLayerVersion = prefs[Keys.zoneLayerVersion] ?: 0,
            zoneLayerStringency = prefs[Keys.zoneLayerStringency],
            dangerLayerVersion = prefs[Keys.dangerLayerVersion] ?: 0,
            profileName = prefs[Keys.profileName],
            profileEmail = prefs[Keys.profileEmail],
            profileAvatarUrl = prefs[Keys.profileAvatarUrl],
        )
    }

    suspend fun snapshot(): SessionSnapshot = snapshots.first()

    /** Stores who is signed in, for display only; cleared with the session. */
    suspend fun setProfile(name: String?, email: String?, avatarUrl: String?) = context.dataStore.edit { prefs ->
        if (name.isNullOrBlank()) prefs.remove(Keys.profileName) else prefs[Keys.profileName] = name
        if (email.isNullOrBlank()) prefs.remove(Keys.profileEmail) else prefs[Keys.profileEmail] = email
        if (avatarUrl.isNullOrBlank()) prefs.remove(Keys.profileAvatarUrl) else prefs[Keys.profileAvatarUrl] = avatarUrl
    }

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

    suspend fun setAvoidDangerZones(enabled: Boolean) = context.dataStore.edit {
        it[Keys.avoidDangerZones] = enabled
    }

    suspend fun setVoiceEnabled(enabled: Boolean) = context.dataStore.edit {
        it[Keys.voiceEnabled] = enabled
    }

    suspend fun setVoiceId(voiceId: String) = context.dataStore.edit {
        it[Keys.voiceId] = voiceId
    }

    suspend fun setMapProvider(provider: MapProvider) = context.dataStore.edit {
        it[Keys.mapProvider] = provider.name
    }

    suspend fun setAcceptedTermsVersion(version: String) = context.dataStore.edit {
        it[Keys.acceptedTermsVersion] = version
    }

    suspend fun setOnboardingComplete(complete: Boolean) = context.dataStore.edit {
        it[Keys.onboardingComplete] = complete
    }

    /** Records the voice-pack version, language and voice now held in the on-disk cache. */
    suspend fun setVoicePack(language: Language, voiceId: String, version: Int) = context.dataStore.edit {
        it[Keys.voicePackVersion] = version
        it[Keys.voicePackLanguage] = language.tag
        it[Keys.voicePackVoiceId] = voiceId
    }

    /** Records the zone-layer version and stringency now held in the on-disk cache. */
    suspend fun setZoneLayer(stringency: Stringency, version: Int) = context.dataStore.edit {
        it[Keys.zoneLayerVersion] = version
        it[Keys.zoneLayerStringency] = stringency.wire
    }

    /** Records the danger-zone (Area A) layer version now held in the on-disk cache. */
    suspend fun setDangerLayer(version: Int) = context.dataStore.edit {
        it[Keys.dangerLayerVersion] = version
    }

    suspend fun clearSession() = context.dataStore.edit {
        it.remove(Keys.accessToken)
        it.remove(Keys.refreshToken)
        it.remove(Keys.profileName)
        it.remove(Keys.profileEmail)
        it.remove(Keys.profileAvatarUrl)
    }

    /**
     * A stable, anonymous analytics client id (a random UUID) for the GA4 Measurement Protocol.
     * Generated once and reused for the life of the install. It is NOT the user id and carries no
     * PII; it only lets GA4 count distinct devices. Survives sign-out (not cleared with the session).
     */
    suspend fun analyticsClientId(): String {
        context.dataStore.data.first()[Keys.analyticsClientId]?.let { return it }
        val generated = java.util.UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.analyticsClientId] = generated }
        return generated
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
