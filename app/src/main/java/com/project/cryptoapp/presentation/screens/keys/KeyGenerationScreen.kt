package com.project.cryptoapp.presentation.screens.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoOutputCard
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.viewmodel.KeyGenerationUiState

@Composable
fun KeyGenerationScreen(
    state: KeyGenerationUiState,
    onGenerateKeyPair: () -> Unit,
    onUsePublicKeyForEncrypt: () -> Unit,
    onUsePublicKeyForVerify: () -> Unit,
    onUsePrivateKeyForDecrypt: () -> Unit,
    onUsePrivateKeyForSign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoButton(
            text = "Generate ECC-512 Key Pair",
            onClick = onGenerateKeyPair,
            isLoading = state.isLoading,
        )
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Private Key", state.privateKey)
        CryptoOutputCard("Public Key", state.publicKey)
        if (state.privateKey.isNotBlank() || state.publicKey.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CryptoButton(
                        text = "Use Public Key: Encrypt",
                        onClick = onUsePublicKeyForEncrypt,
                        modifier = Modifier.weight(1f),
                    )
                    CryptoButton(
                        text = "Use Public Key: Verify",
                        onClick = onUsePublicKeyForVerify,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CryptoButton(
                        text = "Use Private Key: Decrypt",
                        onClick = onUsePrivateKeyForDecrypt,
                        modifier = Modifier.weight(1f),
                    )
                    CryptoButton(
                        text = "Use Private Key: Sign",
                        onClick = onUsePrivateKeyForSign,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
