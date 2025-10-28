/* package com.example.keyless_app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keyless_app.ablauf.AblaufScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()

){
    NavHost(
        navController = navController,
        startDestination = "ablauf"
    ) {
        composable("ablauf") {
            AblaufScreen(navController)
        }
    }




}*/