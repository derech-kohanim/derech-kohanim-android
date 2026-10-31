package com.abaye.derechkohanim.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A durable snapshot of an active real drive, enough to reconstruct the whole navigation
 * session after the app is killed. Kept serialization-friendly with the existing
 * kotlinx.serialization models: the route geometry and maneuvers, the destination (for
 * rerouting), the chosen stringency, and how far along the route the driver had progressed.
 * Simulated drives are never persisted - they only ever live in memory.
 */
@Serializable
data class PersistedNavState(
    val route: RouteResult,
    val destination: PlaceSuggestion? = null,
    val simulated: Boolean = false,
    val progressIndex: Int = 0,
    val stringency: Stringency,
    /**
     * The route's events (zones, cameras, level crossings) as fetched at the start, so a drive
     * resumed with no network still has its road alerts.
     */
    val events: RouteEventBundle? = null,
)

private val Context.navDataStore: DataStore<Preferences> by preferencesDataStore(name = "derech_kohanim_nav")

/**
 * Persists the one active navigation session so a drive survives the process being killed. The
 * large, static part (route + destination + stringency) is written once when the drive starts
 * or reroutes; the cheap, fast-moving [PersistedNavState.progressIndex] is updated on its own,
 * throttled, as the drive advances. The whole record is cleared the moment navigation ends,
 * arrives or is cancelled, so a resume is only ever offered for a drive that was genuinely
 * still running.
 */
@Singleton
class NavStateStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val blob = stringPreferencesKey("active_nav_blob")
        val progress = intPreferencesKey("active_nav_progress")
    }

    /** Writes the full record (static part + current progress). Used at start and on reroute. */
    suspend fun save(state: PersistedNavState) = context.navDataStore.edit { prefs ->
        prefs[Keys.blob] = json.encodeToString(PersistedNavState.serializer(), state)
        prefs[Keys.progress] = state.progressIndex
    }

    /** Cheap update of just how far along the route we are, called on a throttle while driving. */
    suspend fun updateProgress(progressIndex: Int) = context.navDataStore.edit { prefs ->
        if (prefs[Keys.blob] != null) prefs[Keys.progress] = progressIndex
    }

    /**
     * Reconstructs the persisted session, if any. The stored progress index overrides the one in
     * the blob so the drive resumes from the latest position, not the position at the last full
     * save. Returns null when nothing is stored or the blob can no longer be decoded.
     */
    suspend fun load(): PersistedNavState? {
        val prefs = context.navDataStore.data.first()
        val blob = prefs[Keys.blob] ?: return null
        return runCatching {
            val decoded = json.decodeFromString(PersistedNavState.serializer(), blob)
            decoded.copy(progressIndex = prefs[Keys.progress] ?: decoded.progressIndex)
        }.getOrNull()
    }

    suspend fun clear() = context.navDataStore.edit { it.clear() }
}
