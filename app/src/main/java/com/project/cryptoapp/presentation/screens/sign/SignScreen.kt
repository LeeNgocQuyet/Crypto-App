package com.project.cryptoapp.presentation.screens.sign

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
import com.project.cryptoapp.presentation.viewmodel.SignUiState

@Composable
fun SignScreen(
    state: SignUiState,
    onMessageChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onUseLatestPrivateKey: () -> Unit,
    onSign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = "Message",
            minLines = 4,
            supportingText = "Signed with deterministic ECDSA-SHA512.",
        )
        CryptoTextField(
            value = state.privateKey,
            onValueChange = onPrivateKeyChange,
            label = "Private key",
            minLines = 2,
            supportingText = "Nonce k is derived deterministically from this key and message.",
        )
        CryptoButton("Use Latest Private Key", onClick = onUseLatestPrivateKey)
        CryptoButton("Sign with Deterministic ECDSA", onClick = onSign, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Digital Signature", state.signature)
    }
}
