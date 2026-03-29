package com.picoshell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picoshell.domain.model.CacheStrategy
import com.picoshell.domain.model.ModelOrigin
import com.picoshell.ui.components.ModelTile
import com.picoshell.ui.state.ShellPresenter
import com.picoshell.ui.state.ShellUiState

@Composable
fun ModelsScreen(
    state: ShellUiState,
    presenter: ShellPresenter,
    onAddLocalModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localModels = state.models.filter { it.catalog.origin == ModelOrigin.Imported }
    val catalogModels = state.models.filter { it.catalog.origin == ModelOrigin.Catalog }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Cache Strategy",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CacheStrategy.entries.forEach { strategy ->
                        FilterChip(
                            selected = state.config.cacheStrategy == strategy,
                            onClick = { presenter.updateCacheStrategy(strategy) },
                            label = { Text(strategy.name) },
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Local GGUF Models",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Pick a GGUF file from device storage. The app links the selected model in place without duplicating the file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onAddLocalModel) {
                        Text("Import Local GGUF")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Download From Link",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Paste a direct GGUF file URL or a Hugging Face model repository link. If multiple GGUF files are published, the app resolves the preferred quantized artifact automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.modelUrlDraft,
                        onValueChange = presenter::updateModelUrlDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model URL") },
                        supportingText = {
                            Text("Example: https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF")
                        },
                    )
                    Button(
                        onClick = presenter::downloadModelFromUrl,
                        enabled = state.modelUrlDraft.isNotBlank(),
                    ) {
                        Text("Download Model")
                    }
                }
            }
        }

        if (localModels.isNotEmpty()) {
            item {
                Text(
                    text = "Imported Models",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            items(localModels, key = { it.catalog.id }) { model ->
                ModelTile(
                    model = model,
                    isSelected = state.config.selectedModelId == model.catalog.id,
                    onSelect = { presenter.selectModel(model.catalog.id) },
                    onStage = {},
                )
            }
        }

        item {
            Text(
                text = "Catalog Models",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        items(catalogModels, key = { it.catalog.id }) { model ->
            ModelTile(
                model = model,
                isSelected = state.config.selectedModelId == model.catalog.id,
                onSelect = { presenter.selectModel(model.catalog.id) },
                onStage = { presenter.stageModel(model.catalog.id) },
            )
        }
    }
}
