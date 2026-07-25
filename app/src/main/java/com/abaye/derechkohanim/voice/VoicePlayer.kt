package com.abaye.derechkohanim.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.VoiceClip
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an assembled instruction as a chain of short clips, falling back down the ladder in
 * spec 15.7: a clip from the pack, then the device speech engine, then screen only.
 *
 * Two hard rules from spec 15.6 and 15.9:
 *  - Navigation never waits on audio. Submitting is fire and forget; a missing clip or a dead
 *    engine simply produces less sound, never a delay.
 *  - The voice never plays through a backlog. Announcements are superseded: while one is
 *    playing, newer submissions are coalesced to the LATEST (a conflated queue drops the
 *    intermediates), so when the current short clip finishes the player jumps straight to the
 *    current navigation situation instead of chaining through passed manoeuvres. A zone alert
 *    is urgent and cuts the current playback immediately.
 *
 * Scaffolding remains: clip download and caching are not written, so in practice the device
 * TTS fallback is what speaks.
 */
@Singleton
class VoicePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // Its own scope so a single worker outlives individual navigation sessions. Main.immediate
    // because MediaPlayer and TextToSpeech want a Looper thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** CONFLATED: only the most recent unconsumed request is kept, older ones are dropped. */
    private val queue = Channel<SpokenInstruction>(Channel.CONFLATED)

    private var workerJob: Job? = null
    private var playJob: Job? = null

    private var clips: Map<String, VoiceClip> = emptyMap()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    @Volatile private var muted = false
    /** True while the announcement currently playing is urgent (a zone warning), so [dropStale]
     *  never cuts it. */
    @Volatile private var currentUrgent = false

    init {
        startWorker()
    }

    private fun startWorker() {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch {
            for (request in queue) {
                // Before playing a normal announcement, drain to the freshest pending one, so a
                // stale line is skipped and the current situation wins. Stops stutter at 4x/8x.
                var current = request
                if (!current.urgent) {
                    while (true) {
                        val next = queue.tryReceive().getOrNull() ?: break
                        current = next
                    }
                }
                if (muted) continue

                currentUrgent = current.urgent
                playJob = scope.launch { playInstruction(current) }
                try {
                    playJob?.join()
                } catch (_: CancellationException) {
                    // Cut by an urgent alert; the loop moves on to the newest request.
                } finally {
                    currentUrgent = false
                }
            }
        }
    }

    /**
     * Queues an instruction to speak. Returns immediately. A normal announcement lets the
     * current short clip finish, then the player jumps to the latest queued announcement
     * (backlog dropped). An urgent one (zone alert) cuts the current playback at once.
     */
    fun submit(instruction: SpokenInstruction) {
        if (muted) return
        startWorker()
        if (instruction.urgent) {
            tts?.stop()
            playJob?.cancel()
        }
        queue.trySend(instruction)
    }

    /**
     * Preempts a now-stale announcement: stops the current clip/utterance and drops the queued
     * backlog, so the player jumps to the next fresh situation instead of finishing a line that
     * belongs to a manoeuvre the driver has already passed. Reuses the urgent-preempt path
     * (tts.stop() + playJob cancel). An active zone warning is never cut - it is urgent and must
     * be heard - so this is a no-op while an urgent announcement is playing.
     */
    fun dropStale() {
        if (currentUrgent) return
        tts?.stop()
        playJob?.cancel()
        drainQueue()
    }

    fun setPack(clips: List<VoiceClip>) {
        this.clips = clips.associateBy { it.key }
    }

    fun setMuted(muted: Boolean) {
        this.muted = muted
        if (muted) {
            tts?.stop()
            playJob?.cancel()
            drainQueue()
        }
    }

    fun prepareTts(language: Language, onUnavailable: () -> Unit = {}) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (!ttsReady) {
                onUnavailable()
                return@TextToSpeech
            }
            val locale = Locale.forLanguageTag(language.tag)
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                onUnavailable()
            }
        }
    }

    private suspend fun playInstruction(instruction: SpokenInstruction) {
        val allClipsPresent = instruction.clipKeys.isNotEmpty() &&
            instruction.clipKeys.all { clips.containsKey(it) }
        if (allClipsPresent) {
            instruction.clipKeys.forEach { key ->
                if (!scope.isActive) return
                clips[key]?.let { playClip(it) }
            }
        } else {
            // QUEUE_FLUSH so the device engine also drops any older utterance and speaks only
            // this one - the TTS path never chains a backlog either.
            speakWithTts(instruction.text)
        }
    }

    private suspend fun playClip(clip: VoiceClip) {
        val finished = CompletableDeferred<Unit>()
        val player = MediaPlayer()
        try {
            player.setDataSource(clip.url)
            player.setOnCompletionListener { finished.complete(Unit) }
            player.setOnErrorListener { _, _, _ ->
                finished.complete(Unit)
                true
            }
            player.prepare()
            player.start()
            finished.await()
        } catch (cancelled: CancellationException) {
            // Superseded mid-clip: stop and rethrow so the worker advances to the latest.
            runCatching { player.stop() }
            throw cancelled
        } catch (error: Exception) {
            // A broken clip is skipped, exactly like a missing one.
        } finally {
            runCatching { player.release() }
        }
    }

    /**
     * Last resort before screen only: the assembled sentence spoken by the device engine.
     * QUEUE_FLUSH replaces any in-progress utterance, so a superseded line is cut rather than
     * chained. The commas in the phrase give it the pauses the clip boundaries would.
     */
    private fun speakWithTts(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    private fun drainQueue() {
        while (queue.tryReceive().isSuccess) {
            // discard any pending announcements
        }
    }

    fun release() {
        playJob?.cancel()
        drainQueue()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
