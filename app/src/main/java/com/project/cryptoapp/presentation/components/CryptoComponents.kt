package com.project.cryptoapp.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.presentation.navigation.AppRoute
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSurface
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
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
) {
    if (output.isBlank()) return
    CryptoCard(modifier = modifier) {
        SectionHeader(title)
        Text(
            text = output,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                icon = { Text(route.title.take(1)) },
                label = { Text(route.title) },
            )
        }
    }
}
