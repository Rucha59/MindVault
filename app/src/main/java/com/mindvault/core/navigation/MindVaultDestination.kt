package com.mindvault.core.navigation

import android.net.Uri

const val KNOWLEDGE_ID_ARG = "itemId"
const val DECK_ID_ARG = "deckId"
const val STUDY_SHUFFLE_ARG = "shuffle"
const val REVIEW_MODE_ARG = "mode"

/** Query arguments that prefill the Add form with a Search result the user wants to edit first. */
const val PREFILL_TITLE_ARG = "prefillTitle"
const val PREFILL_DEFINITION_ARG = "prefillDefinition"
const val PREFILL_EXAMPLE_ARG = "prefillExample"
const val PREFILL_TYPE_ARG = "prefillType"

/** Single source of truth for navigation routes. */
sealed class MindVaultDestination(val route: String) {
    data object Home : MindVaultDestination("home")
    data object Search : MindVaultDestination("search")
    data object Vault : MindVaultDestination("vault")
    data object Flashcards : MindVaultDestination("flashcards")
    data object Profile : MindVaultDestination("profile")
    data object Auth : MindVaultDestination("auth")
    data object AddKnowledge : MindVaultDestination("knowledge/add")
    data object AddKnowledgeFromSearch : MindVaultDestination(
        "knowledge/add-from-search" +
                "?$PREFILL_TITLE_ARG={$PREFILL_TITLE_ARG}" +
                "&$PREFILL_DEFINITION_ARG={$PREFILL_DEFINITION_ARG}" +
                "&$PREFILL_EXAMPLE_ARG={$PREFILL_EXAMPLE_ARG}" +
                "&$PREFILL_TYPE_ARG={$PREFILL_TYPE_ARG}",
    ) {
        /** [type] is a KnowledgeType name (WORD or CONCEPT); the form starts with that type selected. */
        fun createRoute(title: String, definition: String, example: String?, type: String? = null): String = buildString {
            append("knowledge/add-from-search")
            append("?$PREFILL_TITLE_ARG=${Uri.encode(title)}")
            append("&$PREFILL_DEFINITION_ARG=${Uri.encode(definition)}")
            if (!example.isNullOrBlank()) append("&$PREFILL_EXAMPLE_ARG=${Uri.encode(example)}")
            if (!type.isNullOrBlank()) append("&$PREFILL_TYPE_ARG=${Uri.encode(type)}")
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
    /** Rated review (spaced repetition). [mode] is a ReviewMode name: DAILY, NEW_CARDS or DIFFICULT. */
    data object Review : MindVaultDestination("flashcards/review/{$DECK_ID_ARG}/{$REVIEW_MODE_ARG}") {
        fun createRoute(deckId: String, mode: String) = "flashcards/review/$deckId/$mode"
    }
    companion object {
        /** Routes that show the shared bottom navigation bar. */
        val bottomBarRoutes: Set<String> by lazy {
            setOf(Home.route, Search.route, Vault.route, Flashcards.route)
        }
    }
}