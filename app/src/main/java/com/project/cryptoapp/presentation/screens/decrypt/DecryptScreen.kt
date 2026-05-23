package com.project.cryptoapp.presentation.screens.decrypt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoOutputCard
import com.project.cryptoapp.presentation.components.CryptoTextField
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.viewmodel.DecryptUiState

@Composable
fun DecryptScreen(
    state: DecryptUiState,
    onCipherTextChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onAadChange: (String) -> Unit,
    onUseLatestCipherText: () -> Unit,
    onUseLatestPrivateKey: () -> Unit,
    onDecrypt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoTextField(
            value = state.cipherText,
            onValueChange = onCipherTextChange,
            label = "Hybrid ciphertext JSON",
            minLines = 7,
            supportingText = "Must include algorithm, ephemeral public key, salt, nonce, ciphertext and tag.",
        )
        CryptoButton("Use Latest Ciphertext", onClick = onUseLatestCipherText)
        CryptoTextField(
            value = state.privateKey,
            onValueChange = onPrivateKeyChange,
            label = "Receiver private key",
            minLines = 2,
            supportingText = "Private key must match the public key used during encryption.",
        )
        CryptoTextField(
            value = state.aad,
            onValueChange = onAadChange,
            label = "AAD / Context override (optional)",
            minLines = 2,
            supportingText = "Leave blank to use the AAD embedded in the payload, or enter the exact external AAD.",
        )
        CryptoButton("Use Latest Private Key", onClick = onUseLatestPrivateKey)
        CryptoButton("Decrypt and Verify Tag", onClick = onDecrypt, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Plaintext", state.plaintext)
    }
}
