package com.picoshell.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picoshell.domain.model.InstallState
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelOrigin

@Composable
fun ModelTile(
    model: ModelCard,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = model.catalog.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${model.catalog.category} | ${model.catalog.format} | ${model.catalog.origin.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = model.catalog.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (model.installState != InstallState.Missing) {
                LinearProgressIndicator(
                    progress = { model.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = model.installation?.notes ?: model.installState.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                model.installation?.localPath?.let { location ->
                    Text(
                        text = if (location.startsWith("content://")) {
                            "Linked document: $location"
                        } else {
                            "Local file: $location"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (model.catalog.origin == ModelOrigin.Catalog) {
                    Button(onClick = onStage) {
                        Text(
                            text = if (model.installState == InstallState.Missing) {
                                "Stage"
                            } else {
                                "Refresh"
                            },
                        )
                    }
                }
                OutlinedButton(onClick = onSelect) {
                    Text(text = if (isSelected) "Selected" else "Use Model")
                }
            }
        }
    }
}
