package com.abaye.derechkohanim.service

import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.MapProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves and publishes the EFFECTIVE map provider - the renderer `DkMap` actually draws with -
 * from three inputs:
 *
 *  - the user's [MapProvider] setting (AUTOMATIC / GOOGLE / OPENFREEMAP),
 *  - whether the current network is NetFree ([NetFreeDetector]),
 *  - whether the Google map failed to load this session ([reportGoogleMapLoadFailed]).
 *
 * Resolution:
 *  - GOOGLE / OPENFREEMAP force that renderer, always.
 *  - AUTOMATIC uses Google, but switches to OpenFreeMap once a NetFree network is detected AND
 *    the Google map has failed to load. So Google keeps being used whenever it actually works,
 *    even on NetFree; the fallback only kicks in when Google demonstrably cannot.
 *
 * The Google-failed flag is session-scoped and sticky: once Google fails on a NetFree network we
 * stay on OpenFreeMap for the rest of the process, rather than flapping between renderers.
 */
@Singleton
class MapProviderState @Inject constructor(
    sessionStore: SessionStore,
    netFreeDetector: NetFreeDetector,
    connectivity: Connectivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Whether the current network is NetFree. Starts false (fail-open) until the probe runs. */
    private val netFree = MutableStateFlow(false)

    /** Sticky for the session: the Google map failed to load while it was the active provider. */
    private val googleFailed = MutableStateFlow(false)

    val effective: StateFlow<MapProvider> =
        combine(
            sessionStore.snapshots.map { it.mapProvider }.distinctUntilChanged(),
            netFree,
            googleFailed,
        ) { setting, isNetFree, failed ->
            when (setting) {
                MapProvider.GOOGLE -> MapProvider.GOOGLE
                MapProvider.OPENFREEMAP -> MapProvider.OPENFREEMAP
                MapProvider.AUTOMATIC ->
                    if (isNetFree && failed) MapProvider.OPENFREEMAP else MapProvider.GOOGLE
            }
        }.stateIn(scope, SharingStarted.Eagerly, MapProvider.GOOGLE)

    init {
        // Re-probe NetFree whenever connectivity changes. online() seeds with the current state,
        // so this also performs the initial detection at startup.
        scope.launch {
            connectivity.online().collect {
                netFree.value = netFreeDetector.isNetFree()
            }
        }
    }

    /** Called by the Google renderer when its map does not report loaded within the timeout. */
    fun reportGoogleMapLoadFailed() {
        googleFailed.value = true
    }

    /**
     * Called by the Google renderer when its map reports loaded. Intentionally does NOT clear
     * [googleFailed]: the flag is sticky for the session so the provider never flaps, and a late
     * onMapLoaded arriving just after the timeout must not bounce the user back to Google.
     */
    fun reportGoogleMapLoaded() {
        // No-op by design; see kdoc.
    }
}
