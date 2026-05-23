package com.project.cryptoapp.presentation.screens.verify

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
import com.project.cryptoapp.presentation.viewmodel.VerifyUiState

@Composable
fun VerifyScreen(
    state: VerifyUiState,
    onMessageChange: (String) -> Unit,
    onPublicKeyChange: (String) -> Unit,
    onSignatureRChange: (String) -> Unit,
    onSignatureSChange: (String) -> Unit,
    onUseLatestPublicKey: () -> Unit,
    onUseLatestSignature: () -> Unit,
    onVerify: () -> Unit,
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
            minLines = 3,
            supportingText = "Must match the message that was signed.",
        )
        CryptoTextField(
            value = state.publicKey,
            onValueChange = onPublicKeyChange,
            label = "Signer public key Q(x, y)",
            minLines = 3,
            supportingText = "Use the public key paired with the signing private key.",
        )
        CryptoButton("Use Latest Public Key", onClick = onUseLatestPublicKey)
        CryptoTextField(
            value = state.signatureR,
            onValueChange = onSignatureRChange,
            label = "Signature r",
            supportingText = "Hex scalar in [1, n - 1].",
        )
        CryptoTextField(
            value = state.signatureS,
            onValueChange = onSignatureSChange,
            label = "Signature s",
            supportingText = "Hex scalar in [1, n - 1].",
        )
        CryptoButton("Use Latest Signature", onClick = onUseLatestSignature)
        CryptoButton("Verify Signature", onClick = onVerify, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard(
            title = "Verification Result",
            output = state.isValid?.let { if (it) "VALID" else "INVALID" }.orEmpty(),
        )
    }
}
