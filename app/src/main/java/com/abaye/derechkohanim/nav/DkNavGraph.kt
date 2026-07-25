package com.abaye.derechkohanim.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abaye.derechkohanim.ui.screens.auth.LoginRoute
import com.abaye.derechkohanim.ui.screens.auth.RegisterRoute
import com.abaye.derechkohanim.ui.screens.home.HomeRoute
import com.abaye.derechkohanim.ui.screens.nav.NavigationRoute
import com.abaye.derechkohanim.ui.screens.onboarding.OnboardingRoute
import com.abaye.derechkohanim.ui.screens.route.RouteReviewRoute
import com.abaye.derechkohanim.ui.screens.search.DestinationSearchRoute
import com.abaye.derechkohanim.ui.screens.settings.SettingsRoute

/**
 * One graph for the whole app. Live navigation and simulation share the same destination and
 * the same screen; only the trip's `simulated` flag differs, exactly as spec 15.4 intends.
 */
@Composable
fun DkNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
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
            )
        }

        composable(Routes.HOME) {
            HomeRoute(
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onDestinationChosen = { navController.navigate(Routes.ROUTE_REVIEW) },
            )
        }

        composable(Routes.SEARCH) {
            DestinationSearchRoute(
                onBack = { navController.popBackStack() },
                onDestinationChosen = {
                    navController.navigate(Routes.ROUTE_REVIEW) {
                        popUpTo(Routes.SEARCH) { inclusive = true }
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
                onChangeStringency = { navController.navigate(Routes.SETTINGS) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.NAVIGATION) {
            NavigationRoute(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onSignIn = { navController.navigate(Routes.LOGIN) },
            )
        }
    }
}
