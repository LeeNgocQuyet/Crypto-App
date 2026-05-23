package com.project.cryptoapp.presentation.screens.curve

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.cryptoapp.domain.crypto.BrainpoolP512r1
import com.project.cryptoapp.domain.model.ECCurveParams
import com.project.cryptoapp.presentation.components.CopyTextButton
import com.project.cryptoapp.presentation.components.CryptoCard
import com.project.cryptoapp.presentation.components.SectionHeader
import com.project.cryptoapp.presentation.theme.CyberPrimary
import com.project.cryptoapp.presentation.theme.CyberSecondary
import com.project.cryptoapp.presentation.theme.CyberTertiary
import com.project.cryptoapp.util.toDisplayString

@Composable
fun CurveParametersScreen(
    modifier: Modifier = Modifier,
) {
    val params = demoCurveParams()

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CryptoCard {
            SectionHeader("BrainpoolP512r1 over Fp - 512-bit")
            Text(
                text = "The app now uses these fixed BrainpoolP512r1 parameters for key generation, encryption, decryption, signing and verification.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            CopyTextButton(text = params.toDisplayString(), label = "Copy All")
        }

        CurveParamCard("p", "Prime modulus", params.p, CyberPrimary)
        CurveParamCard("a", "Curve coefficient", params.a, CyberSecondary)
        CurveParamCard("b", "Curve coefficient", params.b, CyberTertiary)
        CurveParamCard("Gx", "Base point x-coordinate", params.gx, Color(0xFF70E1F5))
        CurveParamCard("Gy", "Base point y-coordinate", params.gy, Color(0xFFFFC857))
        CurveParamCard("n", "Base point order", params.n.orEmpty(), Color(0xFFFF6B9A))
    }
}

@Composable
private fun CurveParamCard(
    name: String,
    description: String,
    value: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = accentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = accentColor,
                )
            }
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
            )
            CopyTextButton(text = value, label = "Copy $name")
        }
    }
}

private fun demoCurveParams(): ECCurveParams = BrainpoolP512r1.params
