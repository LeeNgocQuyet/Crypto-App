package com.project.cryptoapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.cryptoapp.presentation.components.BottomNavigationBar
import com.project.cryptoapp.presentation.screens.crypto.CryptoScreen
import com.project.cryptoapp.presentation.screens.decrypt.DecryptScreen
import com.project.cryptoapp.presentation.screens.encrypt.EncryptScreen
import com.project.cryptoapp.presentation.screens.history.HistoryScreen
import com.project.cryptoapp.presentation.screens.home.HomeScreen
import com.project.cryptoapp.presentation.screens.keys.KeyGenerationScreen
import com.project.cryptoapp.presentation.screens.settings.SettingsScreen
import com.project.cryptoapp.presentation.screens.sign.SignScreen
import com.project.cryptoapp.presentation.screens.verify.VerifyScreen
import com.project.cryptoapp.presentation.viewmodel.AppViewModelFactory
import com.project.cryptoapp.presentation.viewmodel.DecryptViewModel
import com.project.cryptoapp.presentation.viewmodel.EncryptViewModel
import com.project.cryptoapp.presentation.viewmodel.HistoryViewModel
import com.project.cryptoapp.presentation.viewmodel.KeyGenerationViewModel
import com.project.cryptoapp.presentation.viewmodel.SettingsViewModel
import com.project.cryptoapp.presentation.viewmodel.SignViewModel
import com.project.cryptoapp.presentation.viewmodel.VerifyViewModel
import com.project.cryptoapp.util.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val factory = remember(appContainer) { AppViewModelFactory(appContainer) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val canNavigateBack = currentRoute in setOf(
        AppRoute.Encrypt.route,
        AppRoute.Decrypt.route,
        AppRoute.Sign.route,
        AppRoute.Verify.route,
    )
    val selectedBottomRoute = when (currentRoute) {
        AppRoute.Encrypt.route,
        AppRoute.Decrypt.route,
        AppRoute.Sign.route,
        AppRoute.Verify.route
        -> AppRoute.Crypto.route
        else -> currentRoute
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(routeTitle(currentRoute)) },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = selectedBottomRoute,
                onNavigate = { route ->
                    navController.navigateToBottomRoute(route)
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            composable(AppRoute.Home.route) {
                HomeScreen(onNavigate = { route ->
                    if (route in AppRoute.bottomRoutes) {
                        navController.navigateToBottomRoute(route)
                    } else {
                        navController.navigate(route.route)
                    }
                })
            }
            composable(AppRoute.Keys.route) {
                val viewModel: KeyGenerationViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                KeyGenerationScreen(
                    state = state,
                    onGenerateKeyPair = viewModel::generateKeyPair,
                )
            }
            composable(AppRoute.Crypto.route) {
                CryptoScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(AppRoute.Encrypt.route) {
                val viewModel: EncryptViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                EncryptScreen(
                    state = state,
                    onPlaintextChange = viewModel::onPlaintextChange,
                    onPublicKeyChange = viewModel::onPublicKeyChange,
                    onEncrypt = viewModel::encrypt,
                )
            }
            composable(AppRoute.Decrypt.route) {
                val viewModel: DecryptViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                DecryptScreen(
                    state = state,
                    onCipherTextChange = viewModel::onCipherTextChange,
                    onPrivateKeyChange = viewModel::onPrivateKeyChange,
                    onDecrypt = viewModel::decrypt,
                )
            }
            composable(AppRoute.Sign.route) {
                val viewModel: SignViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                SignScreen(
                    state = state,
                    onMessageChange = viewModel::onMessageChange,
                    onPrivateKeyChange = viewModel::onPrivateKeyChange,
                    onSign = viewModel::sign,
                )
            }
            composable(AppRoute.Verify.route) {
                val viewModel: VerifyViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                VerifyScreen(
                    state = state,
                    onMessageChange = viewModel::onMessageChange,
                    onPublicKeyChange = viewModel::onPublicKeyChange,
                    onSignatureRChange = viewModel::onSignatureRChange,
                    onSignatureSChange = viewModel::onSignatureSChange,
                    onVerify = viewModel::verify,
                )
            }
            composable(AppRoute.History.route) {
                val viewModel: HistoryViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                HistoryScreen(
                    state = state,
                    onClearHistory = viewModel::clearHistory,
                )
            }
            composable(AppRoute.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                SettingsScreen(
                    state = state,
                    onHistoryEnabledChange = viewModel::setHistoryEnabled,
                )
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToBottomRoute(route: AppRoute) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun routeTitle(route: String?): String = when (route) {
    AppRoute.Home.route -> AppRoute.Home.title
    AppRoute.Keys.route -> "Key Generation"
    AppRoute.Crypto.route -> AppRoute.Crypto.title
    AppRoute.Encrypt.route -> AppRoute.Encrypt.title
    AppRoute.Decrypt.route -> AppRoute.Decrypt.title
    AppRoute.Sign.route -> AppRoute.Sign.title
    AppRoute.Verify.route -> AppRoute.Verify.title
    AppRoute.History.route -> AppRoute.History.title
    AppRoute.Settings.route -> AppRoute.Settings.title
    else -> "ECC-512 Crypto"
}
