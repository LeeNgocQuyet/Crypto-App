package com.project.cryptoapp.presentation.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.presentation.navigation.AppRoute
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSecondary
import com.project.cryptoapp.presentation.theme.CyberSurface
import com.project.cryptoapp.presentation.theme.CyberSurfaceVariant
import com.project.cryptoapp.presentation.theme.CyberTertiary

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val operations = listOf(
        DashboardOperation(
            title = "Generate Keys",
            description = "Create a BrainpoolP512r1 private/public key pair.",
            icon = Icons.Filled.VpnKey,
            accentColor = CyberPrimary,
            route = AppRoute.Keys,
        ),
        DashboardOperation(
            title = "Encrypt",
            description = "Encrypt plaintext with EC ElGamal.",
            icon = Icons.Filled.Lock,
            accentColor = CyberTertiary,
            route = AppRoute.Encrypt,
        ),
        DashboardOperation(
            title = "Decrypt",
            description = "Recover plaintext from C1 and C2 points.",
            icon = Icons.Filled.Security,
            accentColor = Color(0xFFFFC857),
            route = AppRoute.Decrypt,
        ),
        DashboardOperation(
            title = "Sign Message",
            description = "Generate an ECDSA signature r and s.",
            icon = Icons.Filled.Create,
            accentColor = CyberSecondary,
            route = AppRoute.Sign,
        ),
        DashboardOperation(
            title = "Verify Signature",
            description = "Verify an ECDSA signature.",
            icon = Icons.Filled.CheckCircle,
            accentColor = Color(0xFF70E1F5),
            route = AppRoute.Verify,
        ),
        DashboardOperation(
            title = "History",
            description = "Review saved Room operation logs.",
            icon = Icons.Filled.History,
            accentColor = Color(0xFFFF6B9A),
            route = AppRoute.History,
        ),
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard()
        Text(
            text = "Operations",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        operations.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { operation ->
                    OperationCard(
                        operation = operation,
                        onClick = { onNavigate(operation.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, CyberPrimary),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "BrainpoolP512r1 over Fp - 512-bit",
                style = MaterialTheme.typography.headlineSmall,
                color = CyberPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Elliptic Curve Cryptography with real curve arithmetic",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OperationCard(
    operation: DashboardOperation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, operation.accentColor.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = operation.icon,
                contentDescription = operation.title,
                tint = operation.accentColor,
            )
            Text(
                text = operation.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = operation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class DashboardOperation(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val route: AppRoute,
)
