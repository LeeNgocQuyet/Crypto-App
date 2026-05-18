package com.project.cryptoapp.presentation.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.OperationType
import com.project.cryptoapp.presentation.components.CryptoButton
import com.project.cryptoapp.presentation.components.CryptoCard
import com.project.cryptoapp.presentation.components.StatusMessage
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSecondary
import com.project.cryptoapp.presentation.theme.CyberSurface
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
import com.project.cryptoapp.presentation.theme.CyberTertiary
import com.project.cryptoapp.presentation.theme.CyberTextSecondary
import com.project.cryptoapp.presentation.viewmodel.HistoryUiState
import com.project.cryptoapp.util.toReadableDateTime

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.All) }
    val filteredHistory = remember(state.history, selectedFilter) {
        state.history.filter { item ->
            selectedFilter.operationType == null || item.operationType == selectedFilter.operationType
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ClearHistoryCard(
            enabled = state.history.isNotEmpty(),
            onClearHistory = onClearHistory,
        )
        StatusMessage(state.errorMessage, state.successMessage)
        HistoryFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
        )
        when {
            state.isLoading -> CircularProgressIndicator(color = CyberPrimary)
            state.history.isEmpty() -> EmptyHistoryState()
            filteredHistory.isEmpty() -> EmptyHistoryState(
                message = "No ${selectedFilter.label.lowercase()} operations yet.",
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredHistory, key = { it.id }) { item ->
                    CompactHistoryItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun ClearHistoryCard(
    enabled: Boolean,
    onClearHistory: () -> Unit,
) {
    CryptoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Operation History",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Room-backed audit trail for demo operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CryptoButton(
            text = "Clear History",
            onClick = onClearHistory,
            enabled = enabled,
        )
    }
}

@Composable
private fun HistoryFilterChips(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberSurfaceVariant,
                    selectedLabelColor = CyberPrimary,
                    labelColor = CyberTextSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = CyberSurfaceVariant,
                    selectedBorderColor = CyberPrimary,
                ),
            )
        }
    }
}

@Composable
private fun CompactHistoryItem(
    item: CryptoHistory,
    modifier: Modifier = Modifier,
) {
    val accentColor = item.operationType.accentColor()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.operationType.displayName(),
                    color = accentColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.status.name,
                    color = if (item.status.name == "SUCCESS") CyberTertiary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = item.timestamp.toReadableDateTime(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = item.outputText.compactOutput(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyHistoryState(
    message: String = "No operation history yet.",
) {
    CryptoCard {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = CyberTextSecondary,
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Run key generation or crypto operations to populate this list.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private enum class HistoryFilter(
    val label: String,
    val operationType: OperationType?,
) {
    All("All", null),
    Key("Key", OperationType.KEY_GENERATION),
    Encrypt("Encrypt", OperationType.ENCRYPT),
    Decrypt("Decrypt", OperationType.DECRYPT),
    Sign("Sign", OperationType.SIGN),
    Verify("Verify", OperationType.VERIFY),
}

private fun OperationType.displayName(): String = when (this) {
    OperationType.KEY_GENERATION -> "Key Generation"
    OperationType.ENCRYPT -> "Encrypt"
    OperationType.DECRYPT -> "Decrypt"
    OperationType.SIGN -> "Sign"
    OperationType.VERIFY -> "Verify"
}

private fun OperationType.accentColor(): Color = when (this) {
    OperationType.KEY_GENERATION -> CyberPrimary
    OperationType.ENCRYPT -> CyberTertiary
    OperationType.DECRYPT -> Color(0xFFFFC857)
    OperationType.SIGN -> CyberSecondary
    OperationType.VERIFY -> Color(0xFF70E1F5)
}

private fun String.compactOutput(maxLength: Int = 120): String {
    val normalized = replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    return if (normalized.length <= maxLength) normalized else "${normalized.take(maxLength)}..."
}
