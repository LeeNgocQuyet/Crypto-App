package com.project.cryptoapp.presentation.screens.encrypt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoOutputCard
import com.project.cryptoapp.presentation.components.CryptoTextField
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
import com.project.cryptoapp.presentation.theme.CyberTextSecondary
import com.project.cryptoapp.presentation.viewmodel.EncryptUiState

@Composable
fun EncryptScreen(
    state: EncryptUiState,
    onPlaintextChange: (String) -> Unit,
    onPublicKeyChange: (String) -> Unit,
    onAadChange: (String) -> Unit,
    onOutputFormatChange: (String) -> Unit,
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
        CryptoTextField(
            value = state.plaintext,
            onValueChange = onPlaintextChange,
            label = "Plaintext",
            minLines = 4,
            supportingText = "Encrypted with AES-256-GCM after ECDH/HKDF; no 62-byte ECC point limit.",
        )
        CryptoTextField(
            value = state.publicKey,
            onValueChange = onPublicKeyChange,
            label = "Recipient public key Q(x, y)",
            minLines = 3,
            supportingText = "Use the receiver's public key from the active 512-bit curve.",
        )
        CryptoTextField(
            value = state.aad,
            onValueChange = onAadChange,
            label = "AAD / Context (optional)",
            minLines = 2,
            supportingText = "AAD is authenticated but not encrypted; decrypt must use the same value.",
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ciphertext Format",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Pretty JSON", "Compact JSON").forEach { format ->
                    FilterChip(
                        selected = state.outputFormat == format,
                        onClick = { onOutputFormatChange(format) },
                        label = { Text(format) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberSurfaceVariant,
                            selectedLabelColor = CyberPrimary,
                            labelColor = CyberTextSecondary,
                        ),
                    )
                }
            }
        }
        CryptoButton("Use Latest Public Key", onClick = onUseLatestPublicKey)
        CryptoButton("Encrypt with ECDH + AES-GCM", onClick = onEncrypt, isLoading = state.isLoading)
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Hybrid Ciphertext JSON", state.cipherText)
    }
}
