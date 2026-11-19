package com.mindvault.data.auth

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import javax.inject.Singleton

/** Lets MainActivity pass email-confirmation / reset links to Supabase without touching its types. */
@Singleton
class AuthDeepLinkHandler @Inject constructor(
    private val config: AuthConfig,
    private val client: dagger.Lazy<SupabaseClient>,
) {
    fun handle(intent: Intent?) {
        if (!config.isConfigured) return
        if (intent?.data?.scheme != AuthDeepLink.SCHEME) return
        client.get().handleDeeplinks(intent)
    }
}