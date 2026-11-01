package com.abaye.derechkohanim.voice

import android.content.Context
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.model.ROUNDABOUT_EXIT_KEYS_MAX
import com.abaye.derechkohanim.data.model.RouteEventType
import kotlin.math.abs

/**
 * Assembles the SPOKEN instruction from structured parts, exactly as a real navigator does.
 * This is not the on-screen banner and must never be derived from it - the banner and the
 * phrase are two independent renderings of the same manoeuvre (spec 15.1).
 *
 * Shape, per language:
 *   "בעוד 200 מטר, פְּנֵה ימינה אל ז'בוטינסקי"
 *   "פְּנֵה ימינה עכשיו"
 *   "הִיכָּנֵס לכיכר, ביציאה השנייה"
 *   "In 200 m, turn right onto Jabotinsky"
 *
 * The verb texts are the dedicated voice_* strings, not the banner labels: they carry niqqud so
 * the device speech engine pronounces the verbs right. The driver is addressed in the masculine
 * whichever voice is picked: the voice choice is the speaker, not the listener.
 *
 * Commas separate the segments so the device speech engine inserts a natural pause; the
 * recorded-clip path gets the same pauses from the clip boundaries.
 */
object VoicePhrase {

    /**
     * Distances at which a manoeuvre is announced. A closed ladder, not a continuous read
     * (spec 15.2). 0 stands for "now". Kept in sync with DISTANCE_LADDER_METERS in the
     * shared voice contract.
     */
    val ANNOUNCE_RUNGS_METERS = listOf(800, 500, 300, 200, 100, 50, 0)

    private const val NOW_THRESHOLD_METERS = 25.0

    /** Rounds a real distance to the nearest rung so the phrase uses a spoken value. */
    fun snapToRung(meters: Double): Int {
        if (meters <= NOW_THRESHOLD_METERS) return 0
        return ANNOUNCE_RUNGS_METERS.minByOrNull { abs(it - meters) } ?: 0
    }

    /** The spoken string for a driving manoeuvre. */
    fun maneuver(
        context: Context,
        action: String,
        distanceBucketMeters: Int,
        street: String?,
        exitNumber: Int? = null,
    ): String {
        val parts = mutableListOf<String>()
        if (distanceBucketMeters > 0) {
            parts += distancePrefix(context, distanceBucketMeters)
        }

        val core = buildString {
            append(context.getString(actionTextRes(action)))
            if (!street.isNullOrBlank() && speaksStreet(action)) {
                append(' ')
                append(context.getString(R.string.maneuver_onto_street, street))
            }
            if (distanceBucketMeters == 0) {
                append(' ')
                append(context.getString(R.string.nav_now))
            }
        }
        parts += core
        exitTextRes(action, exitNumber)?.let { parts += context.getString(it) }
        return parts.joinToString(", ")
    }

    /** The spoken string for a fixed alert or status cue, e.g. "בעוד 500 מטר, מצלמת מהירות לפניך". */
    fun cue(context: Context, cue: VoiceCue, distanceBucketMeters: Int): String {
        val text = context.getString(cue.textRes)
        return if (cue.takesDistance && distanceBucketMeters > 0) {
            "${distancePrefix(context, distanceBucketMeters)}, $text"
        } else {
            text
        }
    }

    /** The spoken string for an approaching event: a zone, or a user report. */
    fun event(context: Context, type: RouteEventType, title: String?, distanceBucketMeters: Int): String {
        val name = title?.takeIf { it.isNotBlank() && type == RouteEventType.ZONE }
            ?: context.getString(eventNameRes(type))
        return if (distanceBucketMeters == 0) {
            name
        } else {
            context.getString(
                R.string.event_ahead_announcement,
                name,
                Formatters.distance(context, distanceBucketMeters.toDouble()),
            )
        }
    }

    private fun distancePrefix(context: Context, bucketMeters: Int): String =
        "${context.getString(R.string.nav_prefix_in)} ${Formatters.distance(context, bucketMeters.toDouble())}"

    /** The ordinal clause for a roundabout exit, when the step names one we have a clip for. */
    fun exitTextRes(action: String, exitNumber: Int?): Int? {
        if (!isRoundaboutEntry(action)) return null
        return when (exitNumber) {
            1 -> R.string.voice_exit_1
            2 -> R.string.voice_exit_2
            3 -> R.string.voice_exit_3
            4 -> R.string.voice_exit_4
            5 -> R.string.voice_exit_5
            6 -> R.string.voice_exit_6
            7 -> R.string.voice_exit_7
            ROUNDABOUT_EXIT_KEYS_MAX -> R.string.voice_exit_8
            else -> null
        }
    }

    fun isRoundaboutEntry(action: String): Boolean = action == "roundabout_enter" || action == "roundabout"

    /** The voice text (with niqqud) for an action key. */
    fun actionTextRes(action: String): Int = when (action) {
        "depart" -> R.string.voice_action_depart
        "turn_left" -> R.string.voice_action_turn_left
        "turn_right" -> R.string.voice_action_turn_right
        "slight_left" -> R.string.voice_action_slight_left
        "slight_right" -> R.string.voice_action_slight_right
        "sharp_left" -> R.string.voice_action_sharp_left
        "sharp_right" -> R.string.voice_action_sharp_right
        "keep_left" -> R.string.voice_action_keep_left
        "keep_right" -> R.string.voice_action_keep_right
        "continue" -> R.string.voice_action_continue
        "uturn" -> R.string.voice_action_uturn
        "roundabout", "roundabout_enter" ->
            R.string.voice_action_roundabout_enter
        "roundabout_exit" -> R.string.voice_action_roundabout_exit
        "merge" -> R.string.voice_action_merge
        "ramp_left" -> R.string.voice_action_ramp_left
        "ramp_right" -> R.string.voice_action_ramp_right
        "arrive" -> R.string.voice_action_arrive
        else -> R.string.voice_action_unknown
    }

    /** Arrival and departure never carry a street clause. */
    private fun speaksStreet(action: String): Boolean =
        action != "arrive" && action != "depart"

    private fun eventNameRes(type: RouteEventType): Int = when (type) {
        RouteEventType.ZONE -> R.string.event_zone
        RouteEventType.TRAFFIC -> R.string.report_traffic
        RouteEventType.POLICE -> R.string.report_police
        RouteEventType.ACCIDENT -> R.string.report_accident
        RouteEventType.HAZARD -> R.string.report_hazard
        RouteEventType.CLOSURE -> R.string.report_road_closure
        RouteEventType.SPEED_CAMERA -> R.string.event_speed_camera
        RouteEventType.RAIL_CROSSING -> R.string.event_rail_crossing
    }
}
