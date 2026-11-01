package com.abaye.derechkohanim.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import com.abaye.derechkohanim.core.ZoneFilter
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.DangerZoneCollection
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ZoneCollection
import com.abaye.derechkohanim.data.model.ZoneFeature
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ZoneLayerState { UNKNOWN, UPDATING, READY, ERROR }

/**
 * What the settings screen shows for the cached map layers: whether they are downloaded, their
 * versions and how many features are cached, and whether a refresh is in flight.
 */
data class ZoneLayerStatus(
    val state: ZoneLayerState = ZoneLayerState.UNKNOWN,
    val zoneVersion: Int = 0,
    val dangerVersion: Int = 0,
    val zoneCount: Int = 0,
    val dangerCount: Int = 0,
    /** When the cached layers were last written (a sync that changed them), epoch millis. */
    val updatedAtMillis: Long? = null,
)

/**
 * The change a launch sync applied, so the home screen can show a brief banner. [added] and
 * [changed] count features that appeared or differ; [removed] counts features that went away.
 */
data class ZoneLayerChange(
    val added: Int = 0,
    val changed: Int = 0,
    val removed: Int = 0,
) {
    /** Additions plus changes, the "N updated" figure for the banner. */
    val upserted: Int get() = added + changed
    val isEmpty: Boolean get() = added == 0 && changed == 0 && removed == 0
}

/**
 * Owns the zone (halachic stringency) and danger-zone (Area A) polygon layers locally, so the Home
 * and Navigation maps can draw them instantly and offline - mirroring [com.abaye.derechkohanim.voice.VoicePackManager].
 *
 * The last full [ZoneCollection]/[DangerZoneCollection] are cached under filesDir/zones (one file per
 * stringency plus danger.json); their versions and the cached stringency live in [SessionStore]. On
 * [start] the cache is loaded into [zones]/[danger] at once, then a background refresh asks the server
 * with the cached version: a 304 keeps the cache; a full 200 is diffed by feature id, the cache and
 * version are rewritten, the flows update, and the change counts are emitted on [changes] for a banner.
 *
 * Zones are cumulative per stringency, so a stringency change replaces that layer wholesale
 * (sinceVersion = null) with no banner. Every path is failure-tolerant: a failed refresh simply
 * leaves the previous cache in place and never blocks the UI.
 */
@Singleton
class ZoneLayerStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val routingRepository: RoutingRepository,
    private val sessionStore: SessionStore,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _zones = MutableStateFlow<List<ZoneFeature>>(emptyList())
    val zones: StateFlow<List<ZoneFeature>> = _zones.asStateFlow()

    private val _danger = MutableStateFlow<List<DangerZoneFeature>>(emptyList())
    val danger: StateFlow<List<DangerZoneFeature>> = _danger.asStateFlow()

    private val _zonesIndexed = MutableStateFlow<List<ZoneFilter.Bounded<ZoneFeature>>>(emptyList())

    /**
     * The same features as [zones], each with its bounding box precomputed. The map clips the layer
     * to the camera every time it moves, so the boxes are built once here - on the store's IO
     * scope, as the layer is published - and never re-derived per camera event.
     */
    val zonesIndexed: StateFlow<List<ZoneFilter.Bounded<ZoneFeature>>> = _zonesIndexed.asStateFlow()

    private val _dangerIndexed =
        MutableStateFlow<List<ZoneFilter.Bounded<DangerZoneFeature>>>(emptyList())

    /** [danger] with its bounding boxes precomputed; see [zonesIndexed]. */
    val dangerIndexed: StateFlow<List<ZoneFilter.Bounded<DangerZoneFeature>>> =
        _dangerIndexed.asStateFlow()

    private val _status = MutableStateFlow(ZoneLayerStatus())
    val status: StateFlow<ZoneLayerStatus> = _status.asStateFlow()

    /** Emits the counts of a launch sync that applied changes, for the home-screen banner. */
    private val _changes = MutableSharedFlow<ZoneLayerChange>(extraBufferCapacity = 4)
    val changes: SharedFlow<ZoneLayerChange> = _changes.asSharedFlow()

    private var started = false

    /**
     * Loads whatever is cached into the flows immediately, then refreshes in the background and
     * re-checks periodically. A stringency change is watched separately so that layer is re-fetched
     * fresh. Safe to call more than once; only the first call arms the loops.
     */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            loadCachedIntoFlows()
            while (true) {
                refresh()
                delay(REFRESH_INTERVAL_MS)
            }
        }
        // The cached set is per-stringency, so when the user switches level the layer is replaced.
        scope.launch {
            sessionStore.snapshots
                .map { it.stringency }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    /** Manual "download / update now" from settings. Runs in the background. */
    fun refreshNow() {
        scope.launch { refresh() }
    }

    /** Reads the on-disk collections for the current stringency and publishes them at once. */
    private suspend fun loadCachedIntoFlows() = mutex.withLock {
        val snapshot = sessionStore.snapshot()
        val stringency = snapshot.stringency
        val cachedZones = if (snapshot.zoneLayerStringency == stringency.wire) {
            readZones(stringency)
        } else {
            null
        }
        publishZones(cachedZones?.features.orEmpty())
        publishDanger(readDanger()?.features.orEmpty())
        publishStatus()
    }

    /** Publishes a zone layer together with its bounding-box index, always in that order. */
    private fun publishZones(features: List<ZoneFeature>) {
        _zonesIndexed.value = ZoneFilter.indexZones(features)
        _zones.value = features
    }

    /** Publishes a danger layer together with its bounding-box index, always in that order. */
    private fun publishDanger(features: List<DangerZoneFeature>) {
        _dangerIndexed.value = ZoneFilter.indexDanger(features)
        _danger.value = features
    }

    /**
     * Fetches both layers with their cached versions and applies any change. On 304 the cache is
     * kept; on a full body the new collection is diffed, cached and published; on failure the cache
     * is kept. Emits the combined change counts on [changes] when a background sync altered features.
     */
    suspend fun refresh() = mutex.withLock {
        val snapshot = sessionStore.snapshot()
        _status.value = _status.value.copy(state = ZoneLayerState.UPDATING)

        val zoneChange = refreshZones(snapshot.stringency, snapshot.zoneLayerStringency, snapshot.zoneLayerVersion)
        val dangerChange = refreshDanger(snapshot.dangerLayerVersion)

        publishStatus()

        val combined = ZoneLayerChange(
            added = (zoneChange?.added ?: 0) + (dangerChange?.added ?: 0),
            changed = (zoneChange?.changed ?: 0) + (dangerChange?.changed ?: 0),
            removed = (zoneChange?.removed ?: 0) + (dangerChange?.removed ?: 0),
        )
        if (!combined.isEmpty) _changes.tryEmit(combined)
    }

    /**
     * Refreshes the stringency zone layer. Returns the diff only for a real version delta (same
     * stringency), so a stringency switch - a full replacement, not a background change - is silent.
     */
    private suspend fun refreshZones(
        stringency: Stringency,
        cachedStringency: String?,
        cachedVersion: Int,
    ): ZoneLayerChange? {
        val sameStringency = cachedStringency == stringency.wire
        val since = if (sameStringency) cachedVersion.takeIf { it > 0 } else null
        return when (val fetch = routingRepository.zonesSince(stringency, since)) {
            is LayerFetch.Unchanged, is LayerFetch.Failed -> null
            is LayerFetch.Changed -> {
                val fresh = fetch.value
                val cached = if (sameStringency) readZones(stringency)?.features.orEmpty() else emptyList()
                val diff = diff(cached, fresh.features) { it.properties.id }
                writeZones(stringency, fresh)
                sessionStore.setZoneLayer(stringency, fresh.version)
                publishZones(fresh.features)
                if (sameStringency) diff else null
            }
        }
    }

    private suspend fun refreshDanger(cachedVersion: Int): ZoneLayerChange? {
        val since = cachedVersion.takeIf { it > 0 }
        return when (val fetch = routingRepository.dangerZonesSince(since)) {
            is LayerFetch.Unchanged, is LayerFetch.Failed -> null
            is LayerFetch.Changed -> {
                val fresh = fetch.value
                val cached = readDanger()?.features.orEmpty()
                val diff = diff(cached, fresh.features) { it.properties.id }
                writeDanger(fresh)
                sessionStore.setDangerLayer(fresh.version)
                publishDanger(fresh.features)
                diff
            }
        }
    }

    /**
     * Compares two feature sets by id. Added = ids only in [fresh]; removed = ids only in [cached];
     * changed = ids in both whose content differs. Content equality is the feature's own structural
     * equality (a data class over geometry + properties), which is a stable hash of both.
     */
    private fun <T> diff(
        cached: List<T>,
        fresh: List<T>,
        id: (T) -> Int,
    ): ZoneLayerChange {
        val cachedById = cached.associateBy(id)
        val freshById = fresh.associateBy(id)
        var added = 0
        var changed = 0
        for ((key, value) in freshById) {
            val old = cachedById[key]
            if (old == null) added++ else if (old != value) changed++
        }
        val removed = cachedById.keys.count { it !in freshById }
        return ZoneLayerChange(added = added, changed = changed, removed = removed)
    }

    private suspend fun publishStatus() {
        val snapshot = sessionStore.snapshot()
        val zoneCount = _zones.value.size
        val dangerCount = _danger.value.size
        _status.value = ZoneLayerStatus(
            state = if (zoneCount == 0 && dangerCount == 0) {
                ZoneLayerState.UNKNOWN
            } else {
                ZoneLayerState.READY
            },
            zoneVersion = snapshot.zoneLayerVersion,
            dangerVersion = snapshot.dangerLayerVersion,
            zoneCount = zoneCount,
            dangerCount = dangerCount,
            updatedAtMillis = zonesDir().listFiles()?.maxOfOrNull { it.lastModified() }?.takeIf { it > 0 },
        )
    }

    // -- disk cache ------------------------------------------------------------------------------

    private fun zonesDir(): File = File(context.filesDir, "zones")

    private fun zonesFile(stringency: Stringency): File =
        File(zonesDir(), "${stringency.wire}.json")

    private fun dangerFile(): File = File(zonesDir(), "danger.json")

    private fun readZones(stringency: Stringency): ZoneCollection? {
        val file = zonesFile(stringency)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(ZoneCollection.serializer(), file.readText())
        }.getOrNull()
    }

    private fun writeZones(stringency: Stringency, collection: ZoneCollection) {
        runCatching {
            val file = zonesFile(stringency)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(ZoneCollection.serializer(), collection))
        }
    }

    private fun readDanger(): DangerZoneCollection? {
        val file = dangerFile()
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(DangerZoneCollection.serializer(), file.readText())
        }.getOrNull()
    }

    private fun writeDanger(collection: DangerZoneCollection) {
        runCatching {
            val file = dangerFile()
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(DangerZoneCollection.serializer(), collection))
        }
    }

    private companion object {
        // Zone data changes rarely; a check every few hours plus the app-start refresh is ample.
        const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
