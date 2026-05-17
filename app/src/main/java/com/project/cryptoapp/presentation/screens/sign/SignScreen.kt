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
    onSign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoTextField(state.message, onMessageChange, "Message", minLines = 4)
        CryptoTextField(state.privateKey, onPrivateKeyChange, "Private key", minLines = 2)
        CryptoButton("Sign Message", onClick = onSign, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Digital Signature", state.signature)
    }
}
