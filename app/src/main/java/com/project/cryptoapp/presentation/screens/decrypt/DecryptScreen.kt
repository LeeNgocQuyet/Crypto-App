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
    onDecrypt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoTextField(state.cipherText, onCipherTextChange, "Ciphertext C1, C2", minLines = 5)
        CryptoTextField(state.privateKey, onPrivateKeyChange, "Private key", minLines = 2)
        CryptoButton("Decrypt Message", onClick = onDecrypt, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Plaintext", state.plaintext)
    }
}
