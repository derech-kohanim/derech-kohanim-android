package com.abaye.derechkohanim.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.VoiceClip
import com.abaye.derechkohanim.data.model.VoiceIds
import com.abaye.derechkohanim.data.model.VoicePackManifest
import com.abaye.derechkohanim.data.repository.VoiceRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class VoicePackState { UNKNOWN, DOWNLOADING, READY, ERROR }

/**
 * What the settings screen shows for the voice pack: whether it is downloaded, its version and
 * how many clips are cached, and whether an update is in flight.
 */
data class VoicePackStatus(
    val state: VoicePackState = VoicePackState.UNKNOWN,
    val version: Int = 0,
    val clipCount: Int = 0,
)

/**
 * Owns the recorded voice pack: downloads the global pack for the current language, caches each
 * clip's audio under filesDir/voice/<lang>/, and keeps [VoicePlayer] pointed at the local files
 * so navigation speaks the recorded clips instead of only device TTS.
 *
 * The cached manifest (with local file paths substituted for [VoiceClip.url]) is written to
 * manifest.json in the same folder, so on the next launch the pack is available offline before any
 * network call. Refreshes ask the server with `sinceVersion` and download only the changed clips.
 *
 * Every path is [DkResult]-tolerant and runs off the main thread: a failed download simply leaves
 * the previous cache in place (or falls back to TTS), never blocking the UI.
 */
@Singleton
class VoicePackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val voiceRepository: VoiceRepository,
    private val voicePlayer: VoicePlayer,
    private val sessionStore: SessionStore,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _status = MutableStateFlow(VoicePackStatus())
    val status: StateFlow<VoicePackStatus> = _status.asStateFlow()

    /** The pack currently loaded into the player, keyed by clip key, with local-file urls. */
    private var packClips: MutableMap<String, VoiceClip> = mutableMapOf()
    private var started = false

    /**
     * Loads whatever is cached into the player immediately, then kicks off a background refresh and
     * a periodic version check. Safe to call more than once; only the first call arms the loop.
     */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            loadCachedIntoPlayer()
            while (true) {
                refresh()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    /** Manual "download / update now" from settings. Runs in the background. */
    fun refreshNow() {
        scope.launch { refresh() }
    }

    /** Reads the on-disk manifest for the current language and points the player at it. */
    private suspend fun loadCachedIntoPlayer() = mutex.withLock {
        val snapshot = sessionStore.snapshot()
        val language = snapshot.language
        val voiceId = VoiceIds.normalize(snapshot.voiceId, language)
        val cached = readManifest(language, voiceId) ?: run {
            _status.value = VoicePackStatus(state = VoicePackState.UNKNOWN)
            return@withLock
        }
        val present = cached.clips.filter { File(it.url).exists() }
        packClips = present.associateBy { it.key }.toMutableMap()
        voicePlayer.setPack(present)
        _status.value = VoicePackStatus(
            state = if (present.isEmpty()) VoicePackState.UNKNOWN else VoicePackState.READY,
            version = cached.version,
            clipCount = present.size,
        )
    }

    /**
     * Fetches the pack manifest with `sinceVersion`, downloads any new or changed clip audio, merges
     * it into the cache and re-points the player. A language or voice change forces a full
     * re-download: each voice (masculine, feminine, English) is its own pack with its own texts.
     */
    suspend fun refresh(): DkResult<Unit> = mutex.withLock {
        val snapshot = sessionStore.snapshot()
        val language = snapshot.language
        val voiceId = VoiceIds.normalize(snapshot.voiceId, language)
        val sameLanguage = snapshot.voicePackLanguage == language.tag &&
            snapshot.voicePackVoiceId == voiceId
        val since = if (sameLanguage) snapshot.voicePackVersion.takeIf { it > 0 } else null

        _status.value = _status.value.copy(state = VoicePackState.DOWNLOADING)

        val result = voiceRepository.manifest(language, voiceId, since)
        if (result is DkResult.Failure) {
            // Keep the existing cache; report ready if we have one, else surface the error.
            _status.value = _status.value.copy(
                state = if (packClips.isEmpty()) VoicePackState.ERROR else VoicePackState.READY,
            )
            return@withLock DkResult.Failure(result.code, result.cause)
        }

        val manifest = (result as DkResult.Success).value
        val existing: MutableMap<String, VoiceClip> = if (sameLanguage) {
            (readManifest(language, voiceId)?.clips ?: emptyList())
                .filter { File(it.url).exists() }
                .associateBy { it.key }
                .toMutableMap()
        } else {
            mutableMapOf()
        }

        val dir = voiceDir(language, voiceId)
        for (clip in manifest.clips) {
            val local = downloadClip(clip, dir)
            if (local != null) existing[clip.key] = clip.copy(url = local.absolutePath)
        }

        val merged = existing.values.toList()
        writeManifest(manifest.copy(clips = merged), language, voiceId)
        sessionStore.setVoicePack(language, voiceId, manifest.version)

        packClips = merged.associateBy { it.key }.toMutableMap()
        voicePlayer.setPack(merged)
        _status.value = VoicePackStatus(
            state = VoicePackState.READY,
            version = manifest.version,
            clipCount = merged.size,
        )
        DkResult.Success(Unit)
    }

    /**
     * Per-drive prefetch for offline continuity: resolves the route's required clip keys that are
     * not already cached (typically street-name clips), downloads them and folds them into the live
     * pack so the whole drive plays without connectivity. Returns success once every fetchable clip
     * is on disk; a clip that cannot be produced simply falls back to TTS.
     */
    suspend fun ensureRouteClips(requiredKeys: List<String>): DkResult<Unit> = mutex.withLock {
        val missing = requiredKeys.filter { it.isNotBlank() && !packClips.containsKey(it) }
        if (missing.isEmpty()) return@withLock DkResult.Success(Unit)

        val snapshot = sessionStore.snapshot()
        val language = snapshot.language
        val voiceId = VoiceIds.normalize(snapshot.voiceId, language)
        val resolved = voiceRepository.resolve(language, voiceId, missing)
        if (resolved is DkResult.Failure) return@withLock DkResult.Failure(resolved.code, resolved.cause)

        val dir = voiceDir(language, voiceId)
        (resolved as DkResult.Success).value.clips.forEach { clip ->
            val local = downloadClip(clip, dir)
            if (local != null) packClips[clip.key] = clip.copy(url = local.absolutePath)
        }
        voicePlayer.setPack(packClips.values.toList())
        DkResult.Success(Unit)
    }

    /** Downloads a clip's audio into [dir], skipping the fetch when a file with its hash exists. */
    private suspend fun downloadClip(clip: VoiceClip, dir: File): File? = withContext(Dispatchers.IO) {
        if (clip.url.isBlank()) return@withContext null
        val target = File(dir, "${clip.hash}.${extensionOf(clip.url)}")
        if (target.exists() && target.length() > 0) return@withContext target
        runCatching {
            dir.mkdirs()
            val request = Request.Builder().url(clip.url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                val tmp = File(dir, "${clip.hash}.tmp")
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                if (tmp.renameTo(target)) target else { tmp.delete(); null }
            }
        }.getOrNull()
    }

    /** One directory per voice, e.g. voice/he-he-female, so switching voices never mixes clips. */
    private fun voiceDir(language: Language, voiceId: String): File =
        File(context.filesDir, "voice/${language.tag}-$voiceId")

    private fun manifestFile(language: Language, voiceId: String): File =
        File(voiceDir(language, voiceId), "manifest.json")

    private fun readManifest(language: Language, voiceId: String): VoicePackManifest? {
        val file = manifestFile(language, voiceId)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(VoicePackManifest.serializer(), file.readText())
        }.getOrNull()
    }

    private fun writeManifest(manifest: VoicePackManifest, language: Language, voiceId: String) {
        runCatching {
            val file = manifestFile(language, voiceId)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(VoicePackManifest.serializer(), manifest))
        }
    }

    private fun extensionOf(url: String): String {
        val ext = url.substringBefore('?').substringAfterLast('.', "")
        return ext.takeIf { it.isNotBlank() && it.length <= 4 } ?: "mp3"
    }

    private companion object {
        // Recorded clips change rarely; a check every few hours plus the app-start refresh is ample.
        const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
