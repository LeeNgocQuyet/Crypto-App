package com.project.cryptoapp.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.presentation.navigation.AppRoute
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSurface
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
import com.project.cryptoapp.presentation.theme.CyberTextSecondary
import com.project.cryptoapp.util.toReadableDateTime

@Composable
fun CryptoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, CyberSurfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
fun CryptoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (minLines > 1) 112.dp else 56.dp),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberPrimary,
            unfocusedBorderColor = CyberSurfaceVariant,
            focusedLabelColor = CyberPrimary,
            cursorColor = CyberPrimary,
        ),
    )
}

@Composable
fun CryptoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CryptoOutputCard(
    title: String,
    output: String,
    modifier: Modifier = Modifier,
    showCopy: Boolean = true,
) {
    if (output.isBlank()) return
    val clipboardManager = LocalClipboardManager.current
    CryptoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionHeader(
                text = title,
                modifier = Modifier.weight(1f),
            )
            if (showCopy) {
                TextButton(
                    onClick = { clipboardManager.setText(AnnotatedString(output)) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy $title",
                        tint = CyberPrimary,
                    )
                    Text("Copy")
                }
            }
        }
        Text(
            text = output,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun CopyTextButton(
    text: String,
    label: String = "Copy",
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    TextButton(
        onClick = { clipboardManager.setText(AnnotatedString(text)) },
        modifier = modifier,
        enabled = text.isNotBlank(),
    ) {
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = label,
            tint = CyberPrimary,
        )
        Text(label)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = CyberPrimary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun StatusMessage(
    errorMessage: String?,
    successMessage: String?,
    modifier: Modifier = Modifier,
) {
    val message = errorMessage ?: successMessage ?: return
    val color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Text(
        text = message,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun OperationHistoryItem(
    item: CryptoHistory,
    modifier: Modifier = Modifier,
) {
    CryptoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.operationType.name,
                color = CyberPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.status.name,
                color = if (item.status.name == "SUCCESS") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = item.timestamp.toReadableDateTime(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "Input: ${item.inputText}",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Output: ${item.outputText}",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = CyberSurface,
    ) {
        AppRoute.bottomRoutes.forEach { route ->
            NavigationBarItem(
                selected = currentRoute == route.route,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = route.bottomNavIcon(),
                        contentDescription = route.title,
                    )
                },
                label = { Text(route.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberPrimary,
                    selectedTextColor = CyberPrimary,
                    indicatorColor = CyberSurfaceVariant,
                    unselectedIconColor = CyberTextSecondary,
                    unselectedTextColor = CyberTextSecondary,
                ),
            )
        }
    }
}

private fun AppRoute.bottomNavIcon(): ImageVector = when (this) {
    AppRoute.Home -> Icons.Filled.Home
    AppRoute.Keys -> Icons.Filled.VpnKey
    AppRoute.Crypto -> Icons.Filled.Security
    AppRoute.History -> Icons.Filled.History
    AppRoute.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}
