package com.picoshell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.picoshell.domain.model.CapabilityState
import com.picoshell.domain.model.CapabilityStatus

@Composable
fun CapabilityTile(
    capability: CapabilityStatus,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val tone = when (capability.state) {
                CapabilityState.Ready -> MaterialTheme.colorScheme.primary
                CapabilityState.Degraded -> MaterialTheme.colorScheme.secondary
                CapabilityState.Disabled -> MaterialTheme.colorScheme.surfaceVariant
            }

            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tone)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = capability.state.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (capability.state == CapabilityState.Disabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.White
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = capability.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = capability.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

