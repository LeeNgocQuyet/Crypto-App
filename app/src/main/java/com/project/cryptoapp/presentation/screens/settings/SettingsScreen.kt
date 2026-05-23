package com.project.cryptoapp.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.theme.CyberError
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSecondary
import com.project.cryptoapp.presentation.theme.CyberSurface
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
import com.project.cryptoapp.presentation.theme.CyberTertiary
import com.project.cryptoapp.presentation.theme.CyberTextSecondary
import com.project.cryptoapp.presentation.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onDefaultEncodingChange: (String) -> Unit,
    onClearHistory: () -> Unit,
    onResetAppData: () -> Unit,
    onOpenCurveParameters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsCard(
            title = "Crypto Engine",
            description = "Real ECC service enabled",
            icon = Icons.Filled.Security,
            accentColor = CyberPrimary,
        ) {
            CryptoButton("View Curve Parameters", onClick = onOpenCurveParameters)
        }

        SettingsCard(
            title = "Default Encoding",
            description = "Choose how text input should be interpreted by crypto forms.",
            icon = Icons.Filled.Code,
            accentColor = CyberSecondary,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Text", "Hex").forEach { encoding ->
                    FilterChip(
                        selected = state.defaultEncoding == encoding,
                        onClick = { onDefaultEncodingChange(encoding) },
                        label = { Text(encoding) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberSurfaceVariant,
                            selectedLabelColor = CyberPrimary,
                            labelColor = CyberTextSecondary,
                        ),
                    )
                }
            }
        }

        SettingsCard(
            title = "Save History",
            description = "Store operation logs locally using Room database.",
            icon = Icons.Filled.Storage,
            accentColor = CyberTertiary,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.enableHistory) "Enabled" else "Disabled",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = state.enableHistory,
                    onCheckedChange = onHistoryEnabledChange,
                )
            }
        }

        SettingsCard(
            title = "Clear History",
            description = "Remove all saved local operation records.",
            icon = Icons.Filled.Delete,
            accentColor = CyberError,
        ) {
            CryptoButton(
                text = "Clear History",
                onClick = { showClearDialog = true },
                isLoading = state.isLoading,
            )
        }

        SettingsCard(
            title = "Reset App Data",
            description = "Clear history and restore default local settings.",
            icon = Icons.Filled.RestartAlt,
            accentColor = Color(0xFFFF6B9A),
        ) {
            CryptoButton(
                text = "Reset App Data",
                onClick = { showResetDialog = true },
                isLoading = state.isLoading,
            )
        }

        SettingsCard(
            title = "About App",
            description = "Version 1.0\nEncryption: ECDH + HKDF-SHA512 + AES-256-GCM\nSignature: deterministic ECDSA-SHA512\nCurve: BrainpoolP512r1 over Fp",
            icon = Icons.Filled.Info,
            accentColor = Color(0xFF70E1F5),
        )

        SettingsCard(
            title = "Security Warning",
            description = "Educational purpose only. Messages are authenticated with AES-GCM, but private keys are not yet protected by Android Keystore.",
            icon = Icons.Filled.Warning,
            accentColor = Color(0xFFFFC857),
        )

        StatusMessage(state.errorMessage, state.successMessage)
    }

    if (showClearDialog) {
        ConfirmationDialog(
            title = "Clear history?",
            text = "This removes all saved operation history from Room.",
            onConfirm = {
                showClearDialog = false
                onClearHistory()
            },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showResetDialog) {
        ConfirmationDialog(
            title = "Reset app data?",
            text = "This clears history and restores local settings to defaults.",
            onConfirm = {
                showResetDialog = false
                onResetAppData()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = accentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            content?.invoke()
        }
    }
}
