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
