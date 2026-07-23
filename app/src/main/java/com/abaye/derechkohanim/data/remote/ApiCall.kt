package com.abaye.derechkohanim.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.data.model.ApiError
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Turns transport failures into the stable codes the interface translates. Cancellation is
 * rethrown so a superseded search really is dropped rather than reported as an error.
 */
suspend fun <T> apiCall(block: suspend () -> T): DkResult<T> = try {
    DkResult.Success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (io: IOException) {
    DkResult.Failure(ErrorCode.NETWORK, io)
} catch (http: HttpException) {
    DkResult.Failure(http.toErrorCode(), http)
} catch (other: Throwable) {
    DkResult.Failure(ErrorCode.UNKNOWN, other)
}

private fun HttpException.toErrorCode(): String {
    if (code() == 401 || code() == 403) return ErrorCode.UNAUTHORIZED
    val body = response()?.errorBody()?.string().orEmpty()
    if (body.isBlank()) return ErrorCode.UNKNOWN
    return runCatching { errorJson.decodeFromString<ApiError>(body).code }
        .getOrDefault(ErrorCode.UNKNOWN)
}
