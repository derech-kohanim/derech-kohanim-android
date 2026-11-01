package com.abaye.derechkohanim.nav

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.screens.auth.LoginRoute
import com.abaye.derechkohanim.ui.screens.auth.RegisterRoute
import com.abaye.derechkohanim.ui.screens.home.HomeRoute
import com.abaye.derechkohanim.ui.screens.legal.DeleteAccountRoute
import com.abaye.derechkohanim.ui.screens.legal.PrivacyRoute
import com.abaye.derechkohanim.ui.screens.legal.TermsRoute
import com.abaye.derechkohanim.ui.screens.nav.NavigationRoute
import com.abaye.derechkohanim.ui.screens.onboarding.OnboardingRoute
import com.abaye.derechkohanim.ui.screens.route.RouteReviewRoute
import com.abaye.derechkohanim.ui.screens.search.DestinationSearchRoute
import com.abaye.derechkohanim.ui.screens.settings.SettingsRoute
import com.abaye.derechkohanim.ui.screens.settings.SettingsSection

/**
 * One graph for the whole app. Live navigation and simulation share the same destination and
 * the same screen; only the trip's `simulated` flag differs, exactly as spec 15.4 intends.
 */
@Composable
fun DkNavGraph(
    startDestination: String,
    pendingResume: Boolean = false,
    onConfirmResume: () -> Unit = {},
    onDiscardResume: () -> Unit = {},
    onScreen: (String) -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    // Emit a GA4 screen_view as the back stack changes. Analytics is injected in MainActivity and
    // handed in as [onScreen]. Every destination reports a stable logical name of its own, so the
    // funnel from onboarding to a driven route is readable in GA; the search route reports one
    // name for both its modes, since the argument is a detail of how it was opened.
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val screen = when (destination.route) {
                Routes.ONBOARDING -> "onboarding"
                Routes.LOGIN -> "login"
                Routes.REGISTER -> "register"
                Routes.HOME -> "home"
                Routes.SEARCH_PATTERN -> "search"
                Routes.ROUTE_REVIEW -> "route_review"
                Routes.NAVIGATION -> "navigation"
                Routes.SETTINGS, Routes.SETTINGS_SECTION_PATTERN -> "settings"
                Routes.TERMS -> "terms"
                Routes.PRIVACY -> "privacy"
                Routes.DELETE_ACCOUNT -> "delete_account"
                else -> null
            }
            if (screen != null) onScreen(screen)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
            )
        }

        composable(Routes.LOGIN) {
            LoginRoute(
                onSignedIn = { navController.popBackStack() },
                onGoToRegister = { navController.navigate(Routes.REGISTER) },
                onContinueAsGuest = { navController.popBackStack() },
            )
        }

        composable(Routes.REGISTER) {
            RegisterRoute(
                onSignedIn = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onGoToLogin = { navController.popBackStack() },
                onContinueAsGuest = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
            )
        }

        composable(Routes.HOME) { backStackEntry ->
            // A place picked in origin-mode search pops back here and leaves this flag as a result,
            // so Home can show the "start location set" confirmation just like the map-tap path.
            val originJustSet by backStackEntry.savedStateHandle
                .getStateFlow(Routes.RESULT_ORIGIN_SET, false)
                .collectAsStateWithLifecycle()
            HomeRoute(
                onOpenSearch = { navController.navigate(Routes.search()) },
                onOpenSearchForOrigin = { navController.navigate(Routes.search(originMode = true)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onDestinationChosen = { navController.navigate(Routes.ROUTE_REVIEW) },
                originJustSet = originJustSet,
                onOriginConfirmConsumed = {
                    backStackEntry.savedStateHandle[Routes.RESULT_ORIGIN_SET] = false
                },
            )
        }

        composable(
            route = Routes.SEARCH_PATTERN,
            arguments = listOf(
                navArgument(Routes.SEARCH_ARG_MODE) {
                    type = NavType.StringType
                    defaultValue = Routes.SEARCH_MODE_DESTINATION
                },
            ),
        ) { backStackEntry ->
            val originMode = backStackEntry.arguments
                ?.getString(Routes.SEARCH_ARG_MODE) == Routes.SEARCH_MODE_ORIGIN
            DestinationSearchRoute(
                onBack = { navController.popBackStack() },
                onDestinationChosen = {
                    if (originMode) {
                        // The manual origin was stored by the search view model. Signal Home to
                        // confirm it, then return there instead of proceeding to route review.
                        navController.previousBackStackEntry
                            ?.savedStateHandle?.set(Routes.RESULT_ORIGIN_SET, true)
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.ROUTE_REVIEW) {
                            popUpTo(Routes.SEARCH_PATTERN) { inclusive = true }
                        }
                    }
                },
                // Guests are routed to login for account-only actions; login pops back to search,
                // where the pending action is replayed once signed in (return-to-after-login).
                onRequireLogin = { navController.navigate(Routes.LOGIN) },
                viewModel = hiltViewModel(),
            )
        }

        composable(Routes.ROUTE_REVIEW) {
            RouteReviewRoute(
                origin = null,
                onStart = {
                    navController.navigate(Routes.NAVIGATION) {
                        popUpTo(Routes.HOME)
                    }
                },
                onChangeDestination = {
                    navController.navigate(Routes.SEARCH) {
                        popUpTo(Routes.ROUTE_REVIEW) { inclusive = true }
                    }
                },
                onChangeStringency = {
                    navController.navigate(Routes.settingsSection(SettingsSection.AVOIDANCE.route))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.NAVIGATION) {
            NavigationRoute(
                onOpenVoiceSettings = {
                    navController.navigate(Routes.settingsSection(SettingsSection.VOICE.route))
                },
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                section = null,
                onBack = { navController.popBackStack() },
                onOpenSection = { navController.navigate(Routes.settingsSection(it.route)) },
                onSignIn = { navController.navigate(Routes.LOGIN) },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
            )
        }

        composable(
            route = Routes.SETTINGS_SECTION_PATTERN,
            arguments = listOf(navArgument(Routes.SETTINGS_ARG_SECTION) { type = NavType.StringType }),
        ) { entry ->
            SettingsRoute(
                section = SettingsSection.fromRoute(entry.arguments?.getString(Routes.SETTINGS_ARG_SECTION)),
                onPlaceChosen = { navController.navigate(Routes.ROUTE_REVIEW) },
                onBack = { navController.popBackStack() },
                onOpenSection = { navController.navigate(Routes.settingsSection(it.route)) },
                onSignIn = { navController.navigate(Routes.LOGIN) },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
            )
        }

        composable(Routes.TERMS) {
            TermsRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyRoute(
                onBack = { navController.popBackStack() },
                onDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
            )
        }

        composable(Routes.DELETE_ACCOUNT) {
            DeleteAccountRoute(onBack = { navController.popBackStack() })
        }
    }

    // Offered once on launch when a real drive was still active when the app was killed. Confirming
    // hands the saved drive to the engine and jumps to the navigation screen; declining discards it.
    if (pendingResume) {
        AlertDialog(
            onDismissRequest = onDiscardResume,
            title = { Text(stringResource(R.string.nav_resume_title)) },
            text = { Text(stringResource(R.string.nav_resume_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmResume()
                    navController.navigate(Routes.NAVIGATION) {
                        popUpTo(Routes.HOME)
                    }
                }) {
                    Text(stringResource(R.string.nav_resume_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardResume) {
                    Text(stringResource(R.string.nav_resume_dismiss))
                }
            },
        )
    }
}
