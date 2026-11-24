package com.mindvault.core.di

import android.content.Context
import com.mindvault.BuildConfig
import com.mindvault.data.remote.ConceptDataSource
import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.data.remote.SuggestionDataSource
import com.mindvault.data.remote.wikimedia.BackoffInterceptor
import com.mindvault.data.remote.wikimedia.BackoffPolicy
import com.mindvault.data.remote.wikimedia.UserAgentInterceptor
import com.mindvault.data.remote.wikimedia.WIKIPEDIA_HOST
import com.mindvault.data.remote.wikimedia.WIKTIONARY_HOST
import com.mindvault.data.remote.wikimedia.WikimediaJson
import com.mindvault.data.remote.wikimedia.WikipediaApi
import com.mindvault.data.remote.wikimedia.WikipediaDataSource
import com.mindvault.data.remote.wikimedia.WiktionaryApi
import com.mindvault.data.remote.wikimedia.WiktionaryDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Search V2 wiring: Wiktionary (words) and Wikipedia (concepts). Names the V2 providers; which Search
 * implementation the app uses is chosen in RepositoryModule. Does not touch the V1 (Free Dictionary) wiring in
 * SearchProviderModule: its OkHttpClient is unqualified, this module's is @Named("wikimedia").
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WikimediaModule {

    @Binds
    @Singleton
    @Named("wiktionary")
    abstract fun bindWordSource(impl: WiktionaryDataSource): DefinitionDataSource

    @Binds
    @Singleton
    abstract fun bindConceptSource(impl: WikipediaDataSource): ConceptDataSource

    @Binds
    @Singleton
    @Named("wiktionary")
    abstract fun bindWordSuggestionSource(impl: WiktionaryDataSource): SuggestionDataSource

    @Binds
    @Singleton
    @Named("wikipedia")
    abstract fun bindConceptSuggestionSource(impl: WikipediaDataSource): SuggestionDataSource

    companion object {
        /**
         * Wikimedia asks every client to identify itself with contact information. REPLACE this with a project
         * email address or website URL before any public release (it is sent with every request).
         */
        private const val CONTACT = "REPLACE_WITH_EMAIL_OR_URL"

        private const val HTTP_CACHE_BYTES = 10L * 1024 * 1024

        @Provides
        @Singleton
        @Named("wikimedia")
        fun provideWikimediaClient(@ApplicationContext context: Context): OkHttpClient =
            OkHttpClient.Builder()
                // At most two requests in flight: Wikimedia rate-limits clients and asks for modest concurrency.
                .dispatcher(Dispatcher().apply { maxRequests = 2; maxRequestsPerHost = 2 })
                .cache(Cache(File(context.cacheDir, "wikimedia_http"), HTTP_CACHE_BYTES))
                .addInterceptor(UserAgentInterceptor("MindVault/${BuildConfig.VERSION_NAME} (Android; contact: $CONTACT)"))
                .addInterceptor(BackoffInterceptor(BackoffPolicy()))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()

        @Provides
        @Singleton
        fun provideWiktionaryApi(@Named("wikimedia") client: OkHttpClient): WiktionaryApi =
            retrofit("https://$WIKTIONARY_HOST/", client).create(WiktionaryApi::class.java)

        @Provides
        @Singleton
        fun provideWikipediaApi(@Named("wikimedia") client: OkHttpClient): WikipediaApi =
            retrofit("https://$WIKIPEDIA_HOST/", client).create(WikipediaApi::class.java)

        private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(WikimediaJson.asConverterFactory("application/json".toMediaType()))
                .build()
    }
}