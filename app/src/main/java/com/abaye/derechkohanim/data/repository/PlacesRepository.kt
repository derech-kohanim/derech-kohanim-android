package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.model.CreateRecentPlaceRequest
import com.abaye.derechkohanim.data.model.CreateSavedPlaceRequest
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.RecentPlace
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import java.util.Locale
import javax.inject.Inject

interface PlacesRepository {
    suspend fun search(
        query: String,
        language: Language,
        near: LatLng?,
        sessionToken: String,
    ): DkResult<List<PlaceSuggestion>>

    suspend fun details(
        id: String,
        language: Language,
        sessionToken: String,
    ): DkResult<PlaceSuggestion>

    suspend fun savedPlaces(): DkResult<List<SavedPlace>>
    suspend fun addSavedPlace(request: CreateSavedPlaceRequest): DkResult<SavedPlace>
    suspend fun removeSavedPlace(id: String): DkResult<Unit>

    suspend fun recentPlaces(): DkResult<List<RecentPlace>>
    suspend fun addRecentPlace(request: CreateRecentPlaceRequest): DkResult<RecentPlace>
    suspend fun removeRecentPlace(id: String): DkResult<Unit>
}

class RemotePlacesRepository @Inject constructor(
    private val api: DerechKohanimApi,
) : PlacesRepository {

    override suspend fun search(
        query: String,
        language: Language,
        near: LatLng?,
        sessionToken: String,
    ): DkResult<List<PlaceSuggestion>> = apiCall {
        api.searchPlaces(
            query = query,
            language = language.tag,
            near = near?.toQueryParam(),
            sessionToken = sessionToken,
            limit = SEARCH_LIMIT,
        ).suggestions
    }

    override suspend fun details(
        id: String,
        language: Language,
        sessionToken: String,
    ): DkResult<PlaceSuggestion> = apiCall {
        api.placeDetails(id = id, language = language.tag, sessionToken = sessionToken)
    }

    override suspend fun savedPlaces(): DkResult<List<SavedPlace>> = apiCall { api.savedPlaces() }

    override suspend fun addSavedPlace(request: CreateSavedPlaceRequest): DkResult<SavedPlace> =
        apiCall { api.createSavedPlace(request) }

    override suspend fun removeSavedPlace(id: String): DkResult<Unit> =
        apiCall { api.deleteSavedPlace(id) }

    override suspend fun recentPlaces(): DkResult<List<RecentPlace>> = apiCall { api.recentPlaces() }

    override suspend fun addRecentPlace(request: CreateRecentPlaceRequest): DkResult<RecentPlace> =
        apiCall { api.createRecentPlace(request) }

    override suspend fun removeRecentPlace(id: String): DkResult<Unit> =
        apiCall { api.deleteRecentPlace(id) }

    private companion object {
        const val SEARCH_LIMIT = 8
    }
}

/** The search endpoint biases by a single "lat,lng" query param. */
fun LatLng.toQueryParam(): String = "%.6f,%.6f".format(Locale.US, lat, lng)

fun PlaceSuggestion.toCreateRecentRequest(): CreateRecentPlaceRequest? {
    val point = location ?: return null
    return CreateRecentPlaceRequest(title = title, subtitle = subtitle, location = point)
}

fun PlaceSuggestion.toCreateSavedRequest(
    category: PlaceCategory = this.category,
): CreateSavedPlaceRequest? {
    val point = location ?: return null
    return CreateSavedPlaceRequest(
        label = title,
        category = category,
        location = point,
        address = subtitle,
    )
}
