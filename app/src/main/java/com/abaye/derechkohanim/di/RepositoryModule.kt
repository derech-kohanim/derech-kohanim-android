package com.abaye.derechkohanim.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.mock.MockAuthRepository
import com.abaye.derechkohanim.data.mock.MockPlacesRepository
import com.abaye.derechkohanim.data.mock.MockRouteEventsRepository
import com.abaye.derechkohanim.data.mock.MockRoutingRepository
import com.abaye.derechkohanim.data.mock.MockVoiceRepository
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.repository.AuthRepository
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.data.repository.RemoteAuthRepository
import com.abaye.derechkohanim.data.repository.RemotePlacesRepository
import com.abaye.derechkohanim.data.repository.RemoteRouteEventsRepository
import com.abaye.derechkohanim.data.repository.RemoteRoutingRepository
import com.abaye.derechkohanim.data.repository.RemoteVoiceRepository
import com.abaye.derechkohanim.data.repository.RouteEventsRepository
import com.abaye.derechkohanim.data.repository.RoutingRepository
import com.abaye.derechkohanim.data.repository.VoiceRepository
import com.abaye.derechkohanim.nav.TripStore
import javax.inject.Singleton

/**
 * The single switch between the fake server and the real one. Set USE_MOCK_DATA in
 * local.properties or pass -PUSE_MOCK_DATA=false on the command line.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun placesRepository(api: DerechKohanimApi): PlacesRepository =
        if (BuildConfig.USE_MOCK_DATA) MockPlacesRepository() else RemotePlacesRepository(api)

    @Provides
    @Singleton
    fun routingRepository(api: DerechKohanimApi): RoutingRepository =
        if (BuildConfig.USE_MOCK_DATA) MockRoutingRepository() else RemoteRoutingRepository(api)

    @Provides
    @Singleton
    fun authRepository(api: DerechKohanimApi, sessionStore: SessionStore): AuthRepository =
        if (BuildConfig.USE_MOCK_DATA) {
            MockAuthRepository(sessionStore)
        } else {
            RemoteAuthRepository(api, sessionStore)
        }

    @Provides
    @Singleton
    fun voiceRepository(api: DerechKohanimApi): VoiceRepository =
        if (BuildConfig.USE_MOCK_DATA) MockVoiceRepository() else RemoteVoiceRepository(api)

    @Provides
    @Singleton
    fun routeEventsRepository(api: DerechKohanimApi, tripStore: TripStore): RouteEventsRepository =
        if (BuildConfig.USE_MOCK_DATA) {
            MockRouteEventsRepository(tripStore)
        } else {
            RemoteRouteEventsRepository(api)
        }
}
