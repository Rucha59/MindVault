package com.mindvault.core.navigation

import android.net.Uri

const val KNOWLEDGE_ID_ARG = "itemId"
const val DECK_ID_ARG = "deckId"
const val STUDY_SHUFFLE_ARG = "shuffle"

/** Query arguments that prefill the Add form with a Search result the user wants to edit first. */
const val PREFILL_TITLE_ARG = "prefillTitle"
const val PREFILL_DEFINITION_ARG = "prefillDefinition"
const val PREFILL_EXAMPLE_ARG = "prefillExample"

/** Single source of truth for navigation routes. */
sealed class MindVaultDestination(val route: String) {
    data object Home : MindVaultDestination("home")
    data object Search : MindVaultDestination("search")
    data object Vault : MindVaultDestination("vault")
    data object Flashcards : MindVaultDestination("flashcards")
    data object Profile : MindVaultDestination("profile")
    data object AddKnowledge : MindVaultDestination("knowledge/add")
    data object AddKnowledgeFromSearch : MindVaultDestination(
        "knowledge/add-from-search" +
                "?$PREFILL_TITLE_ARG={$PREFILL_TITLE_ARG}" +
                "&$PREFILL_DEFINITION_ARG={$PREFILL_DEFINITION_ARG}" +
                "&$PREFILL_EXAMPLE_ARG={$PREFILL_EXAMPLE_ARG}",
    ) {
        fun createRoute(title: String, definition: String, example: String?): String = buildString {
            append("knowledge/add-from-search")
            append("?$PREFILL_TITLE_ARG=${Uri.encode(title)}")
            append("&$PREFILL_DEFINITION_ARG=${Uri.encode(definition)}")
            if (!example.isNullOrBlank()) append("&$PREFILL_EXAMPLE_ARG=${Uri.encode(example)}")
        }
    }
    data object KnowledgeDetail : MindVaultDestination("knowledge/detail/{$KNOWLEDGE_ID_ARG}") {
        fun createRoute(itemId: String) = "knowledge/detail/$itemId"
    }
    data object KnowledgeEdit : MindVaultDestination("knowledge/edit/{$KNOWLEDGE_ID_ARG}") {
        fun createRoute(itemId: String) = "knowledge/edit/$itemId"
    }
    data object DeckDetail : MindVaultDestination("flashcards/deck/{$DECK_ID_ARG}") {
        fun createRoute(deckId: String) = "flashcards/deck/$deckId"
    }
    data object DeckAddCards : MindVaultDestination("flashcards/deck/{$DECK_ID_ARG}/add") {
        fun createRoute(deckId: String) = "flashcards/deck/$deckId/add"
    }
    data object Study : MindVaultDestination("flashcards/study/{$DECK_ID_ARG}?$STUDY_SHUFFLE_ARG={$STUDY_SHUFFLE_ARG}") {
        fun createRoute(deckId: String, shuffle: Boolean) = "flashcards/study/$deckId?$STUDY_SHUFFLE_ARG=$shuffle"
    }
    companion object {
        /** Routes that show the shared bottom navigation bar. */
        val bottomBarRoutes: Set<String> by lazy {
            setOf(Home.route, Search.route, Vault.route, Flashcards.route)
        }
    }
}