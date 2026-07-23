package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.model.CreateReportRequest
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import javax.inject.Inject

/**
 * On-route events and community reports, served per route by the API
 * (GET /api/routes/{id}/events, POST /api/routes/{id}/reports). Mirrors the shared
 * RouteEventBundle contract.
 */
interface RouteEventsRepository {
    suspend fun events(routeId: String): DkResult<RouteEventBundle>
    suspend fun report(request: CreateReportRequest): DkResult<RouteEvent>
}

class RemoteRouteEventsRepository @Inject constructor(
    private val api: DerechKohanimApi,
) : RouteEventsRepository {

    override suspend fun events(routeId: String): DkResult<RouteEventBundle> =
        apiCall { api.routeEvents(routeId) }

    override suspend fun report(request: CreateReportRequest): DkResult<RouteEvent> =
        apiCall { api.createReport(request.routeId, request) }
}
