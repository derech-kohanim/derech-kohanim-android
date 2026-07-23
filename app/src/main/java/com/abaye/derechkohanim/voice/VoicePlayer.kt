package com.abaye.derechkohanim.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.VoiceClip
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an assembled instruction as a chain of short clips, falling back down the ladder in
 * spec 15.7: a clip from the pack, then the device speech engine, then screen only.
 *
 * The hard rule from spec 15.6 is implemented here: playback never waits for audio. A key
 * with no clip is skipped, not queued.
 *
 * Scaffolding. Clip download, caching and the announcement scheduler are not written yet.
 */
@Singleton
class VoicePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var clips: Map<String, VoiceClip> = emptyMap()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var muted = false

    fun setPack(clips: List<VoiceClip>) {
        this.clips = clips.associateBy { it.key }
    }

    fun setMuted(muted: Boolean) {
        this.muted = muted
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

    /**
     * Speaks one instruction through the fallback chain of spec 15.7:
     *   every clip present  ->  play the recorded clips in order,
     *   otherwise           ->  speak the assembled text once with the device engine,
     *   TTS unavailable     ->  nothing, the screen already shows it.
     *
     * Navigation never waits on this (spec 15.6): playback happens on the caller's scope and
     * a missing clip or a dead engine simply produces less sound, never a delay.
     */
    suspend fun speak(instruction: SpokenInstruction) {
        if (muted) return
        if (instruction.urgent) tts?.stop()
        mutex.withLock {
            val allClipsPresent = instruction.clipKeys.isNotEmpty() &&
                instruction.clipKeys.all { clips.containsKey(it) }
            if (allClipsPresent) {
                instruction.clipKeys.forEach { key -> clips[key]?.let { playClip(it) } }
            } else {
                speakWithTts(instruction.text)
            }
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
        } catch (error: Exception) {
            // A broken clip is skipped, exactly like a missing one.
        } finally {
            player.release()
        }
    }

    /**
     * Last resort before screen only: the assembled sentence spoken by the device engine.
     * The commas in the phrase give it the same pauses the clip boundaries would.
     */
    private fun speakWithTts(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun release() {
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
