package com.abaye.derechkohanim.voice

import com.abaye.derechkohanim.R

/**
 * A fixed spoken alert or status message: a zone warning, a road alert, a GPS or route-state
 * change. Each has one recorded clip key from the shared voice contract and a text for the
 * device speech fallback.
 *
 * [takesDistance] cues are spoken as "in N metres, <cue>" when a distance is known.
 */
enum class VoiceCue(
    val clipKey: String,
    val textRes: Int,
    val takesDistance: Boolean = false,
) {
    ZONE_ENTER("zone.enter", R.string.voice_cue_zone_enter),
    ZONE_EXIT("zone.exit", R.string.voice_cue_zone_exit),
    ZONE_AHEAD("zone.ahead", R.string.voice_cue_zone_ahead, takesDistance = true),
    SPEED_CAMERA(
        "alert.speed_camera",
        R.string.voice_cue_speed_camera,
        takesDistance = true,
    ),
    RAIL_CROSSING(
        "alert.rail_crossing",
        R.string.voice_cue_rail_crossing,
        takesDistance = true,
    ),
    SPEEDING("alert.speeding", R.string.voice_cue_speeding),
    GPS_LOST("misc.gps_lost", R.string.voice_cue_gps_lost),
    GPS_RECOVERED("misc.gps_recovered", R.string.voice_cue_gps_recovered),
    BACK_ON_ROUTE("misc.back_on_route", R.string.voice_cue_back_on_route),
    RETURN_TO_ROUTE(
        "misc.return_to_route",
        R.string.voice_cue_return_to_route,
    ),
    ;

    companion object {
        /** Every cue clip, so a drive can prefetch them all before it might lose connectivity. */
        val ALL_CLIP_KEYS: List<String> = entries.map { it.clipKey }
    }
}
