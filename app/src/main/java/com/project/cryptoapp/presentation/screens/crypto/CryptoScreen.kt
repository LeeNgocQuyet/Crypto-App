package com.project.cryptoapp.presentation.screens.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoCard
import com.project.cryptoapp.presentation.components.SectionHeader
import com.project.cryptoapp.presentation.navigation.AppRoute

@Composable
fun CryptoScreen(
    onNavigate: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoCard {
            SectionHeader("Crypto Tools")
            CryptoButton("Encrypt", onClick = { onNavigate(AppRoute.Encrypt) })
            CryptoButton("Decrypt", onClick = { onNavigate(AppRoute.Decrypt) })
            CryptoButton("Sign", onClick = { onNavigate(AppRoute.Sign) })
            CryptoButton("Verify", onClick = { onNavigate(AppRoute.Verify) })
        }
    }
}
