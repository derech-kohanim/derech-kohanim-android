package com.abaye.derechkohanim.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val ROUTE_REVIEW = "route"
    const val NAVIGATION = "navigation"
    const val SIMULATION = "simulation"
    const val SETTINGS = "settings"

    /** The report and feedback form, with the context it was opened from as optional arguments. */
    const val REPORT = "report"
    const val REPORT_ARG_KIND = "kind"
    const val REPORT_ARG_ROUTE = "routeId"
    const val REPORT_ARG_ZONE = "zoneId"
    const val REPORT_ARG_ZONE_NAME = "zoneName"
    const val REPORT_ARG_LAT = "lat"
    const val REPORT_ARG_LNG = "lng"
    const val REPORT_ARG_MARKED = "marked"
    val REPORT_ARGS = listOf(REPORT_ARG_KIND, REPORT_ARG_ROUTE, REPORT_ARG_ZONE, REPORT_ARG_ZONE_NAME, REPORT_ARG_LAT, REPORT_ARG_LNG, REPORT_ARG_MARKED)
    val REPORT_PATTERN = REPORT + "?" + REPORT_ARGS.joinToString("&") { "$it={$it}" }

    /** The report form opened with whatever context is known. */
    fun report(
        kind: String? = null,
        routeId: String? = null,
        zoneId: Int? = null,
        zoneName: String? = null,
        location: com.abaye.derechkohanim.data.model.LatLng? = null,
        marked: Boolean = false,
    ): String {
        val values = mapOf(
            REPORT_ARG_KIND to kind,
            REPORT_ARG_ROUTE to routeId,
            REPORT_ARG_ZONE to zoneId?.toString(),
            REPORT_ARG_ZONE_NAME to zoneName,
            REPORT_ARG_LAT to location?.lat?.toString(),
            REPORT_ARG_LNG to location?.lng?.toString(),
            REPORT_ARG_MARKED to if (marked) "1" else null,
        ).filterValues { it != null }
        if (values.isEmpty()) return REPORT
        return REPORT + "?" + values.entries.joinToString("&") { (k, v) -> "$k=" + android.net.Uri.encode(v) }
    }

    /** A settings sub-screen; the argument is a SettingsSection route segment. */
    const val SETTINGS_ARG_SECTION = "section"
    const val SETTINGS_SECTION_PATTERN = "$SETTINGS/{$SETTINGS_ARG_SECTION}"
    fun settingsSection(section: String): String = "$SETTINGS/$section"

    /**
     * The policy documents and the deletion form. Reachable before the terms are accepted and
     * without an account, because that is exactly who needs them: a reader deciding whether to
     * accept, and someone who has already signed out.
     */
    const val TERMS = "legal/terms"
    const val PRIVACY = "legal/privacy"
    const val DELETE_ACCOUNT = "account/delete"

    /** Nav argument on the search route selecting what a picked place becomes. */
    const val SEARCH_ARG_MODE = "mode"
    const val SEARCH_MODE_DESTINATION = "destination"
    const val SEARCH_MODE_ORIGIN = "origin"

    /** Back-stack result Home reads after origin-mode search set the manual start point. */
    const val RESULT_ORIGIN_SET = "origin_set"

    /** The search route pattern with its optional mode argument (defaults to destination). */
    const val SEARCH_PATTERN = "$SEARCH?$SEARCH_ARG_MODE={$SEARCH_ARG_MODE}"

    /**
     * Builds a concrete search route. In origin mode the chosen place becomes the manual ORIGIN
     * (the no-GPS start point) and the flow returns to Home; otherwise it is the destination.
     */
    fun search(originMode: Boolean = false): String =
        if (originMode) "$SEARCH?$SEARCH_ARG_MODE=$SEARCH_MODE_ORIGIN" else SEARCH
}
