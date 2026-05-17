package com.project.cryptoapp.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoCard
import com.project.cryptoapp.presentation.components.SectionHeader
import com.project.cryptoapp.presentation.navigation.AppRoute

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "ECC-512 Crypto App",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Demo workspace for key generation, encryption, decryption, signing and verification over a 512-bit elliptic curve.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        CryptoCard {
            SectionHeader("Operations")
            CryptoButton("Generate Keys", onClick = { onNavigate(AppRoute.Keys) })
            CryptoButton("Open Crypto Tools", onClick = { onNavigate(AppRoute.Crypto) })
            CryptoButton("View History", onClick = { onNavigate(AppRoute.History) })
        }
    }
}
