package com.mindvault.core.navigation

/** Single source of truth for navigation routes. */
sealed class MindVaultDestination(val route: String) {
    data object Home : MindVaultDestination("home")
    data object Search : MindVaultDestination("search")
    data object Vault : MindVaultDestination("vault")
    data object Flashcards : MindVaultDestination("flashcards")
    data object Profile : MindVaultDestination("profile")
    data object AddKnowledge : MindVaultDestination("knowledge/add")

    companion object {
        /** Routes that show the shared bottom navigation bar. */
        val bottomBarRoutes: Set<String> by lazy {
            setOf(Home.route, Search.route, Vault.route, Flashcards.route)
        }
    }
}