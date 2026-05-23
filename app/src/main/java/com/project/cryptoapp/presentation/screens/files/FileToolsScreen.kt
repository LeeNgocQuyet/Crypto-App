package com.project.cryptoapp.presentation.screens.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoCard
import com.project.cryptoapp.presentation.components.CryptoOutputCard
import com.project.cryptoapp.presentation.components.CryptoTextField
import com.project.cryptoapp.presentation.components.SectionHeader
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.viewmodel.FileToolsUiState

@Composable
fun FileToolsScreen(
    state: FileToolsUiState,
    onPublicKeyChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onUseLatestPublicKey: () -> Unit,
    onUseLatestPrivateKey: () -> Unit,
    onEncryptFile: (Uri) -> Unit,
    onDecryptFile: (Uri) -> Unit,
    onSignFile: (Uri) -> Unit,
    onVerifyFileSignature: (Uri, Uri) -> Unit,
    onSaveEncryptedPayload: (Uri) -> Unit,
    onSaveDecryptedFile: (Uri) -> Unit,
    onSaveSignaturePayload: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fileToVerify by remember { mutableStateOf<Uri?>(null) }

    val pickFileForEncrypt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onEncryptFile)
    }
    val pickFileForDecrypt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onDecryptFile)
    }
    val pickFileForSign = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onSignFile)
    }
    val pickFileForVerify = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileToVerify = uri
    }
    val pickSignatureForVerify = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { signatureUri ->
        val fileUri = fileToVerify
        if (fileUri != null && signatureUri != null) {
            onVerifyFileSignature(fileUri, signatureUri)
        }
    }
    val saveEncryptedPayload = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onSaveEncryptedPayload)
    }
    val saveDecryptedFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let(onSaveDecryptedFile)
    }
    val saveSignaturePayload = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onSaveSignaturePayload)
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoCard {
            SectionHeader("File Keys")
            CryptoTextField(
                value = state.publicKey,
                onValueChange = onPublicKeyChange,
                label = "Recipient / signer public key",
                minLines = 3,
                supportingText = "Used for file encryption and signature verification.",
            )
            CryptoButton("Use Latest Public Key", onClick = onUseLatestPublicKey)
            CryptoTextField(
                value = state.privateKey,
                onValueChange = onPrivateKeyChange,
                label = "Private key",
                minLines = 2,
                supportingText = "Used for file decryption and file signing.",
            )
            CryptoButton("Use Latest Private Key", onClick = onUseLatestPrivateKey)
        }

        FileActionCard(
            title = "Encrypt File",
            description = "Reads the selected file, encrypts its bytes with ECDH + AES-GCM, and creates a JSON payload.",
        ) {
            CryptoButton("Choose File to Encrypt", onClick = { pickFileForEncrypt.launch(arrayOf("*/*")) }, isLoading = state.isLoading)
            CryptoButton(
                text = "Save Encrypted JSON",
                onClick = { saveEncryptedPayload.launch("encrypted-file.cryptoapp.json") },
                enabled = state.encryptedFilePayload.isNotBlank(),
            )
        }

        FileActionCard(
            title = "Decrypt File",
            description = "Reads a .cryptoapp.json payload, verifies the AES-GCM tag, and restores original bytes.",
        ) {
            CryptoButton("Choose JSON to Decrypt", onClick = { pickFileForDecrypt.launch(arrayOf("application/json", "text/*", "*/*")) }, isLoading = state.isLoading)
            CryptoButton(
                text = "Save Decrypted File",
                onClick = { saveDecryptedFile.launch("decrypted-file.bin") },
                enabled = state.decryptedFileBytes != null,
            )
        }

        FileActionCard(
            title = "Sign File",
            description = "Hashes the file with SHA-512 and signs the digest with deterministic ECDSA.",
        ) {
            CryptoButton("Choose File to Sign", onClick = { pickFileForSign.launch(arrayOf("*/*")) }, isLoading = state.isLoading)
            CryptoButton(
                text = "Save Signature JSON",
                onClick = { saveSignaturePayload.launch("file-signature.cryptoapp.json") },
                enabled = state.fileSignaturePayload.isNotBlank(),
            )
        }

        FileActionCard(
            title = "Verify File Signature",
            description = "Choose the original file first, then choose its signature JSON.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CryptoButton(
                    text = "Choose File",
                    onClick = { pickFileForVerify.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f),
                )
                CryptoButton(
                    text = "Choose Signature",
                    onClick = { pickSignatureForVerify.launch(arrayOf("application/json", "text/*", "*/*")) },
                    modifier = Modifier.weight(1f),
                    enabled = fileToVerify != null,
                    isLoading = state.isLoading,
                )
            }
            if (fileToVerify != null) {
                Text(
                    text = "File selected. Now choose the signature JSON.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        StatusMessage(state.errorMessage, state.successMessage)
        CryptoOutputCard("Encrypted File Payload", state.encryptedFilePayload)
        CryptoOutputCard("File Signature Payload", state.fileSignaturePayload)
        CryptoOutputCard("File Verification Result", state.verificationResult)
    }
}

@Composable
private fun FileActionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    CryptoCard {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        content()
    }
}
