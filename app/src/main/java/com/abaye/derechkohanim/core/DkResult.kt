package com.abaye.derechkohanim.core

/**
 * Failures carry a stable code, never a server sentence, so the client owns the wording
 * (spec 14.4). [ErrorCode] holds the codes the app reacts to on its own.
 */
sealed interface DkResult<out T> {
    data class Success<T>(val value: T) : DkResult<T>
    data class Failure(val code: String, val cause: Throwable? = null) : DkResult<Nothing>
}

object ErrorCode {
    const val NETWORK = "network_unavailable"
    const val UNAUTHORIZED = "unauthorized"
    const val AVOIDANCE_FAILED = "avoidance_failed"
    const val NO_ROUTE = "no_route"
    const val UNKNOWN = "unknown"
}

inline fun <T, R> DkResult<T>.map(transform: (T) -> R): DkResult<R> = when (this) {
    is DkResult.Success -> DkResult.Success(transform(value))
    is DkResult.Failure -> this
}

fun <T> DkResult<T>.valueOrNull(): T? = (this as? DkResult.Success)?.value
