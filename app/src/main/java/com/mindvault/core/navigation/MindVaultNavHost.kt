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
import com.mindvault.feature.flashcards.FlashcardsScreen
import com.mindvault.feature.home.HomeScreen
import com.mindvault.feature.knowledge.AddKnowledgeScreen
import com.mindvault.feature.profile.ProfileScreen
import com.mindvault.feature.search.SearchScreen
import com.mindvault.feature.vault.VaultScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mindvault.feature.knowledge.KnowledgeDetailScreen
private const val TransitionMillis = 150

@Composable
fun MindVaultNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showAddMenu by rememberSaveable { mutableStateOf(false) }

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
                    onEditBeforeSaveClick = { title, definition, example ->
                        navController.navigate(
                            MindVaultDestination.AddKnowledgeFromSearch.createRoute(title, definition, example),
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
                FlashcardsScreen()
            }
            composable(MindVaultDestination.Profile.route) {
                ProfileScreen(onBackClick = { navController.popBackStack() })
            }
            composable(MindVaultDestination.AddKnowledge.route) {
                AddKnowledgeScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = MindVaultDestination.AddKnowledgeFromSearch.route,
                arguments = listOf(PREFILL_TITLE_ARG, PREFILL_DEFINITION_ARG, PREFILL_EXAMPLE_ARG).map { name ->
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