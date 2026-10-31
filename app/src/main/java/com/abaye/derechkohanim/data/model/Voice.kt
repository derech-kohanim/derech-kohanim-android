package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors derech-kohanim-shared/src/types/voice.ts. */

@Serializable
enum class VoiceClipRole {
    @SerialName("prefix") PREFIX,
    @SerialName("dist") DIST,
    @SerialName("action") ACTION,
    @SerialName("zone") ZONE,
    @SerialName("street") STREET,
    @SerialName("misc") MISC,
}

@Serializable
enum class VoiceClipSource {
    @SerialName("recorded") RECORDED,
    @SerialName("generated") GENERATED,
}

@Serializable
data class VoiceClip(
    /** Shape: "<role>.<value>", e.g. "prefix.in", "dist.200m", "action.turn_right". */
    val key: String,
    val role: VoiceClipRole,
    /** Text the clip speaks, for QA and for the text fallback. */
    val text: String,
    val url: String,
    val durationMs: Int,
    val source: VoiceClipSource,
    /** Content hash, so a client re-downloads only what changed. */
    val hash: String,
)

@Serializable
data class VoicePackManifest(
    val language: Language,
    val voiceId: String,
    val version: Int,
    val clips: List<VoiceClip> = emptyList(),
    val updatedAt: String,
)

@Serializable
data class VoiceResolveRequest(
    val language: Language,
    val voiceId: String,
    /** Clip keys the client does not have yet. */
    val keys: List<String>,
)

@Serializable
data class VoiceResolveResponse(
    val clips: List<VoiceClip> = emptyList(),
    /** Keys that could not be produced. The client speaks a shorter instruction instead. */
    val missing: List<String> = emptyList(),
)

/**
 * The selectable voices. Hebrew instructions are gendered, so the id decides both the
 * synthesized voice and the grammatical form of the text. `he-default` is a legacy alias of
 * `he-male`.
 */
object VoiceIds {
    const val HE_MALE = "he-male"
    const val HE_FEMALE = "he-female"
    const val EN_DEFAULT = "en-default"
    const val HE_DEFAULT_LEGACY = "he-default"

    /** Maps a stored id onto a current one; legacy and unknown Hebrew ids become [HE_MALE]. */
    fun normalize(id: String?, language: Language): String = when {
        language == Language.EN -> EN_DEFAULT
        id == HE_FEMALE -> HE_FEMALE
        else -> HE_MALE
    }

    fun isFemale(id: String?): Boolean = id == HE_FEMALE
}

/** Highest roundabout exit that has its own ordinal clip (`exit.1` .. `exit.8`). */
const val ROUNDABOUT_EXIT_KEYS_MAX = 8

/** Announced distances are a closed ladder, so the clip library stays small. */
val DISTANCE_LADDER_METERS = listOf(2000, 1000, 800, 500, 300, 200, 100, 50, 0)
