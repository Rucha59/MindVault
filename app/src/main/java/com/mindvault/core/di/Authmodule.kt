package com.mindvault.core.di

import com.mindvault.BuildConfig
import com.mindvault.data.auth.AuthConfig
import com.mindvault.data.auth.AuthDeepLink
import com.mindvault.data.auth.AuthRepositoryImpl
import com.mindvault.data.auth.EncryptedSessionManager
import com.mindvault.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides @Singleton
        fun provideAuthConfig(): AuthConfig = AuthConfig(
            url = AuthConfig.normalizeSupabaseUrl(BuildConfig.SUPABASE_URL),
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            emailEnabled = BuildConfig.AUTH_EMAIL_ENABLED,
        )

        /** Only called (lazily) when [AuthConfig.isConfigured] is true. */
        @Provides @Singleton
        fun provideSupabaseClient(
            config: AuthConfig,
            sessionManager: EncryptedSessionManager,
        ): SupabaseClient = createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.anonKey,
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                this.sessionManager = sessionManager
                // PKCE: the email link carries a one-time code, not tokens, so another app that
                // registers the same custom scheme cannot steal a session.
                flowType = FlowType.PKCE
                scheme = AuthDeepLink.SCHEME
                host = AuthDeepLink.HOST
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
        }
    }
}