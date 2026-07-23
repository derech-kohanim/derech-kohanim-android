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
    fun maneuverClipKeys(action: String, bucketMeters: Int, street: String?): List<String> {
        val keys = mutableListOf<String>()
        if (bucketMeters > 0) {
            keys += "prefix.in"
            keys += distanceKey(bucketMeters)
        } else {
            keys += "prefix.now"
        }
        keys += "action.$action"
        street?.takeIf { it.isNotBlank() }?.let { keys += "street.$it" }
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
