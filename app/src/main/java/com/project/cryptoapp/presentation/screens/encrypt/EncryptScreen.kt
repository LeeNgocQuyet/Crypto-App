package com.project.cryptoapp.presentation.screens.encrypt

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
import com.project.cryptoapp.presentation.viewmodel.EncryptUiState

@Composable
fun EncryptScreen(
    state: EncryptUiState,
    onPlaintextChange: (String) -> Unit,
    onPublicKeyChange: (String) -> Unit,
    onUseLatestPublicKey: () -> Unit,
    onEncrypt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoTextField(state.plaintext, onPlaintextChange, "Plaintext", minLines = 4)
        CryptoTextField(state.publicKey, onPublicKeyChange, "Public key Q(x, y)", minLines = 3)
        CryptoButton("Use Latest Public Key", onClick = onUseLatestPublicKey)
        CryptoButton("Encrypt Message", onClick = onEncrypt, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Ciphertext", state.cipherText)
    }
}
