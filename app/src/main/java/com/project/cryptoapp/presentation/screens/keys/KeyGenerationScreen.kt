package com.project.cryptoapp.presentation.screens.keys

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    onLoadProtectedKeyPair: () -> Unit,
    onClearProtectedKeyPair: () -> Unit,
    onPreparePublicKeyExport: () -> Unit,
    onPreparePrivateKeyBackup: () -> Unit,
    onImportKeyPayload: (Uri) -> Unit,
    onSaveKeyExport: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val importKeyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportKeyPayload)
    }
    val saveKeyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onSaveKeyExport)
    }

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
        CryptoButton(
            text = "Load Protected Key Pair",
            onClick = onLoadProtectedKeyPair,
            enabled = state.hasProtectedKey,
        )
        CryptoButton(
            text = "Clear Protected Key Pair",
            onClick = onClearProtectedKeyPair,
            enabled = state.hasProtectedKey,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CryptoButton(
                text = "Export Public Key",
                onClick = onPreparePublicKeyExport,
                modifier = Modifier.weight(1f),
                enabled = state.publicKey.isNotBlank(),
            )
            CryptoButton(
                text = "Backup Private Key",
                onClick = onPreparePrivateKeyBackup,
                modifier = Modifier.weight(1f),
                enabled = state.privateKey.isNotBlank() && state.publicKey.isNotBlank(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CryptoButton(
                text = "Import Key JSON",
                onClick = { importKeyLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                modifier = Modifier.weight(1f),
            )
            CryptoButton(
                text = "Save Key JSON",
                onClick = { saveKeyLauncher.launch("cryptoapp-key.json") },
                modifier = Modifier.weight(1f),
                enabled = state.keyExportPayload.isNotBlank(),
            )
        }
        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Private Key", state.privateKey)
        CryptoOutputCard("Public Key", state.publicKey)
        CryptoOutputCard("QR-ready Key JSON", state.keyExportPayload)
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
