package com.picoshell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picoshell.ui.state.ShellPresenter
import com.picoshell.ui.state.ShellUiState

@Composable
fun ServicesScreen(
    state: ShellUiState,
    presenter: ShellPresenter,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ServiceToggleCard(
                title = "Voice Control",
                body = "Use a voice-first fallback prompt when the console is empty.",
                checked = state.config.voiceEnabled,
                onCheckedChange = presenter::updateVoiceEnabled,
            )
        }

        item {
            ServiceToggleCard(
                title = "Telegram Bridge",
                body = "Attach a Telegram deep link to each response for fast handoff.",
                checked = state.config.telegramEnabled,
                onCheckedChange = presenter::updateTelegramEnabled,
            )
        }

        item {
            ServiceToggleCard(
                title = "Cloud Fallback",
                body = "Probe a lightweight HTTP endpoint when local execution needs a second lane.",
                checked = state.config.cloudFallbackEnabled,
                onCheckedChange = presenter::updateCloudFallbackEnabled,
            )
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
                        text = "Bridge Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    OutlinedTextField(
                        value = state.config.picoClawCommand,
                        onValueChange = presenter::updatePicoClawCommand,
                        label = { Text("PicoClaw command") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Example: /data/local/tmp/picoclaw --agent mobile") },
                    )
                    OutlinedTextField(
                        value = state.config.telegramChannel,
                        onValueChange = presenter::updateTelegramChannel,
                        label = { Text("Telegram channel or handle") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Example: @picoshell_ops") },
                    )
                    OutlinedTextField(
                        value = state.config.cloudEndpoint,
                        onValueChange = presenter::updateCloudEndpoint,
                        label = { Text("Cloud fallback endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Example: https://example.com/agent") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceToggleCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
