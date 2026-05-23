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
        CryptoTextField(state.cipherText, onCipherTextChange, "Hybrid ciphertext JSON", minLines = 7)
        CryptoButton("Use Latest Ciphertext", onClick = onUseLatestCipherText)
        CryptoTextField(state.privateKey, onPrivateKeyChange, "Private key", minLines = 2)
        CryptoTextField(state.aad, onAadChange, "AAD / Context override (optional)", minLines = 2)
        CryptoButton("Use Latest Private Key", onClick = onUseLatestPrivateKey)
        CryptoButton("Decrypt Message", onClick = onDecrypt, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Plaintext", state.plaintext)
    }
}
