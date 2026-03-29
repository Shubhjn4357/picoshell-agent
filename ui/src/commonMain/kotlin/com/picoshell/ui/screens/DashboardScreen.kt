package com.picoshell.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picoshell.ui.components.CapabilityTile
import com.picoshell.ui.components.PhaseTimeline
import com.picoshell.ui.state.ShellPresenter
import com.picoshell.ui.state.ShellUiState

@Composable
fun DashboardScreen(
    state: ShellUiState,
    presenter: ShellPresenter,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "PicoShell Agent Bundle",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "A local-first mobile cockpit for PicoClaw execution, GGUF staging, cloud fallback, Telegram relay, and TurboQuant cache tuning.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Prompt Console",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    OutlinedTextField(
                        value = state.draftPrompt,
                        onValueChange = presenter::updatePrompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        label = { Text("Prompt or voice fallback brief") },
                    )
                    Button(
                        onClick = presenter::runAgent,
                        enabled = !state.isRunning,
                    ) {
                        Text(if (state.isRunning) "Running..." else "Run Agent")
                    }
                    AnimatedVisibility(state.latestResponse.isNotBlank()) {
                        Text(
                            text = state.latestResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "System Surface",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        items(state.capabilities) { capability ->
            CapabilityTile(capability = capability)
        }

        item {
            Text(
                text = "Build Sequence",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            PhaseTimeline(phases = state.phases)
        }

        item {
            Text(
                text = "Recent Runs",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        items(state.recentRuns) { run ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${run.engine.name} · ${run.id.takeLast(4)}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = run.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = run.output,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

