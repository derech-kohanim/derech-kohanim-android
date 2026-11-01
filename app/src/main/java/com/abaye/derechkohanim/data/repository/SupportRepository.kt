package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.SupportReportRequest
import com.abaye.derechkohanim.data.model.SupportReportResponse
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Sends reports and feedback to the support desk. Signed-in users only; 429 when rate limited. */
interface SupportRepository {
    suspend fun sendReport(request: SupportReportRequest): DkResult<SupportReportResponse>
}

class RemoteSupportRepository @Inject constructor(
    private val api: DerechKohanimApi,
) : SupportRepository {
    override suspend fun sendReport(request: SupportReportRequest): DkResult<SupportReportResponse> =
        apiCall { api.sendSupportReport(request) }
}

/** One tap on "mark a problem" while driving: where and when, for the post-drive prompt. */
data class DriveMark(val routeId: String?, val location: LatLng?, val atMillis: Long)

/**
 * The problems marked during the current drive. Kept in memory for the life of the app process:
 * a mark only matters until the drive ends and the driver is asked whether to describe it.
 */
@Singleton
class DriveMarkStore @Inject constructor() {
    private val marks = mutableListOf<DriveMark>()

    @Synchronized
    fun add(mark: DriveMark) {
        marks += mark
    }

    @Synchronized
    fun all(): List<DriveMark> = marks.toList()

    @Synchronized
    fun clear() {
        marks.clear()
    }
}
