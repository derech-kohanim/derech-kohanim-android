package com.abaye.derechkohanim.data.mock

import com.abaye.derechkohanim.core.Geo
import com.abaye.derechkohanim.data.model.AuthProvider
import com.abaye.derechkohanim.data.model.AuthResponse
import com.abaye.derechkohanim.data.model.AuthTokens
import com.abaye.derechkohanim.data.model.GeoJsonLineString
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.data.model.ManeuverZone
import com.abaye.derechkohanim.data.model.MatchedRange
import com.abaye.derechkohanim.data.model.NavigationSession
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.PlaceQueryKind
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.ProviderInfo
import com.abaye.derechkohanim.data.model.RecentPlace
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.RoutePlan
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.RoutingEngine
import com.abaye.derechkohanim.data.model.TrafficLevel
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.model.User
import com.abaye.derechkohanim.data.model.UserPreferences
import com.abaye.derechkohanim.data.model.UserTier
import kotlin.math.roundToInt

/**
 * Fixed data so every screen can be opened, previewed and demonstrated without a server.
 * Only ever reached when BuildConfig.USE_MOCK_DATA is on.
 */
object MockData {

    val currentLocation = LatLng(31.7942, 35.2000)

    val savedPlaces = listOf(
        SavedPlace(
            id = "saved-home",
            label = "הבית",
            category = PlaceCategory.HOME,
            location = LatLng(31.7975, 35.2137),
            address = "רחוב מלכי ישראל 12, ירושלים",
            createdAt = "2026-01-04T08:00:00Z",
        ),
        SavedPlace(
            id = "saved-work",
            label = "העבודה",
            category = PlaceCategory.WORK,
            location = LatLng(31.7833, 35.2033),
            address = "כיכר ספרא 1, ירושלים",
            createdAt = "2026-01-04T08:05:00Z",
        ),
        SavedPlace(
            id = "saved-shul",
            label = "בית הכנסת המרכזי",
            category = PlaceCategory.SYNAGOGUE,
            location = LatLng(31.7889, 35.2050),
            address = "רחוב הרב קוק 8, ירושלים",
            createdAt = "2026-02-11T19:30:00Z",
        ),
    )

    val recentPlaces = listOf(
        RecentPlace(
            id = "recent-bnei-brak",
            title = "בני ברק",
            subtitle = "מחוז תל אביב",
            location = LatLng(32.0840, 34.8330),
            lastUsedAt = "2026-07-19T07:12:00Z",
            useCount = 9,
        ),
        RecentPlace(
            id = "recent-modiin",
            title = "מודיעין עילית",
            subtitle = "מחוז יהודה ושומרון",
            location = LatLng(31.9319, 35.0417),
            lastUsedAt = "2026-07-14T16:40:00Z",
            useCount = 4,
        ),
        RecentPlace(
            id = "recent-shaarei-tzedek",
            title = "מרכז רפואי שערי צדק",
            subtitle = "שמואל בית, ירושלים",
            location = LatLng(31.7869, 35.1800),
            lastUsedAt = "2026-07-02T09:05:00Z",
            useCount = 2,
        ),
    )

    /** The catalogue the fake server searches over. */
    private val catalogue = listOf(
        Triple("בני ברק", "מחוז תל אביב", PlaceCategory.CITY) to LatLng(32.0840, 34.8330),
        Triple("בית שמש", "מחוז ירושלים", PlaceCategory.CITY) to LatLng(31.7497, 34.9886),
        Triple("ביתר עילית", "מחוז יהודה ושומרון", PlaceCategory.CITY) to LatLng(31.6969, 35.1181),
        Triple("מודיעין עילית", "מחוז יהודה ושומרון", PlaceCategory.CITY) to LatLng(31.9319, 35.0417),
        Triple("אלעד", "מחוז המרכז", PlaceCategory.CITY) to LatLng(32.0500, 34.9500),
        Triple("רחוב רבי עקיבא", "בני ברק", PlaceCategory.ADDRESS) to LatLng(32.0853, 34.8353),
        Triple("רחוב חזון איש", "בני ברק", PlaceCategory.ADDRESS) to LatLng(32.0895, 34.8324),
        Triple("רחוב מלכי ישראל", "ירושלים", PlaceCategory.ADDRESS) to LatLng(31.7975, 35.2137),
        Triple("רחוב יפו", "ירושלים", PlaceCategory.ADDRESS) to LatLng(31.7857, 35.2145),
        Triple("ישיבת פוניבז׳", "בני ברק", PlaceCategory.POI) to LatLng(32.0916, 34.8353),
        Triple("בית הכנסת הגדול", "ירושלים", PlaceCategory.SYNAGOGUE) to LatLng(31.7801, 35.2202),
        Triple("מרכז רפואי שערי צדק", "ירושלים", PlaceCategory.POI) to LatLng(31.7869, 35.1800),
        Triple("קניון מלחה", "ירושלים", PlaceCategory.POI) to LatLng(31.7513, 35.1875),
        Triple("תחנת רכבת ירושלים יצחק נבון", "ירושלים", PlaceCategory.POI) to LatLng(31.7885, 35.2027),
        Triple("Ben Gurion Airport", "Lod", PlaceCategory.POI) to LatLng(32.0004, 34.8708),
        Triple("Tel Aviv", "Tel Aviv District", PlaceCategory.CITY) to LatLng(32.0853, 34.7818),
    )

    fun allSuggestions(near: LatLng?): List<PlaceSuggestion> = catalogue.map { (labels, location) ->
        val (title, subtitle, category) = labels
        PlaceSuggestion(
            id = "mock-" + title.hashCode().toString(),
            title = title,
            subtitle = subtitle,
            category = category,
            kind = PlaceQueryKind.TEXT,
            location = location,
            externalId = null,
            distanceMeters = near?.let { haversineMeters(it, location) },
        )
    }

    /**
     * Case insensitive substring match, with the matched range reported the same way the
     * server would report it.
     */
    fun search(query: String, near: LatLng?): List<PlaceSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return allSuggestions(near).mapNotNull { suggestion ->
            val index = suggestion.title.indexOf(trimmed, ignoreCase = true)
            val subtitleHit = suggestion.subtitle?.contains(trimmed, ignoreCase = true) == true
            if (index < 0 && !subtitleHit) return@mapNotNull null
            suggestion.copy(
                matchedRanges = if (index >= 0) {
                    listOf(MatchedRange(index, trimmed.length))
                } else {
                    emptyList()
                },
            )
        }.sortedWith(compareBy({ it.distanceMeters ?: Double.MAX_VALUE }, { it.title }))
    }

    val user = User(
        id = "mock-user",
        email = "kohen@example.org",
        displayName = "משתמש לדוגמה",
        tier = UserTier.REGISTERED,
        providers = listOf(AuthProvider.PASSWORD),
        preferences = UserPreferences(
            language = Language.HE,
            theme = ThemePreference.SYSTEM,
            stringency = Stringency.TAHARAT,
            trafficAware = true,
            voiceEnabled = true,
            voiceId = "he-default",
        ),
        acceptedTermsVersion = TERMS_VERSION,
        createdAt = "2026-01-04T08:00:00Z",
    )

    val authResponse = AuthResponse(
        user = user,
        tokens = AuthTokens("mock-access-token", "mock-refresh-token", expiresIn = 3600),
    )

    val providers = listOf(
        ProviderInfo(RoutingEngine.MOCK, available = true, isDefault = true, attribution = "Mock data", supportsNativeAvoidance = true, supportsLiveTraffic = false),
        ProviderInfo(RoutingEngine.HERE, available = false, isDefault = false, attribution = "HERE", supportsNativeAvoidance = true, supportsLiveTraffic = true),
        ProviderInfo(RoutingEngine.GRAPHHOPPER, available = false, isDefault = false, attribution = "GraphHopper", supportsNativeAvoidance = false, supportsLiveTraffic = false),
    )

    fun session(routeId: String, simulated: Boolean) = NavigationSession(
        id = "mock-session",
        routeId = routeId,
        status = NavigationStatus.NAVIGATING,
        simulated = simulated,
        startedAt = "2026-07-22T07:00:00Z",
    )

    /**
     * A route that is verified clean and pays a visible detour, so the review screen shows
     * the interesting case rather than the trivial one.
     */
    fun route(
        origin: LatLng,
        destination: LatLng,
        clean: Boolean = true,
        trafficAware: Boolean = false,
    ): RouteResult {
        val geometry = interpolate(origin, destination, steps = 48)
        val distance = haversineMeters(origin, destination) * if (clean) 1.34 else 1.0
        // Traffic-aware pricing is slower here, so the toggle visibly changes the travel time.
        val trafficFactor = if (trafficAware) 1.18 else 1.0
        return RouteResult(
            id = "mock-route",
            geometry = GeoJsonLineString(coordinates = geometry.map { listOf(it.lng, it.lat) }),
            maneuvers = maneuvers(geometry),
            distanceMeters = distance,
            durationSeconds = distance / 11.0 * trafficFactor,
            verifiedClean = clean,
            reason = if (clean) null else "avoidance_failed",
            attribution = "Mock data",
            engine = RoutingEngine.MOCK,
            detourRatio = if (clean) 1.34 else null,
            requiredVoiceKeys = listOf(
                "prefix.in", "dist.500m", "dist.200m", "dist.50m",
                "action.turn_right", "action.turn_left", "action.continue", "action.arrive",
                "zone.ahead",
            ),
        )
    }

    /**
     * A plan with up to three verified alternatives, plus the unavoided route when asked for.
     * Mirrors what POST /api/routes/compute returns, so the review screen has real data to
     * drive the picker, the toll flag and the unavoided overlay without a backend.
     */
    fun routePlan(request: RouteRequest): RoutePlan {
        val clean = request.stringency != Stringency.MACHMIRIM
        val base = route(request.origin, request.destination, clean, request.trafficAware)
            .copy(id = "mock-route-0", label = "fastest", avoidsTolls = request.avoidTolls)
        val routes = if (clean) {
            listOf(
                base,
                variant(base, "mock-route-1", "shortest", 0.006, 0.94, 1.08)
                    .copy(avoidsTolls = request.avoidTolls),
                variant(base, "mock-route-2", "alt", -0.008, 1.06, 1.02)
                    .copy(avoidsTolls = request.avoidTolls),
            )
        } else {
            listOf(base)
        }
        // The unavoided route only makes sense when avoidance actually changed the outcome,
        // which in the mock is the clean case (the naive route crosses a zone).
        val unavoided = if (request.includeUnavoided == true && clean) {
            route(request.origin, request.destination, clean = false, trafficAware = request.trafficAware)
                .copy(id = "mock-unavoided", label = "unavoided")
        } else {
            null
        }
        return RoutePlan(routes = routes, unavoidedRoute = unavoided)
    }

    private fun variant(
        base: RouteResult,
        id: String,
        label: String,
        latShift: Double,
        distFactor: Double,
        durFactor: Double,
    ): RouteResult {
        val last = base.geometry.coordinates.lastIndex.coerceAtLeast(1)
        val shifted = base.geometry.coordinates.mapIndexed { i, c ->
            val t = i.toDouble() / last
            val bow = Math.sin(t * Math.PI) * latShift
            listOf(c[0], c[1] + bow)
        }
        return base.copy(
            id = id,
            label = label,
            geometry = GeoJsonLineString(coordinates = shifted),
            distanceMeters = base.distanceMeters * distFactor,
            durationSeconds = base.durationSeconds * durFactor,
        )
    }

    private fun maneuvers(geometry: List<LatLng>): List<Maneuver> {
        val picks = listOf(0, 8, 18, 30, 40, geometry.lastIndex)
        val actions = listOf("depart", "turn_right", "continue", "turn_left", "continue", "arrive")
        val streets = listOf("שדרות הרצל", "רחוב הרב קוק", "כביש 1", "רחוב רבי עקיבא", "כביש 4", null)
        return picks.mapIndexed { i, index ->
            Maneuver(
                id = "mock-maneuver-$i",
                instruction = "",
                action = actions[i],
                point = geometry[index],
                distanceMeters = if (i == 0) 0.0 else 420.0 * i,
                geometryIndex = index,
                street = streets[i],
                zone = if (i == 3) {
                    ManeuverZone(
                        name = "הר המנוחות",
                        nameHe = "הר המנוחות",
                        stringency = Stringency.BASIC,
                        city = "ירושלים",
                        description = "בית קברות מרכזי - קברי כהנים לאורך הכביש",
                        danger = false,
                    )
                } else {
                    null
                },
                voiceKeys = listOf("prefix.in", "dist.200m", "action.${actions[i]}"),
            )
        }
    }

    private fun interpolate(from: LatLng, to: LatLng, steps: Int): List<LatLng> =
        (0..steps).map { i ->
            val t = i.toDouble() / steps
            // A slight bow, so the drawn line does not look like a ruler.
            val bow = Math.sin(t * Math.PI) * 0.012
            LatLng(
                lat = from.lat + (to.lat - from.lat) * t + bow,
                lng = from.lng + (to.lng - from.lng) * t,
            )
        }

    /** One small polygon per stringency layer, with rich fields, so tap cards have content. */
    val zoneFeatures: List<com.abaye.derechkohanim.data.model.ZoneFeature> = listOf(
        zoneFeature(
            1, "הר המנוחות", Stringency.BASIC, LatLng(31.7960, 35.1830), 0.010,
            city = "ירושלים", street = "שדרות הרצל",
            description = "בית הקברות הר המנוחות. קברי כהנים לאורך הכביש, איסור גמור לכל הדעות.",
        ),
        zoneFeature(
            2, "סנהדריה", Stringency.TAHARAT, LatLng(31.7995, 35.2130), 0.008,
            city = "ירושלים", street = "רחוב הרב סורוצקין",
            description = "מערת הסנהדרין וקברים סמוכים, לפי הכרעת ועדת טהרת הכהנים.",
        ),
        zoneFeature(
            3, "גבעת שאול", Stringency.MACHMIRIM, LatLng(31.7930, 35.1930), 0.009,
            city = "ירושלים", street = null,
            description = "אזור שיש בו ספק קברים, למחמירים בלבד.",
        ),
    )

    /** Danger zones (Area A), a separate layer with their own rich fields. */
    val dangerZoneFeatures: List<com.abaye.derechkohanim.data.model.DangerZoneFeature> = listOf(
        dangerZoneFeature(
            101, "שטח A - קלנדיה", LatLng(31.8620, 35.2210), 0.012,
            city = "קלנדיה",
            description = "שטח A בשליטה אזרחית וביטחונית פלסטינית. כניסת ישראלים אסורה ומסוכנת.",
        ),
        dangerZoneFeature(
            102, "שטח A - אבו דיס", LatLng(31.7620, 35.2660), 0.010,
            city = "אבו דיס",
            description = "שטח A. אין להיכנס, סכנת חיים.",
        ),
    )

    private fun zoneFeature(
        id: Int,
        nameHe: String,
        stringency: Stringency,
        center: LatLng,
        half: Double,
        city: String?,
        street: String?,
        description: String?,
    ): com.abaye.derechkohanim.data.model.ZoneFeature =
        com.abaye.derechkohanim.data.model.ZoneFeature(
            properties = com.abaye.derechkohanim.data.model.ZoneProperties(
                id = id,
                name = nameHe,
                nameHe = nameHe,
                stringency = stringency,
                city = city,
                street = street,
                description = description,
            ),
            geometry = com.abaye.derechkohanim.data.model.GeoJsonPolygon(coordinates = listOf(square(center, half))),
        )

    private fun dangerZoneFeature(
        id: Int,
        name: String,
        center: LatLng,
        half: Double,
        city: String?,
        description: String?,
    ): com.abaye.derechkohanim.data.model.DangerZoneFeature =
        com.abaye.derechkohanim.data.model.DangerZoneFeature(
            properties = com.abaye.derechkohanim.data.model.DangerZoneProperties(
                id = id,
                name = name,
                city = city,
                description = description,
            ),
            geometry = com.abaye.derechkohanim.data.model.GeoJsonPolygon(coordinates = listOf(square(center, half))),
        )

    /** A closed square ring around a centre, [lng, lat] per RFC 7946. */
    private fun square(center: LatLng, half: Double): List<List<Double>> = listOf(
        listOf(center.lng - half, center.lat - half),
        listOf(center.lng + half, center.lat - half),
        listOf(center.lng + half, center.lat + half),
        listOf(center.lng - half, center.lat + half),
        listOf(center.lng - half, center.lat - half),
    )

    /** Layers are cumulative, so a stricter selection includes the looser ones. */
    fun zonesFor(stringency: Stringency): List<com.abaye.derechkohanim.data.model.ZoneFeature> {
        val included = when (stringency) {
            Stringency.BASIC -> setOf(Stringency.BASIC)
            Stringency.TAHARAT -> setOf(Stringency.BASIC, Stringency.TAHARAT)
            Stringency.MACHMIRIM -> setOf(Stringency.BASIC, Stringency.TAHARAT, Stringency.MACHMIRIM)
        }
        return zoneFeatures.filter { it.properties.stringency in included }
    }

    /**
     * Route events keyed to the actual geometry, so markers sit on the drawn route and the
     * along-route distances match. Mirrors what GET /api/routes/{id}/events would return.
     */
    fun eventBundle(route: RouteResult): RouteEventBundle {
        val geometry = route.geometry.points()
        if (geometry.size < 4) return RouteEventBundle.empty(route.id)
        val cumulative = cumulativeDistances(geometry)

        val events = mutableListOf<RouteEvent>()
        route.maneuvers.forEachIndexed { index, maneuver ->
            val zone = maneuver.zone ?: return@forEachIndexed
            events += RouteEvent(
                id = "zone-$index",
                type = RouteEventType.ZONE,
                location = maneuver.point,
                routeDistanceMeters = cumulative[maneuver.geometryIndex.coerceIn(0, cumulative.lastIndex)],
                label = zone.name,
                labelHe = zone.nameHe,
            )
        }
        val policeIndex = geometry.size / 3
        val hazardIndex = geometry.size * 2 / 3
        events += RouteEvent(
            id = "report-police",
            type = RouteEventType.POLICE,
            location = geometry[policeIndex],
            routeDistanceMeters = cumulative[policeIndex],
        )
        events += RouteEvent(
            id = "report-hazard",
            type = RouteEventType.HAZARD,
            location = geometry[hazardIndex],
            routeDistanceMeters = cumulative[hazardIndex],
        )

        val traffic = listOf(
            TrafficSegment(geometry.size / 4, geometry.size / 3, TrafficLevel.MODERATE),
            TrafficSegment(geometry.size / 2, geometry.size * 3 / 5, TrafficLevel.HEAVY),
        )

        return RouteEventBundle(
            routeId = route.id,
            events = events,
            traffic = traffic,
            delaySeconds = 300.0,
        )
    }

    private fun cumulativeDistances(geometry: List<LatLng>): List<Double> {
        val out = DoubleArray(geometry.size)
        for (i in 1 until geometry.size) {
            out[i] = out[i - 1] + Geo.distanceMeters(geometry[i - 1], geometry[i])
        }
        return out.toList()
    }

    fun haversineMeters(a: LatLng, b: LatLng): Double =
        (Geo.distanceMeters(a, b) * 10).roundToInt() / 10.0

    const val TERMS_VERSION = "2026-05-01"
}
