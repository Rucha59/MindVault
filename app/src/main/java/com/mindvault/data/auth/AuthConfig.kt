package com.mindvault.data.auth

import java.net.URI

/**
 * Supabase URL and anon key from BuildConfig (read from git-ignored local.properties), plus the public
 * Google web client ID. Never the service_role key or the Google client secret.
 */
data class AuthConfig(
    val url: String,
    val anonKey: String,
    val googleWebClientId: String = "",
    val emailEnabled: Boolean = false,
) {
    val isConfigured: Boolean get() = url.startsWith("https://") && anonKey.isNotBlank()
    val isGoogleConfigured: Boolean get() = isConfigured && googleWebClientId.isNotBlank()

    companion object {
        /**
         * supabase-kt wants only the project root, e.g. https://abcd.supabase.co. A pasted REST URL such as
         * https://abcd.supabase.co/rest/v1/ crashes it, so everything after the host is dropped.
         * Returns "" (accounts unavailable, no crash) if the value is not a valid https URL.
         */
        fun normalizeSupabaseUrl(raw: String): String {
            val uri = try {
                URI(raw.trim())
            } catch (e: Exception) {
                return ""
            }
            val host = uri.host ?: return ""
            return if (uri.scheme.equals("https", ignoreCase = true)) "https://$host" else ""
        }
    }
}

/** Must match the intent-filter in AndroidManifest.xml and the redirect URL in the Supabase dashboard. */
object AuthDeepLink {
    const val SCHEME = "mindvault"
    const val HOST = "auth-callback"
}