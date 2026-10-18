package com.mindvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mindvault.core.navigation.MindVaultNavHost
import com.mindvault.core.theme.MindVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default edge-to-edge: transparent bars with icon colors that follow the system
        // light/dark setting, matching MindVaultTheme.
        enableEdgeToEdge()
        setContent {
            MindVaultTheme {
                MindVaultNavHost()
            }
        }
    }
}