package com.example.deepseekstream.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deepseekstream.ui.screens.StartScreen
import com.example.deepseekstream.ui.screens.Task1Screen
import com.example.deepseekstream.ui.screens.Task2Screen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.START
    ) {
        composable(NavRoutes.START) {
            StartScreen(
                onTask1Click = { navController.navigate(NavRoutes.TASK1) },
                onTask2Click = { navController.navigate(NavRoutes.TASK2) }
            )
        }
        composable(NavRoutes.TASK1) {
            Task1Screen()
        }
        composable(NavRoutes.TASK2) {
            Task2Screen()
        }
    }
}
