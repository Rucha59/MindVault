package com.mindvault.core.di

import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.data.remote.freedictionary.FreeDictionaryApi
import com.mindvault.data.remote.freedictionary.FreeDictionaryDataSource
import com.mindvault.data.remote.freedictionary.FreeDictionaryJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The ONLY file that names the active Search provider. To switch providers, follow
 * docs/SEARCH_PROVIDER.md: change the @Binds below and replace the provider-specific @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchProviderModule {

    @Binds
    @Singleton
    abstract fun bindDefinitionDataSource(impl: FreeDictionaryDataSource): DefinitionDataSource

    companion object {
        private const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/"

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun provideFreeDictionaryApi(client: OkHttpClient): FreeDictionaryApi =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(FreeDictionaryJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(FreeDictionaryApi::class.java)
    }
}