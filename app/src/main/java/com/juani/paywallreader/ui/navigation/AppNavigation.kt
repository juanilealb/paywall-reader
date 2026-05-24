package com.juani.paywallreader.ui.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.ui.home.HomeRoute
import com.juani.paywallreader.ui.reader.ReaderRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        enterTransition = { forwardEnterTransition() },
        exitTransition = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition = { backwardExitTransition() },
    ) {
        composable(AppRoute.Home.route) {
            HomeRoute(
                onSourceClick = { source ->
                    navController.navigate(AppRoute.Reader.createRoute(source))
                },
            )
        }
        composable(
            route = AppRoute.Reader.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            ReaderRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")

    data object Reader : AppRoute("reader/{url}?name={name}") {
        fun createRoute(source: Source): String {
            val encodedUrl = Uri.encode(source.url)
            val encodedName = Uri.encode(source.name)
            return "reader/$encodedUrl?name=$encodedName"
        }
    }
}

private fun forwardEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { it }) + fadeIn()

private fun forwardExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()

private fun backwardEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()

private fun backwardExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
