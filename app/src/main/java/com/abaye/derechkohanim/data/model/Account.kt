package com.abaye.derechkohanim.data.model

import kotlinx.serialization.Serializable

/**
 * Account and data deletion. A request is a record of somebody asking for their account to be
 * removed, never the removal itself: the server queues it and an operator verifies the requester
 * before anything is destroyed. Mirrors the shared TypeScript contract in
 * `derech-kohanim-shared/src/types/account.ts`.
 */

/** POST body for /api/account/deletion-requests. Works signed in or as a guest. */
@Serializable
data class CreateDeletionRequestRequest(
    val email: String,
    val reason: String? = null,
    val source: String = "android",
)

/**
 * What the endpoint returns. Deliberately says nothing about whether an account exists for the
 * address, so the form cannot be used to probe for registered emails.
 */
@Serializable
data class DeletionRequestReceipt(
    val id: String,
    val status: String,
    val createdAt: String,
)
