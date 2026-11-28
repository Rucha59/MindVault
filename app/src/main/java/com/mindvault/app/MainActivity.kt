package com.mindvault.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.navigation.MindVaultNavHost
import com.mindvault.core.theme.MindVaultTheme
import com.mindvault.data.auth.AuthDeepLinkHandler
import com.mindvault.domain.usecase.QuickLookupInbox
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: AppThemeViewModel by viewModels()

    @Inject lateinit var authDeepLinkHandler: AuthDeepLinkHandler
    @Inject lateinit var quickLookupInbox: QuickLookupInbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch: after rotation the same intent would be handled twice.
        if (savedInstanceState == null) {
            authDeepLinkHandler.handle(intent)
            handleQuickLookup(intent)
        }
        setContent {
            val preference by themeViewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = preference.isDark(isSystemInDarkTheme())

            // Keep status/navigation bar icon colors in step with the chosen theme, which may
            // differ from the system setting.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        darkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B),
                    ) { darkTheme },
                )
                onDispose {}
            }

            MindVaultTheme(darkTheme = darkTheme) {
                MindVaultNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authDeepLinkHandler.handle(intent)
        handleQuickLookup(intent)
    }

    /** Text forwarded by QuickLookupActivity goes into the inbox once; the extra is removed so it can't be replayed. */
    private fun handleQuickLookup(intent: Intent?) {
        val text = intent?.getStringExtra(QuickLookupIntents.EXTRA_LOOKUP_TEXT) ?: return
        intent.removeExtra(QuickLookupIntents.EXTRA_LOOKUP_TEXT)
        quickLookupInbox.submit(text)
    }
}