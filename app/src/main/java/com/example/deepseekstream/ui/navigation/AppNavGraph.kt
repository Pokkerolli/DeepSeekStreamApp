package com.example.deepseekstream.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deepseekstream.ui.screens.StartScreen
import com.example.deepseekstream.ui.screens.Task1Screen
import com.example.deepseekstream.ui.screens.Task2Screen
import com.example.deepseekstream.ui.screens.Task4Screen
import com.example.deepseekstream.ui.screens.Task5Screen

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
                onTask2Click = { navController.navigate(NavRoutes.TASK2) },
                onTask4Click = { navController.navigate(NavRoutes.TASK4) },
                onTask5Click = { navController.navigate(NavRoutes.TASK5) }
            )
        }
        composable(NavRoutes.TASK1) {
            Task1Screen()
        }
        composable(NavRoutes.TASK2) {
            Task2Screen()
        }
        composable(NavRoutes.TASK4) {
            Task4Screen()
        }
        composable(NavRoutes.TASK5) {
            Task5Screen()
        }
    }
}
