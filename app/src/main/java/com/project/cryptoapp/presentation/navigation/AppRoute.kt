package com.project.cryptoapp.presentation.navigation

sealed class AppRoute(
    val route: String,
    val title: String,
) {
    data object Home : AppRoute("home", "Home")
    data object Keys : AppRoute("keys", "Keys")
    data object Crypto : AppRoute("crypto", "Crypto")
    data object Encrypt : AppRoute("encrypt", "Encrypt")
    data object Decrypt : AppRoute("decrypt", "Decrypt")
    data object Sign : AppRoute("sign", "Sign")
    data object Verify : AppRoute("verify", "Verify")
    data object FileTools : AppRoute("file_tools", "Files")
    data object CurveParameters : AppRoute("curve_parameters", "Curve")
    data object History : AppRoute("history", "History")
    data object Settings : AppRoute("settings", "Settings")

    companion object {
        val bottomRoutes = listOf(Home, Keys, Crypto, History, Settings)
    }
}
