package com.abaye.derechkohanim.data.remote

import com.abaye.derechkohanim.data.model.AcceptTermsRequest
import com.abaye.derechkohanim.data.model.AuthResponse
import com.abaye.derechkohanim.data.model.AuthTokens
import com.abaye.derechkohanim.data.model.CreateRecentPlaceRequest
import com.abaye.derechkohanim.data.model.CreateReportRequest
import com.abaye.derechkohanim.data.model.CreateSavedPlaceRequest
import com.abaye.derechkohanim.data.model.GoogleAuthRequest
import com.abaye.derechkohanim.data.model.LoginRequest
import com.abaye.derechkohanim.data.model.LogoutRequest
import com.abaye.derechkohanim.data.model.NavigationProgressRequest
import com.abaye.derechkohanim.data.model.NavigationProgressResponse
import com.abaye.derechkohanim.data.model.NavigationSession
import com.abaye.derechkohanim.data.model.NavigationSessionRequest
import com.abaye.derechkohanim.data.model.PlaceSearchResponse
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.ProviderInfo
import com.abaye.derechkohanim.data.model.RecentPlace
import com.abaye.derechkohanim.data.model.RefreshRequest
import com.abaye.derechkohanim.data.model.RegisterRequest
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.TermsDocument
import com.abaye.derechkohanim.data.model.User
import com.abaye.derechkohanim.data.model.UserPreferences
import com.abaye.derechkohanim.data.model.VoicePackManifest
import com.abaye.derechkohanim.data.model.VoiceResolveRequest
import com.abaye.derechkohanim.data.model.VoiceResolveResponse
import com.abaye.derechkohanim.data.model.ZoneCollection
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The real API surface. Paths and verbs follow the deployed server, not the spec draft:
 * places search and details are GET, terms acceptance is its own POST, a session is ended
 * with an /end POST, and voice resolve is /voice/clips/resolve.
 *
 * `near` is passed as "lat,lng" in a single query param; see PlacesRepository and the README.
 */
interface DerechKohanimApi {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokens

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @GET("auth/me")
    suspend fun me(): User

    @GET("auth/terms")
    suspend fun terms(@Query("language") language: String): TermsDocument

    @POST("auth/accept-terms")
    suspend fun acceptTerms(@Body body: AcceptTermsRequest): User

    // Users (profile and preferences persist here)
    @GET("users/me")
    suspend fun currentUser(): User

    @PATCH("users/me")
    suspend fun updateUser(@Body body: User): User

    @PATCH("users/me/preferences")
    suspend fun updatePreferences(@Body body: UserPreferences): User

    // Places
    @GET("places/search")
    suspend fun searchPlaces(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("near") near: String? = null,
        @Query("sessionToken") sessionToken: String? = null,
        @Query("limit") limit: Int? = null,
    ): PlaceSearchResponse

    @GET("places/details")
    suspend fun placeDetails(
        @Query("id") id: String,
        @Query("language") language: String,
        @Query("sessionToken") sessionToken: String? = null,
    ): PlaceSuggestion

    @GET("places/saved")
    suspend fun savedPlaces(): List<SavedPlace>

    @POST("places/saved")
    suspend fun createSavedPlace(@Body body: CreateSavedPlaceRequest): SavedPlace

    @DELETE("places/saved/{id}")
    suspend fun deleteSavedPlace(@Path("id") id: String)

    @GET("places/recent")
    suspend fun recentPlaces(): List<RecentPlace>

    @POST("places/recent")
    suspend fun createRecentPlace(@Body body: CreateRecentPlaceRequest): RecentPlace

    @DELETE("places/recent/{id}")
    suspend fun deleteRecentPlace(@Path("id") id: String)

    // Zones and routing
    @GET("zones")
    suspend fun zones(
        @Query("stringency") stringency: String,
        @Query("sinceVersion") sinceVersion: Int? = null,
    ): ZoneCollection

    // Returns a RoutePlan; decoded as JsonElement so the repository can also accept the old
    // single RouteResult shape during the server rollout.
    @POST("routes/compute")
    suspend fun computeRoute(@Body body: RouteRequest): kotlinx.serialization.json.JsonElement

    @GET("providers")
    suspend fun providers(): List<ProviderInfo>

    // Route events and reports
    @GET("routes/{id}/events")
    suspend fun routeEvents(@Path("id") routeId: String): RouteEventBundle

    @POST("routes/{id}/reports")
    suspend fun createReport(
        @Path("id") routeId: String,
        @Body body: CreateReportRequest,
    ): RouteEvent

    // Navigation sessions
    @POST("nav/sessions")
    suspend fun createSession(@Body body: NavigationSessionRequest): NavigationSession

    @POST("nav/sessions/{id}/progress")
    suspend fun reportProgress(
        @Path("id") sessionId: String,
        @Body body: NavigationProgressRequest,
    ): NavigationProgressResponse

    @POST("nav/sessions/{id}/end")
    suspend fun endSession(@Path("id") sessionId: String): NavigationSession

    // Voice
    @GET("voice/packs")
    suspend fun voicePack(
        @Query("language") language: String,
        @Query("voice") voiceId: String,
        @Query("sinceVersion") sinceVersion: Int? = null,
    ): VoicePackManifest

    @POST("voice/clips/resolve")
    suspend fun resolveVoiceClips(@Body body: VoiceResolveRequest): VoiceResolveResponse
}
