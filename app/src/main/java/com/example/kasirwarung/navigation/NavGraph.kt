package com.example.kasirwarung.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kasirwarung.presentation.auth.AuthState
import com.example.kasirwarung.presentation.auth.AuthViewModel
import com.example.kasirwarung.presentation.auth.LoginScreen
import com.example.kasirwarung.presentation.auth.RegisterScreen
import com.example.kasirwarung.presentation.product.AddEditProductScreen
import com.example.kasirwarung.presentation.product.ProductListScreen
import com.example.kasirwarung.presentation.product.ProductViewModel
import com.example.kasirwarung.presentation.report.ReportScreen
import com.example.kasirwarung.presentation.transaction.CartScreen
import com.example.kasirwarung.presentation.transaction.CartViewModel
import com.example.kasirwarung.presentation.transaction.TransactionHistoryScreen

object Routes {
    const val ADD_EDIT_PRODUCT_ROUTE = "addEditProduct"
    const val PRODUCT_ID_ARG = "productId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Authenticated -> "productList"
        is AuthState.Unauthenticated -> "login"
        else -> "loading" // Atau bisa juga screen splash
    }

    if (startDestination != "loading") {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                LoginScreen(navController = navController, authViewModel = authViewModel)
            }
            composable("register") {
                RegisterScreen(navController = navController, authViewModel = authViewModel)
            }
            composable("productList") {
                ProductListScreen(
                    navController = navController,
                    productViewModel = productViewModel,
                    authViewModel = authViewModel,
                    cartViewModel = cartViewModel
                )
            }
            composable(
                route = "${Routes.ADD_EDIT_PRODUCT_ROUTE}?${Routes.PRODUCT_ID_ARG}={${Routes.PRODUCT_ID_ARG}}",
                arguments = listOf(navArgument(Routes.PRODUCT_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString(Routes.PRODUCT_ID_ARG)
                AddEditProductScreen(
                    navController = navController,
                    productViewModel = productViewModel,
                    productId = productId
                )
            }
            composable("cart") {
                CartScreen(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    productViewModel = productViewModel
                )
            }
            composable("transactionHistory") {
                TransactionHistoryScreen(
                    navController = navController,
                    productViewModel = productViewModel
                )
            }
            composable("report") {
                ReportScreen(
                    navController = navController,
                    productViewModel = productViewModel
                )
            }
        }
    }
}