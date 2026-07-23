package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.VoicePackManifest
import com.abaye.derechkohanim.data.model.VoiceResolveRequest
import com.abaye.derechkohanim.data.model.VoiceResolveResponse
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import javax.inject.Inject

interface VoiceRepository {
    suspend fun manifest(
        language: Language,
        voiceId: String,
        sinceVersion: Int?,
    ): DkResult<VoicePackManifest>

    suspend fun resolve(
        language: Language,
        voiceId: String,
        keys: List<String>,
    ): DkResult<VoiceResolveResponse>
}

class RemoteVoiceRepository @Inject constructor(
    private val api: DerechKohanimApi,
) : VoiceRepository {

    override suspend fun manifest(
        language: Language,
        voiceId: String,
        sinceVersion: Int?,
    ): DkResult<VoicePackManifest> = apiCall { api.voicePack(language.tag, voiceId, sinceVersion) }

    override suspend fun resolve(
        language: Language,
        voiceId: String,
        keys: List<String>,
    ): DkResult<VoiceResolveResponse> =
        apiCall { api.resolveVoiceClips(VoiceResolveRequest(language, voiceId, keys)) }
}
