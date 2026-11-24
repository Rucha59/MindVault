package com.mindvault.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.ui.components.AddMenuSheet
import com.mindvault.core.ui.components.MindVaultBottomBar
import com.mindvault.feature.auth.AuthScreen
import com.mindvault.feature.flashcards.AddCardsScreen
import com.mindvault.feature.flashcards.DeckDetailScreen
import com.mindvault.feature.flashcards.FlashcardsScreen
import com.mindvault.feature.flashcards.ReviewScreen
import com.mindvault.feature.flashcards.StudyScreen
import com.mindvault.feature.home.HomeScreen
import com.mindvault.feature.knowledge.AddKnowledgeScreen
import com.mindvault.feature.profile.ProfileScreen
import com.mindvault.feature.search.SearchScreen
import com.mindvault.feature.vault.VaultScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mindvault.feature.knowledge.KnowledgeDetailScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.app.AppSessionViewModel
private const val TransitionMillis = 150

@Composable
fun MindVaultNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    val sessionViewModel: AppSessionViewModel = hiltViewModel()
    val dataOwner by sessionViewModel.owner.collectAsStateWithLifecycle()
    var handledOwner by remember { mutableStateOf(dataOwner) }
    // A different vault (another account, or guest) is now active: go to Home with a clean history and
    // drop the saved tab states, so no screen keeps filters or state from the previous account.
    LaunchedEffect(dataOwner) {
        if (dataOwner != handledOwner) {
            handledOwner = dataOwner
            showAddMenu = false
            navController.navigate(MindVaultDestination.Home.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            MindVaultDestination.bottomBarRoutes.forEach { navController.clearBackStack(it) }
        }
    }

    // Switching between main destinations preserves each destination state.
    fun navigateToMain(destination: MindVaultDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateTo(destination: MindVaultDestination) {
        navController.navigate(destination.route) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = MindVaultColors.Background,
        bottomBar = {
            if (currentRoute != null && currentRoute in MindVaultDestination.bottomBarRoutes) {
                MindVaultBottomBar(
                    selectedRoute = currentRoute,
                    onDestinationClick = { navigateToMain(it) },
                    onAddClick = { showAddMenu = true },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MindVaultDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(TransitionMillis)) },
            exitTransition = { fadeOut(tween(TransitionMillis)) },
            popEnterTransition = { fadeIn(tween(TransitionMillis)) },
            popExitTransition = { fadeOut(tween(TransitionMillis)) },
        ) {
            composable(MindVaultDestination.Home.route) {
                HomeScreen(
                    onSearchClick = { navigateToMain(MindVaultDestination.Search) },
                    onAddConceptClick = { navigateTo(MindVaultDestination.AddKnowledge) },
                    onVaultClick = { navigateToMain(MindVaultDestination.Vault) },
                    onFlashcardsClick = { navigateToMain(MindVaultDestination.Flashcards) },
                    onProfileClick = { navigateTo(MindVaultDestination.Profile) },
                    onItemClick = { itemId ->
                        navController.navigate(MindVaultDestination.KnowledgeDetail.createRoute(itemId)) {
                            launchSingleTop = true
                        }
                    },
                    onDeckClick = { deckId ->
                        navController.navigate(MindVaultDestination.DeckDetail.createRoute(deckId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(MindVaultDestination.Search.route) {
                SearchScreen(
                    onAddConceptClick = { navigateTo(MindVaultDestination.AddKnowledge) },
                    onViewSavedClick = { itemId ->
                        navController.navigate(MindVaultDestination.KnowledgeDetail.createRoute(itemId)) {
                            launchSingleTop = true
                        }
                    },
                    onEditBeforeSaveClick = { title, definition, example, type ->
                        navController.navigate(
                            MindVaultDestination.AddKnowledgeFromSearch.createRoute(title, definition, example, type),
                        ) { launchSingleTop = true }
                    },
                )
            }
            composable(MindVaultDestination.Vault.route) {
                VaultScreen(
                    onAddConceptClick = { navigateTo(MindVaultDestination.AddKnowledge) },
                    onItemClick = { itemId ->
                        navController.navigate(MindVaultDestination.KnowledgeDetail.createRoute(itemId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(MindVaultDestination.Flashcards.route) {
                FlashcardsScreen(
                    onDeckClick = { deckId ->
                        navController.navigate(MindVaultDestination.DeckDetail.createRoute(deckId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = MindVaultDestination.DeckDetail.route,
                arguments = listOf(navArgument(DECK_ID_ARG) { type = NavType.StringType }),
            ) {
                DeckDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onStudyClick = { deckId, shuffle ->
                        navController.navigate(MindVaultDestination.Study.createRoute(deckId, shuffle)) {
                            launchSingleTop = true
                        }
                    },
                    onReviewClick = { deckId, mode ->
                        navController.navigate(MindVaultDestination.Review.createRoute(deckId, mode.name)) {
                            launchSingleTop = true
                        }
                    },
                    onAddCardsClick = { deckId ->
                        navController.navigate(MindVaultDestination.DeckAddCards.createRoute(deckId)) {
                            launchSingleTop = true
                        }
                    },
                    onCardClick = { itemId ->
                        navController.navigate(MindVaultDestination.KnowledgeDetail.createRoute(itemId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = MindVaultDestination.DeckAddCards.route,
                arguments = listOf(navArgument(DECK_ID_ARG) { type = NavType.StringType }),
            ) {
                AddCardsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = MindVaultDestination.Study.route,
                arguments = listOf(
                    navArgument(DECK_ID_ARG) { type = NavType.StringType },
                    navArgument(STUDY_SHUFFLE_ARG) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                StudyScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = MindVaultDestination.Review.route,
                arguments = listOf(
                    navArgument(DECK_ID_ARG) { type = NavType.StringType },
                    navArgument(REVIEW_MODE_ARG) { type = NavType.StringType },
                ),
            ) {
                ReviewScreen(onBackClick = { navController.popBackStack() })
            }
            composable(MindVaultDestination.Profile.route) {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onSignInClick = { navigateTo(MindVaultDestination.Auth) },
                )
            }
            composable(MindVaultDestination.Auth.route) {
                // Signing in changes the vault and resets navigation to Home; only pop if there is somewhere to go back to.
                AuthScreen(onClose = { if (navController.previousBackStackEntry != null) navController.popBackStack() })
            }
            composable(MindVaultDestination.AddKnowledge.route) {
                AddKnowledgeScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = MindVaultDestination.AddKnowledgeFromSearch.route,
                arguments = listOf(PREFILL_TITLE_ARG, PREFILL_DEFINITION_ARG, PREFILL_EXAMPLE_ARG, PREFILL_TYPE_ARG).map { name ->
                    navArgument(name) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                },
            ) {
                // Same form as Add; the ViewModel prefills it from the route arguments.
                AddKnowledgeScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = MindVaultDestination.KnowledgeDetail.route,
                arguments = listOf(navArgument(KNOWLEDGE_ID_ARG) { type = NavType.StringType }),
            ) {
                KnowledgeDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { itemId ->
                        navController.navigate(MindVaultDestination.KnowledgeEdit.createRoute(itemId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = MindVaultDestination.KnowledgeEdit.route,
                arguments = listOf(navArgument(KNOWLEDGE_ID_ARG) { type = NavType.StringType }),
            ) {
                // Same screen as Add; the ViewModel switches to edit mode when it receives an item ID.
                AddKnowledgeScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }

    if (showAddMenu) {
        AddMenuSheet(
            onDismiss = { showAddMenu = false },
            onAddConcept = {
                showAddMenu = false
                navigateTo(MindVaultDestination.AddKnowledge)
            },
            onSearchAndSave = {
                showAddMenu = false
                navigateToMain(MindVaultDestination.Search)
            },
        )
    }
}