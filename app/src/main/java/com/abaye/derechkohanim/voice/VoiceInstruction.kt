package com.abaye.derechkohanim.voice

import com.abaye.derechkohanim.data.model.RouteEventType

/**
 * Builds the ordered recorded-clip keys for an instruction (spec 15.1). Keys follow
 * "<role>.<value>". The clip path and the text path (VoicePhrase) are assembled from the
 * same structured inputs, so they always say the same thing.
 */
object VoiceInstruction {

    fun distanceKey(bucketMeters: Int): String = when {
        bucketMeters <= 0 -> "dist.now"
        bucketMeters >= 1000 -> "dist.${bucketMeters / 1000}km"
        else -> "dist.${bucketMeters}m"
    }

    /**
     * Ordered clip keys for a manoeuvre. The street clip is last and optional, so a missing
     * street recording shortens the sentence instead of blocking it (spec 15.6).
     */
    fun maneuverClipKeys(
        action: String,
        bucketMeters: Int,
        street: String?,
        exitNumber: Int? = null,
    ): List<String> {
        val keys = mutableListOf<String>()
        if (bucketMeters > 0) {
            keys += "prefix.in"
            keys += distanceKey(bucketMeters)
        }
        keys += "action.$action"
        // "Turn right now": the server catalogue has no "prefix.now"; "dist.now" follows the action.
        if (bucketMeters <= 0) keys += "dist.now"
        street?.takeIf { it.isNotBlank() }?.let { keys += "street.$it" }
        exitKey(action, exitNumber)?.let { keys += it }
        return keys
    }

    /** "exit.N" after a roundabout entry, when the step names an exit we have a clip for. */
    fun exitKey(action: String, exitNumber: Int?): String? {
        if (!VoicePhrase.isRoundaboutEntry(action)) return null
        val n = exitNumber ?: return null
        return if (n in 1..com.abaye.derechkohanim.data.model.ROUNDABOUT_EXIT_KEYS_MAX) "exit.$n" else null
    }

    /** Clip keys for a fixed cue, prefixed with the distance when the cue takes one. */
    fun cueClipKeys(cue: VoiceCue, bucketMeters: Int): List<String> {
        val keys = mutableListOf<String>()
        if (cue.takesDistance && bucketMeters > 0) {
            keys += "prefix.in"
            keys += distanceKey(bucketMeters)
        }
        keys += cue.clipKey
        return keys
    }

    /**
     * Clip keys for an approaching event. Zone warnings outrank driving instructions and are
     * spoken immediately (spec 15.9); the caller marks them urgent.
     */
    fun eventClipKeys(type: RouteEventType, bucketMeters: Int): List<String> {
        val keys = mutableListOf<String>()
        if (bucketMeters > 0) {
            keys += "prefix.in"
            keys += distanceKey(bucketMeters)
        }
        keys += "event.${type.name.lowercase()}"
        return keys
    }
}
