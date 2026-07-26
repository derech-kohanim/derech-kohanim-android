package com.abaye.derechkohanim.data.model

/**
 * Which map renderer the app draws with.
 *
 * As a user *setting* all three values are valid: [AUTOMATIC] (recommended default) uses Google
 * but falls back to OpenFreeMap when a NetFree network is detected and the Google map failed to
 * load; [GOOGLE] and [OPENFREEMAP] force one renderer regardless of the network.
 *
 * As the *effective* provider (what actually draws) only [GOOGLE] and [OPENFREEMAP] ever occur -
 * [AUTOMATIC] is resolved to one of them by `MapProviderState`.
 */
enum class MapProvider {
    AUTOMATIC,
    GOOGLE,
    OPENFREEMAP,
}
