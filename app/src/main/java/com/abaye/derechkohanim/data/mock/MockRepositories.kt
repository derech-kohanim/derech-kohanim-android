package com.abaye.derechkohanim.data.mock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionSnapshot
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.CreateRecentPlaceRequest
import com.abaye.derechkohanim.data.model.CreateReportRequest
import com.abaye.derechkohanim.data.model.CreateSavedPlaceRequest
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.NavigationProgressRequest
import com.abaye.derechkohanim.data.model.NavigationProgressResponse
import com.abaye.derechkohanim.data.model.NavigationSession
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.ProviderInfo
import com.abaye.derechkohanim.data.model.RecentPlace
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RoutePlan
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TermsDocument
import com.abaye.derechkohanim.data.model.User
import com.abaye.derechkohanim.data.model.UserPreferences
import com.abaye.derechkohanim.data.model.VoicePackManifest
import com.abaye.derechkohanim.data.model.VoiceResolveResponse
import com.abaye.derechkohanim.data.model.ZoneCollection
import com.abaye.derechkohanim.data.repository.AuthRepository
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.data.repository.RouteEventsRepository
import com.abaye.derechkohanim.data.repository.RoutingRepository
import com.abaye.derechkohanim.data.repository.VoiceRepository
import com.abaye.derechkohanim.nav.TripStore
import javax.inject.Inject

private const val FAKE_LATENCY_MS = 220L

class MockPlacesRepository @Inject constructor() : PlacesRepository {

    override suspend fun search(
        query: String,
        language: Language,
        near: LatLng?,
        sessionToken: String,
    ): DkResult<List<PlaceSuggestion>> {
        delay(FAKE_LATENCY_MS)
        return DkResult.Success(MockData.search(query, near))
    }

    override suspend fun details(
        id: String,
        language: Language,
        sessionToken: String,
    ): DkResult<PlaceSuggestion> {
        delay(FAKE_LATENCY_MS)
        val hit = MockData.allSuggestions(null).firstOrNull { it.id == id }
        return if (hit == null) DkResult.Failure("not_found") else DkResult.Success(hit)
    }

    override suspend fun savedPlaces(): DkResult<List<SavedPlace>> =
        DkResult.Success(MockData.savedPlaces)

    override suspend fun addSavedPlace(request: CreateSavedPlaceRequest): DkResult<SavedPlace> =
        DkResult.Success(
            SavedPlace(
                id = "saved-${request.hashCode()}",
                label = request.label,
                category = request.category,
                location = request.location,
                address = request.address,
                createdAt = "2026-07-23T00:00:00Z",
            )
        )

    override suspend fun removeSavedPlace(id: String): DkResult<Unit> = DkResult.Success(Unit)

    override suspend fun recentPlaces(): DkResult<List<RecentPlace>> =
        DkResult.Success(MockData.recentPlaces)

    override suspend fun addRecentPlace(request: CreateRecentPlaceRequest): DkResult<RecentPlace> =
        DkResult.Success(
            RecentPlace(
                id = "recent-${request.hashCode()}",
                title = request.title,
                subtitle = request.subtitle,
                location = request.location,
                lastUsedAt = "2026-07-23T00:00:00Z",
                useCount = 1,
            )
        )

    override suspend fun removeRecentPlace(id: String): DkResult<Unit> = DkResult.Success(Unit)
}

class MockRouteEventsRepository @Inject constructor(
    private val tripStore: TripStore,
) : RouteEventsRepository {

    override suspend fun events(routeId: String): DkResult<RouteEventBundle> {
        delay(FAKE_LATENCY_MS)
        val route = tripStore.route.value
        return DkResult.Success(
            if (route == null) RouteEventBundle.empty(routeId) else MockData.eventBundle(route)
        )
    }

    override suspend fun report(request: CreateReportRequest): DkResult<RouteEvent> =
        DkResult.Success(
            RouteEvent(
                id = "user-${request.hashCode()}",
                type = request.type,
                location = request.location,
                routeDistanceMeters = 0.0,
                reportedByUser = true,
            )
        )
}

class MockRoutingRepository @Inject constructor() : RoutingRepository {

    override suspend fun computeRoute(request: RouteRequest): DkResult<RoutePlan> {
        delay(FAKE_LATENCY_MS * 2)
        return DkResult.Success(MockData.routePlan(request))
    }

    override suspend fun providers(): DkResult<List<ProviderInfo>> =
        DkResult.Success(MockData.providers)

    override suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection> =
        DkResult.Success(ZoneCollection(features = MockData.zonesFor(stringency), version = 1))

    override suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession> =
        DkResult.Success(MockData.session(routeId, simulated))

    override suspend fun reportProgress(
        request: NavigationProgressRequest,
    ): DkResult<NavigationProgressResponse> = DkResult.Success(
        NavigationProgressResponse(
            status = NavigationStatus.NAVIGATING,
            currentManeuverIndex = 1,
            distanceToManeuverMeters = 240.0,
            distanceRemainingMeters = 4_800.0,
            durationRemainingSeconds = 460.0,
        )
    )

    override suspend fun endSession(sessionId: String): DkResult<NavigationSession> =
        DkResult.Success(MockData.session(sessionId, simulated = false).copy(status = NavigationStatus.ARRIVED))
}

class MockAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
) : AuthRepository {

    override val session: Flow<SessionSnapshot> = sessionStore.snapshots

    override suspend fun login(email: String, password: String): DkResult<User> = signIn()

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?,
        acceptedTermsVersion: String,
    ): DkResult<User> = signIn()

    override suspend fun loginWithGoogle(
        idToken: String,
        acceptedTermsVersion: String?,
    ): DkResult<User> = signIn()

    override suspend fun terms(language: Language): DkResult<TermsDocument> = DkResult.Success(
        TermsDocument(
            version = MockData.TERMS_VERSION,
            language = language,
            title = "תנאי שימוש",
            body = "מסמך לדוגמה. הגרסה המחייבת מגיעה מהשרת.",
            updatedAt = "2026-05-01T00:00:00Z",
        )
    )

    override suspend fun acceptTerms(version: String): DkResult<User> {
        sessionStore.setAcceptedTermsVersion(version)
        return DkResult.Success(MockData.user)
    }

    override suspend fun updatePreferences(preferences: UserPreferences): DkResult<User> =
        DkResult.Success(MockData.user.copy(preferences = preferences))

    override suspend fun signOut() {
        sessionStore.clearSession()
    }

    private suspend fun signIn(): DkResult<User> {
        delay(FAKE_LATENCY_MS)
        sessionStore.setTokens(
            MockData.authResponse.tokens.accessToken,
            MockData.authResponse.tokens.refreshToken,
        )
        sessionStore.setAcceptedTermsVersion(MockData.TERMS_VERSION)
        return DkResult.Success(MockData.user)
    }
}

class MockVoiceRepository @Inject constructor() : VoiceRepository {

    override suspend fun manifest(
        language: Language,
        voiceId: String,
        sinceVersion: Int?,
    ): DkResult<VoicePackManifest> = DkResult.Success(
        VoicePackManifest(
            language = language,
            voiceId = voiceId,
            version = 1,
            clips = emptyList(),
            updatedAt = "2026-05-01T00:00:00Z",
        )
    )

    override suspend fun resolve(
        language: Language,
        voiceId: String,
        keys: List<String>,
    ): DkResult<VoiceResolveResponse> =
        DkResult.Success(VoiceResolveResponse(clips = emptyList(), missing = keys))
}
