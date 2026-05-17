package com.project.cryptoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.project.cryptoapp.presentation.navigation.AppNavigation
import com.project.cryptoapp.presentation.theme.ECC512CryptoTheme
import com.project.cryptoapp.util.AppContainer

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(applicationContext)
        enableEdgeToEdge()
        setContent {
            ECC512CryptoTheme {
                AppNavigation(appContainer = appContainer)
            }
        }
    }
}
