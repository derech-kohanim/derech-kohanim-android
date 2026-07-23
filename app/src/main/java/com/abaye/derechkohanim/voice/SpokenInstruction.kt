package com.abaye.derechkohanim.voice

/**
 * One thing to say, in both renderings the player might use: the ordered recorded-clip keys,
 * and the fully assembled text for the device speech engine. The two say the same sentence.
 *
 * `urgent` marks a zone warning, which preempts a driving instruction (spec 15.9).
 */
data class SpokenInstruction(
    val clipKeys: List<String>,
    val text: String,
    val urgent: Boolean = false,
)
