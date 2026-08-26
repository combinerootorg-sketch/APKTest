package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.details.TripDetailsScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.summary.TripSummaryScreen
import com.example.ui.theme.ElegantNavBackground
import com.example.ui.theme.ElegantNavIndicator
import com.example.ui.theme.ElegantPrimary

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = ElegantNavBackground,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElegantPrimary,
                                selectedTextColor = ElegantPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                indicatorColor = ElegantNavIndicator
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Track.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Track.route) {
                HomeScreen(
                    onNavigateToSummary = { tripId ->
                        navController.navigate("trip_summary/$tripId")
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetails = { tripId ->
                        navController.navigate("trip_details/$tripId")
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = "trip_details/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: -1L
                TripDetailsScreen(
                    tripId = tripId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "trip_summary/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: -1L
                TripSummaryScreen(
                    tripId = tripId,
                    onDone = {
                        navController.navigate(Screen.Track.route) {
                            popUpTo(Screen.Track.route) { inclusive = true }
                        }
                    },
                    onViewFullDetails = { id ->
                        navController.navigate("trip_details/$id")
                    }
                )
            }
        }
    }
}
